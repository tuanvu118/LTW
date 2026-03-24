package com.BTL_JAVA.BTL.Repository;

import com.BTL_JAVA.BTL.Entity.Orders.Order;
import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.productVariation pv " +
           "LEFT JOIN FETCH pv.product " +
           "LEFT JOIN FETCH o.user " +
           "WHERE o.user = :user")
    List<Order> findByUserWithDetails(@Param("user") User user);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.productVariation pv " +
           "LEFT JOIN FETCH pv.product " +
           "LEFT JOIN FETCH o.user " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Integer orderId);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.productVariation pv " +
           "LEFT JOIN FETCH pv.product " +
           "LEFT JOIN FETCH o.user")
    List<Order> findAllWithDetails();

    Page<Order> findByUser(User user, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.productVariation pv " +
           "LEFT JOIN FETCH pv.product " +
           "LEFT JOIN FETCH o.user " +
           "WHERE o.status = :status")
    List<Order> findByStatusWithDetails(@Param("status") OrderStatus status);
}
