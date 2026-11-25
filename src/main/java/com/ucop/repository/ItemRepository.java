package com.ucop.repository;

import com.ucop.entity.Item;
import com.ucop.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCase(String sku);
    List<Item> findByStatusOrderByIdDesc(String status);

    @Query("SELECT i FROM Item i WHERE " +
           "(:categoryId IS NULL OR i.category.id = :categoryId) " +
           "AND (:itemType IS NULL OR i.itemType = :itemType) " +
           "AND (:keyword IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY i.id DESC")
    List<Item> searchItems(@Param("categoryId") Long categoryId,
                           @Param("itemType") ItemType itemType,
                           @Param("keyword") String keyword);
}
