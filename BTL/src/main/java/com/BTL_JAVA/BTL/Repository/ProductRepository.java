package com.BTL_JAVA.BTL.Repository;

import com.BTL_JAVA.BTL.Entity.Product.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    @EntityGraph(attributePaths = { "category", "productVariations" })
    @Query("select p from Product p where p.productId = :productId")
    Optional<Product> findWithCategoryAndVariationsByProductId(Integer productId);

    @Query("select p.productId from Product p where p.category.id = :categoryId")
    List<Integer> findIdsByCategoryId(Integer categoryId);
}
