package _2.LTW.service;

import _2.LTW.dto.request.CareBookingRequest.CareBookingCreateRequest;
import _2.LTW.dto.request.CareBookingRequest.CareBookingServiceItemRequest;
import _2.LTW.dto.request.CareBookingRequest.CareBookingUpdateRequest;
import _2.LTW.dto.response.CareBookingResponse;
import _2.LTW.entity.*;
import _2.LTW.entity.CareBooking.CareBooking;
import _2.LTW.entity.CareBooking.CareBookingServiceItem;
import _2.LTW.entity.CareBooking.CareBookingStatus;
import _2.LTW.entity.Pets.Pets;
import _2.LTW.enums.RoleEnum;
import _2.LTW.enums.ShiftType;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.CareBookingMapper;
import _2.LTW.repository.*;
import _2.LTW.util.SecurityUtil;
import _2.LTW.validate.BookingDateTimeValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CareBookingService {

    CareBookingRepository careBookingRepository;
    CareServiceRepository careServiceRepository;
    PetRepository petRepository;
    UserRepository userRepository;
    UserRoleRepository userRoleRepository;
    BookingDateTimeValidator bookingDateTimeValidator;
    CareBookingMapper careBookingMapper;
    SecurityUtil securityUtil;

    @Transactional
    public CareBookingResponse createBooking(CareBookingCreateRequest request) {
        bookingDateTimeValidator.validate(request.getBookingDate(), request.getStartTime());

        Pets pet = getPetAndValidateOwner(request.getPetId());
        User doctor = getDoctorAndValidateRole(request.getDoctorId());

        Map<Long, CareService> serviceMap = getCareServiceMap(request.getServices());
        int totalDuration = calculateTotalDuration(request.getServices(), serviceMap);
        LocalTime estimatedEndTime = request.getStartTime().plusMinutes(totalDuration);

        validateWithinSingleShift(request.getStartTime(), estimatedEndTime);
        ensureNoBookingOverlap(doctor.getId(), request.getBookingDate(), request.getStartTime(), estimatedEndTime, null);

        CareBooking booking = careBookingMapper.toCareBooking(request);
        booking.setPet(pet);
        booking.setDoctor(doctor);
        booking.setStatus(CareBookingStatus.PENDING);
        booking.setCreatedBy(securityUtil.getCurrentUser());

        List<CareBookingServiceItem> details = buildBookingDetails(request, booking, serviceMap);
        booking.setCareBookingServices(details);

        CareBooking saved = careBookingRepository.save(booking);
        return careBookingMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CareBookingResponse> getMyBookings() {
        return careBookingMapper.toResponses(
                careBookingRepository.findAllByCreatorId(securityUtil.getCurrentUserId())
        );
    }

    @Transactional(readOnly = true)
    public CareBookingResponse getMyBookingDetail(Long bookingId) {
        CareBooking booking = getBookingOrThrow(bookingId);

        if (booking.getCreatedBy() == null || !booking.getCreatedBy().getId().equals(securityUtil.getCurrentUserId())) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền xem booking này");
        }

        return careBookingMapper.toResponse(booking);
    }

    @Transactional
    public CareBookingResponse cancelMyBooking(Long bookingId) {
        CareBooking booking = getBookingOrThrow(bookingId);

        if (booking.getCreatedBy() == null || !booking.getCreatedBy().getId().equals(securityUtil.getCurrentUserId())) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền hủy booking này");
        }

        if (booking.getStatus() != CareBookingStatus.PENDING) {
            throw ErrorCode.CARE_BOOKING_CANNOT_UPDATE.toException("Chỉ được hủy booking chưa xử lý");
        }

        booking.setStatus(CareBookingStatus.CANCELLED);
        return careBookingMapper.toResponse(careBookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<CareBookingResponse> getDoctorBookings(LocalDate date, String status) {
        return careBookingMapper.toResponses(
                careBookingRepository.findAllByDoctorId(
                        securityUtil.getCurrentUserId(),
                        date,
                        parseStatus(status)
                )
        );
    }

    @Transactional(readOnly = true)
    public CareBookingResponse getDoctorBookingDetail(Long bookingId) {
        CareBooking booking = getBookingOrThrow(bookingId);

        if (!booking.getDoctor().getId().equals(securityUtil.getCurrentUserId())) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền xem booking này");
        }

        return careBookingMapper.toResponse(booking);
    }

    @Transactional
    public CareBookingResponse updateDoctorBookingStatus(Long bookingId, String status) {
        CareBooking booking = getBookingOrThrow(bookingId);

        if (!booking.getDoctor().getId().equals(securityUtil.getCurrentUserId())) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền cập nhật booking này");
        }

        CareBookingStatus newStatus = parseStatus(status);
        if (newStatus == null) {
            throw ErrorCode.CARE_BOOKING_INVALID_STATUS.toException();
        }

        List<CareBookingStatus> allowedTargets = List.of(
                CareBookingStatus.CONFIRMED,
                CareBookingStatus.IN_PROGRESS,
                CareBookingStatus.COMPLETED
        );
        if (!allowedTargets.contains(newStatus)) {
            throw ErrorCode.CARE_BOOKING_INVALID_STATUS.toException(
                    "Bác sĩ chỉ được cập nhật CONFIRMED, IN_PROGRESS hoặc COMPLETED"
            );
        }

        if (booking.getStatus() == CareBookingStatus.CANCELLED || booking.getStatus() == CareBookingStatus.COMPLETED) {
            throw ErrorCode.CARE_BOOKING_CANNOT_UPDATE.toException("Booking đã kết thúc, không thể cập nhật");
        }

        booking.setStatus(newStatus);
        return careBookingMapper.toResponse(careBookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<CareBookingResponse> getAllBookings() {
        return careBookingMapper.toResponses(careBookingRepository.findAllActive());
    }

    @Transactional
    public CareBookingResponse adminUpdateBooking(Long bookingId, CareBookingUpdateRequest request) {
        CareBooking booking = getBookingOrThrow(bookingId);

        User doctor = booking.getDoctor();
        if (request.getDoctorId() != null) {
            doctor = getDoctorAndValidateRole(request.getDoctorId());
            booking.setDoctor(doctor);
        }

        LocalDate bookingDate = request.getBookingDate() != null ? request.getBookingDate() : booking.getBookingDate();
        LocalTime startTime = request.getStartTime() != null ? request.getStartTime() : booking.getStartTime();

        bookingDateTimeValidator.validate(bookingDate, startTime);

        int totalDuration = careBookingMapper.toTotalDuration(booking);
        LocalTime estimatedEndTime = startTime.plusMinutes(totalDuration);

        validateWithinSingleShift(startTime, estimatedEndTime);
        ensureNoBookingOverlap(doctor.getId(), bookingDate, startTime, estimatedEndTime, booking.getId());

        booking.setBookingDate(bookingDate);
        booking.setStartTime(startTime);

        if (request.getNotes() != null) {
            booking.setNotes(request.getNotes());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            booking.setStatus(parseStatus(request.getStatus()));
        }

        return careBookingMapper.toResponse(careBookingRepository.save(booking));
    }

    @Transactional
    public void deleteBooking(Long bookingId) {
        CareBooking booking = getBookingOrThrow(bookingId);
        booking.setDeleteAt(java.time.LocalDateTime.now());
        careBookingRepository.save(booking);
    }

    private CareBooking getBookingOrThrow(Long bookingId) {
        return careBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ErrorCode.CARE_BOOKING_NOT_FOUND.toException());
    }

    private Pets getPetAndValidateOwner(Integer petId) {
        Pets pet = petRepository.findActiveById(petId)
                .orElseThrow(() -> ErrorCode.PET_NOT_FOUND.toException("Không tìm thấy thú cưng"));

        if (!pet.getUser().getId().equals(securityUtil.getCurrentUserId())) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn chỉ có thể đặt lịch cho thú cưng của mình");
        }

        return pet;
    }

    private User getDoctorAndValidateRole(Long doctorId) {
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException("Không tìm thấy bác sĩ"));

        boolean isDoctor = userRoleRepository.findByUser_Id(doctor.getId()).stream()
                .map(UserRole::getRole)
                .map(Role::getRoleEnum)
                .anyMatch(role -> role == RoleEnum.DOCTOR);

        if (!isDoctor) {
            throw ErrorCode.BAD_REQUEST.toException("Người được chọn không phải bác sĩ");
        }

        return doctor;
    }

    private Map<Long, CareService> getCareServiceMap(List<CareBookingServiceItemRequest> items) {
        List<Long> ids = items.stream()
                .map(CareBookingServiceItemRequest::getCareServiceId)
                .distinct()
                .toList();

        List<CareService> services = careServiceRepository.findAllById(ids).stream()
                .filter(service -> Boolean.TRUE.equals(service.getIsActive()))
                .toList();

        if (services.size() != ids.size()) {
            throw ErrorCode.NOT_FOUND.toException("Có dịch vụ chăm sóc không tồn tại hoặc không hoạt động");
        }

        return services.stream().collect(Collectors.toMap(CareService::getId, Function.identity()));
    }

    private int calculateTotalDuration(List<CareBookingServiceItemRequest> items, Map<Long, CareService> serviceMap) {
        return items.stream()
                .mapToInt(item -> serviceMap.get(item.getCareServiceId()).getDurationMinutes())
                .sum();
    }

    private List<CareBookingServiceItem> buildBookingDetails(
            CareBookingCreateRequest request,
            CareBooking booking,
            Map<Long, CareService> serviceMap) {
        return request.getServices().stream().map(item -> {
            CareBookingServiceItem detail = careBookingMapper.toCareBookingServiceItem(item);
            CareService master = serviceMap.get(item.getCareServiceId());
            detail.setCareBooking(booking);
            detail.setCareService(master);
            detail.setDurationMinutes(master.getDurationMinutes());
            detail.setPrice(master.getPrice());
            return detail;
        }).toList();
    }

    private void ensureNoBookingOverlap(
            Long doctorId,
            LocalDate bookingDate,
            LocalTime newStart,
            LocalTime newEnd,
            Long ignoredBookingId
    ) {
        List<CareBooking> bookings = careBookingRepository.findDoctorBookingsInDate(
                doctorId,
                bookingDate,
                List.of(CareBookingStatus.PENDING, CareBookingStatus.CONFIRMED, CareBookingStatus.IN_PROGRESS)
        );

        boolean overlapped = bookings.stream()
                .filter(existing -> ignoredBookingId == null || !existing.getId().equals(ignoredBookingId))
                .anyMatch(existing -> existing.getStartTime().isBefore(newEnd)
                        && careBookingMapper.toEstimatedEndTime(existing).isAfter(newStart));

        if (overlapped) {
            throw ErrorCode.CONFLICT.toException("Bác sĩ đã bận trong khoảng thời gian này");
        }
    }

    private void validateWithinSingleShift(LocalTime startTime, LocalTime estimatedEndTime) {
        ShiftType startShift = calculateShiftType(startTime);
        ShiftType endShift = calculateShiftType(estimatedEndTime.minusMinutes(1));

        if (startShift != endShift) {
            throw ErrorCode.BAD_REQUEST.toException("Tổng thời lượng dịch vụ vượt quá phạm vi 1 ca khám");
        }
    }

    private ShiftType calculateShiftType(LocalTime startTime) {
        if (startTime.isBefore(LocalTime.of(12, 30))) {
            return ShiftType.MORNING;
        }
        return ShiftType.AFTERNOON;
    }

    private CareBookingStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }

        String normalized = rawStatus.trim().toUpperCase();
        if ("DONE".equals(normalized)) {
            normalized = "COMPLETED";
        }

        try {
            return CareBookingStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw ErrorCode.CARE_BOOKING_INVALID_STATUS.toException();
        }
    }
}

