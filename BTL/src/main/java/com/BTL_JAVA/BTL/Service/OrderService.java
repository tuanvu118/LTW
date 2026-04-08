package com.BTL_JAVA.BTL.Service;

import com.BTL_JAVA.BTL.DTO.Request.Order.OrderRequest;
import com.BTL_JAVA.BTL.DTO.Request.Order.OrderUpdateRequest;
import com.BTL_JAVA.BTL.DTO.Response.OrderResponse;
import com.BTL_JAVA.BTL.DTO.Response.Product.ProductVariationResponse;
import com.BTL_JAVA.BTL.Entity.Address;
import com.BTL_JAVA.BTL.Entity.Orders.Order;
import com.BTL_JAVA.BTL.Entity.Orders.OrderDetail;
import com.BTL_JAVA.BTL.Entity.Payment;
import com.BTL_JAVA.BTL.Entity.Product.ProductVariation;
import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.*;
import com.BTL_JAVA.BTL.Service.Product.ProductService;
import com.BTL_JAVA.BTL.Service.Product.ProductVariationService;
import com.BTL_JAVA.BTL.Service.Product.SalesService;
import com.BTL_JAVA.BTL.enums.OrderStatus;
import com.BTL_JAVA.BTL.enums.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    OrderRepository orderRepository;
    UserRepository userRepository;
    AddressRepository addressRepository;
    ProductVariationRepository productVariationRepository;

    ProductService productCacheService;
    ProductVariationService variationCacheService;
    SalesService salesCacheService;
    RedisService redisService;
    RedissonClient redissonClient;
    TransactionTemplate transactionTemplate;

    ObjectMapper objectMapper;

    static String VARIATION_CACHE_PREFIX = "variation:";
    static String VARIATION_LOCK_PREFIX = "lock:variation:";
    static String ORDER_USER_CACHE = "order:user:";
    static String ORDER_USER_LOCK = "lock:order:user:";
    static String PENDING_FEEDBACK_CACHE = "user:pending-feedbacks";
    static String NULL_VALUE = "NOT_EXIST";

    // Các phương thức lấy người và ktra quyền
    private User getCurrentAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName(); // Lấy user ID từ token
        return userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void checkAdminPermission(User user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getNameRoles().equals("ADMIN"));
        if (!isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    // tạo Order - redis
    public OrderResponse createOrder(OrderRequest request) {

        // 1. Lấy thông tin user
        User user = getCurrentAuthenticatedUser();

        Address address = addressRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            throw new AppException(ErrorCode.PHONE_NUMBER_EMPTY);
        }

        // 2. Sắp xếp các VariationId để CHỐNG DEADLOCK
        List<Integer> sortedVariationIds = request.getItems().stream()
                .map(OrderRequest.Item::getVariationId)
                .distinct()
                .sorted()
                .toList();

        // 3. Chuẩn bị MultiLock cho tất cả item trong cart
        List<RLock> locks = sortedVariationIds.stream()
                .map(id -> redissonClient.getLock(VARIATION_LOCK_PREFIX + id))
                .toList();
        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

        boolean isLocked = false;
        try{
            // 4. TRY-LOCK FAIL-FAST (Đợi tối đa 2s)
            isLocked = multiLock.tryLock(2, TimeUnit.SECONDS);
            if(!isLocked) {
                log.warn("User {} bị chặn do tranh chấp mua hàng.", user.getId());
                throw new AppException(ErrorCode.SYSTEM_BUSY, "Hệ thống đang xử lý giao dịch, vui lòng thử lại sau giây lát!");
            }

            // 5. Mở Transaction db bên trong khóa
            Order savedOrder = transactionTemplate.execute(status -> {
                try {
                    validateStockAndPriceWithCache(request);

                    return executeOrderCreationDBLogic(
                            user,
                            address,
                            request,
                            sortedVariationIds
                    );
                }
                catch (Exception e){
                    status.setRollbackOnly();
                    throw e;
//                    try {
//                        throw e;
//                    } catch (InterruptedException ex) {
//                        throw new RuntimeException(ex);
//                    }
                }
            });

            sortedVariationIds.forEach(variationCacheService::clearCache);
            productCacheService.clearListCache();
            clearOrderCache(user.getId());

            return mapToOrderResponse(savedOrder);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        finally {
            // 6. Nhả khóa (Sau khi db commit)
            if(isLocked){
                try{
                    multiLock.unlock();
                } catch (Exception e){
                    log.error("Lỗi unlock: {}", e.getMessage());
                }
            }
        }

    }

    private void validateStockAndPriceWithCache(OrderRequest request){

        List<String> keys = request.getItems().stream()
                .map(item -> VARIATION_CACHE_PREFIX + item.getVariationId())
                .toList();

        List<String> cachedValue = redisService.multiGet(keys);

        if (cachedValue == null) return;

        for(int i = 0; i < request.getItems().size(); i++){
            OrderRequest.Item requestItem = request.getItems().get(i);
            String cachedVariation = cachedValue.get(i);

            if (cachedVariation == null || NULL_VALUE.equals(cachedVariation)) {
                continue;
            }

            try{
                ProductVariationResponse response = objectMapper.readValue(cachedVariation, ProductVariationResponse.class);
                if(response.getStockQuantity() < requestItem.getQuantity()){
                    log.warn("Fail-fast: Variation {} out of stock in cache (Require: {}, Have: {})",
                            response.getId(), requestItem.getQuantity(), response.getStockQuantity());
                    throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
                }
            } catch (JsonProcessingException e){
                log.error("Json parse error");
            }
        }

    }

    private Order executeOrderCreationDBLogic(User user, Address address, OrderRequest request, List<Integer> variationIds) {

        // 1. Lấy tất cả Variations trong 1 câu Query (Dữ liệu thật 100% để trừ kho)
        List<ProductVariation> variations = productVariationRepository.findAllByIdsWithProduct(variationIds);
        Map<Integer, ProductVariation> variationMap = variations.stream()
                .collect(Collectors.toMap(ProductVariation::getId, v -> v));

        // 2. Tận dụng Cache Sale
        List<Integer> productIds = variations.stream()
                .map(v -> v.getProduct().getProductId())
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, BigDecimal> maxDiscountMap = salesCacheService.getLastestDiscountMap(productIds);

        Order order = Order.builder()
                .user(user)
                .fullAddress(address.getStreet() + ", " + address.getWard() + ", " + address.getCity())
                .phoneNumber(user.getPhoneNumber())
                .note(request.getNote())
                .status(OrderStatus.PENDING)
                .build();

        List<OrderDetail> orderDetails = new ArrayList<>();
        double totalAmount = 0;

        for (OrderRequest.Item item : request.getItems()) {
            ProductVariation variation = variationMap.get(item.getVariationId());
            if (variation == null) throw new AppException(ErrorCode.PRODUCT_VARIATION_NOT_FOUND);

            // Kiểm tra tồn kho an toàn
            if (variation.getStockQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            // Lấy discount từ Map (O(1)) thay vì Query DB
            BigDecimal discount = maxDiscountMap.getOrDefault(variation.getProduct().getProductId(), BigDecimal.ZERO);
            Double originalPrice = variation.getProduct().getPrice();
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discount);
            double finalPrice = originalPrice * discountMultiplier.doubleValue();

            // Trừ tồn kho trên memory
            variation.setStockQuantity(variation.getStockQuantity() - item.getQuantity());

            OrderDetail orderDetail = OrderDetail.builder()
                    .order(order)
                    .productVariation(variation)
                    .quantity(item.getQuantity())
                    .price(finalPrice)
                    .build();
            orderDetails.add(orderDetail);

            totalAmount += finalPrice * item.getQuantity();
        }

        order.setOrderDetails(orderDetails);
        order.setTotalAmount(totalAmount);

//        Thread.sleep(10000);

        return orderRepository.save(order);
    }

    // Lấy danh sách đơn hàng của user - Tối ưu với JOIN FETCH
    public List<OrderResponse> getUserOrderList() {

        User user = getCurrentAuthenticatedUser();
        Integer userId = user.getId();
        String cacheKey = ORDER_USER_CACHE + userId;

        List<OrderResponse> cachedList = redisService.getList(cacheKey, new TypeReference<>() {});
        if(cachedList != null){
            return cachedList;
        }

        String lockKey = ORDER_USER_LOCK + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try{
            if(lock.tryLock(5, TimeUnit.SECONDS)){
                try{
                    cachedList = redisService.getList(cacheKey, new TypeReference<>() {});
                    if(cachedList != null){
                        return cachedList;
                    }

                    List<OrderResponse> orders = orderRepository.findByUserWithDetails(user).stream()
                            .map(this::mapToOrderResponse)
                            .toList();

                    redisService.set(cacheKey, orders, Duration.ofMinutes(15));
                    return orders;
                } finally {
                    if(lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }
            } else{
                throw new AppException(ErrorCode.SYSTEM_BUSY);
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

    }

    // Hủy đơn hàng - redis
    public OrderResponse cancelOrder(Integer orderId) {

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        User user = getCurrentAuthenticatedUser();

        Payment payment = order.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            // Nếu đã thanh toán VNPAY, cần hoàn tiền
            if ("VNPAY".equals(payment.getPaymentMethod())) {
                throw new AppException(ErrorCode.CANNOT_CANCEL_PAID_ORDER);
            }
        }

        // Kiểm tra quyền: Admin có thể hủy tất cả, user thường chỉ hủy của mình
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getNameRoles().equals("ADMIN"));
        
        if (!isAdmin && order.getUser().getId() != user.getId()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.APPROVED) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_ORDER);
        }

        // Lấy danh sách ID để tạo khóa hoàn kho
        List<Integer> sortedVariationIds = order.getOrderDetails().stream()
                .map(detail -> detail.getProductVariation().getId())
                .distinct()
                .sorted()
                .toList();

        List<RLock> locks = sortedVariationIds.stream()
                .map(id -> redissonClient.getLock(VARIATION_LOCK_PREFIX + id))
                .toList();
        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

        boolean isLocked = false;
        try{
            isLocked = multiLock.tryLock(2, TimeUnit.SECONDS);

            // Xử lý Hủy đơn trong Transaction DB
            Order canceledOrder = transactionTemplate.execute(status -> {
               if(payment != null && payment.getStatus() == PaymentStatus.COMPLETED){
                   payment.setStatus(PaymentStatus.REFUNDED);
               }

                // Trả lại tồn kho
                for(OrderDetail detail : order.getOrderDetails()){
                    ProductVariation productVariation = detail.getProductVariation();
                    productVariation.setStockQuantity(productVariation.getStockQuantity() + detail.getQuantity());
                }

//                try {
//                    Thread.sleep(100000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }

                order.setStatus(OrderStatus.CANCELED);
                return orderRepository.save(order);
            });

            sortedVariationIds.forEach(variationCacheService::clearCache);
            clearOrderCache(user.getId());

            return mapToOrderResponse(canceledOrder);

        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        } finally {
            if(isLocked){
                try{
                    multiLock.unlock();
                } catch (Exception e){}
            }
        }

    }

    // Chỉnh sửa Order - Có thể cập nhật địa chỉ, số điện thoại và ghi chú
    @Transactional
    public OrderResponse updateOrder(OrderUpdateRequest request, Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Kiểm tra xác thực
        User user = getCurrentAuthenticatedUser();

        // Kiểm tra quyền: Admin có thể cập nhật tất cả, user thường chỉ cập nhật của mình
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getNameRoles().equals("ADMIN"));
        
        if (!isAdmin && order.getUser().getId() != user.getId()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Chỉ có thể cập nhật nếu đơn hàng đang ở trạng thái PENDING & APPROVED
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.APPROVED) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_ORDER);
        }

        // Cập nhật địa chỉ nếu có
        if (request.getAddressId() != null) {
            Address address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
            
            // Kiểm tra địa chỉ có thuộc về user không
            if (address.getUser().getId() != user.getId()) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            
            order.setFullAddress(address.getStreet() + ", " + address.getWard() + ", " + address.getCity());
        }

        // Cập nhật số điện thoại nếu có
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            order.setPhoneNumber(request.getPhoneNumber());
        }

        // Cập nhật ghi chú nếu có
        if (request.getNote() != null) {
            order.setNote(request.getNote());
        }

        Order updatedOrder = orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }

    // Xem orderDetail - Tối ưu với JOIN FETCH
    @Transactional
    public OrderResponse getOrderById(Integer orderId) {
        User currentUser = getCurrentAuthenticatedUser();

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getNameRoles().equals("ADMIN"));
        
        if (!isAdmin && order.getUser().getId() != currentUser.getId()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return mapToOrderResponse(order);
    }

    // Lấy tất cả order từ tất cả user
    @Transactional
    public List<OrderResponse> getAllOrderFromAllUser() {
        User currentUser = getCurrentAuthenticatedUser();

        checkAdminPermission(currentUser);

        return orderRepository.findAllWithDetails().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    // CHỉnh sửa status
    @Transactional
    public OrderResponse updateOrderStatus(Integer orderId, OrderStatus orderStatus) {
        User user = getCurrentAuthenticatedUser();

        checkAdminPermission(user);

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        order.setStatus(orderStatus);
        // check nếu là COMPLETED thì chuyển Payment thành COMPLETED
        if (orderStatus == OrderStatus.COMPLETED) {
            order.getPayment().setStatus(PaymentStatus.COMPLETED);
            clearFeedbackCache(order.getUser().getId());
        }
        Order updatedOrder = orderRepository.save(order);
        clearOrderCache(order.getUser().getId());

        return mapToOrderResponse(updatedOrder);
    }

    // Xóa một order
    @Transactional
   public void deleteOrder(Integer orderId) {
        User user = getCurrentAuthenticatedUser();

        checkAdminPermission(user);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = order.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_PAID_ORDER);
        }

        orderRepository.delete(order);

        clearOrderCache(order.getUser().getId());
    }

    public List<Integer> getPendingFeedbackProductIds(){

        Integer userId = Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
        String cacheKey = PENDING_FEEDBACK_CACHE + userId;

        List<Integer> cached = redisService.getList(cacheKey, new TypeReference<>() {});
        if(cached != null){
            return cached;
        }

        log.info("Cache miss for pending feedback user ID {}, reading from DB...", userId);

        List<Integer> ids = orderRepository.getProductIdsWaitingForReview(userId);
        redisService.set(cacheKey, ids, Duration.ofMinutes(30));
        return ids;

    }

    // Phân trang cho orders của user - Tối ưu cho dữ liệu lớn
    public Page<OrderResponse> getAllOrderByUserIdPaginated(int page, int size) {
        User currentUser = getCurrentAuthenticatedUser();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findByUser(currentUser, pageable);
        
        return orderPage.map(this::mapToOrderResponse);
    }
    
    // Lấy orders theo status - Tối ưu để filter
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        User currentUser = getCurrentAuthenticatedUser();
        checkAdminPermission(currentUser);
        
        return orderRepository.findByStatusWithDetails(status).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public void clearOrderCache(Integer id){

        String cacheKey = ORDER_USER_CACHE + id;
        redisService.delete(cacheKey);
        log.info("Cleared cache for Order User ID: {}", id);

    }

    public void clearFeedbackCache(Integer id){

        String cacheKey = PENDING_FEEDBACK_CACHE + id;
        redisService.delete(cacheKey);
        log.info("Cleared cache for pending feedbacks user ID: {}", id);

    }

    public OrderResponse mapToOrderResponse(Order order) {
        List<OrderResponse.OrderDetailResponse> detailResponses = order.getOrderDetails().stream()
                .map(detail -> OrderResponse.OrderDetailResponse.builder()
                        .productName(detail.getProductVariation().getProduct().getTitle())
                        .productId(detail.getProductVariation().getProduct().getProductId())
                        .variationId(detail.getProductVariation().getId())
                        .color(detail.getProductVariation().getColor())
                        .size(detail.getProductVariation().getSize())
                        .image(detail.getProductVariation().getImage())
                        .price(detail.getPrice())
                        .quantity(detail.getQuantity())
                        .build())
                .collect(Collectors.toList());

        PaymentStatus paymentStatus = null;
        String paymentMethod = null;
        LocalDateTime paymentDate = null;

        if (order.getPayment() != null) {
            paymentStatus = order.getPayment().getStatus();
            paymentMethod = order.getPayment().getPaymentMethod();
            paymentDate = order.getPayment().getPaymentDate();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userFullName(order.getUser().getFullName())
                .fullAddress(order.getFullAddress())
                .phoneNumber(order.getPhoneNumber())
                .note(order.getNote())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .paymentStatus(paymentStatus)
                .paymentMethod(paymentMethod)
                .paymentDate(paymentDate)
                .orderDetails(detailResponses)
                .build();
    }
}
