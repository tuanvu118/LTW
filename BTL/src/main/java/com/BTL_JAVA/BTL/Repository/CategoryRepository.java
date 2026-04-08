package com.BTL_JAVA.BTL.Repository;

import com.BTL_JAVA.BTL.Entity.Product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category,Integer> {

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        LEFT JOIN FETCH c.products p
        LEFT JOIN FETCH p.productVariations
    """)
    List<Category> findAllWithProductsAndVariations();
}
