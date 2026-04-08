package com.BTL_JAVA.BTL.Repository;

import com.BTL_JAVA.BTL.Entity.Product.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    // Lấy giỏ hàng theo UserID (Dùng JOIN FETCH để lấy luôn thông tin Variation và Product)
    @Query("""
        SELECT c FROM Cart c
        JOIN FETCH c.productVariation pv
        JOIN FETCH pv.product
        WHERE c.user.id = :userId
    """)
    List<Cart> findByUserId(@Param("userId") Integer userId);

    // Tìm 1 món cụ thể trong giỏ của user
    Optional<Cart> findByUserIdAndProductVariationId(Integer userId, Integer variationId);

    @Modifying
    @Query("DELETE FROM Cart c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

}