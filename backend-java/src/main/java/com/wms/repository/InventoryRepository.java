package com.wms.repository;

import com.wms.dto.InventoryResponse;
import com.wms.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndLocationCode(Long productId, String locationCode);

    boolean existsByProductId(Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Inventory i
            SET i.quantity = i.quantity + :quantity,
                i.updatedAt = CURRENT_TIMESTAMP
            WHERE i.productId = :productId
              AND i.locationCode = :locationCode
            """)
    int increaseQuantity(
            @Param("productId") Long productId,
            @Param("locationCode") String locationCode,
            @Param("quantity") Integer quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Inventory i
            SET i.quantity = i.quantity - :quantity,
                i.updatedAt = CURRENT_TIMESTAMP
            WHERE i.productId = :productId
              AND i.locationCode = :locationCode
              AND i.quantity >= :quantity
            """)
    int decreaseQuantityIfEnough(
            @Param("productId") Long productId,
            @Param("locationCode") String locationCode,
            @Param("quantity") Integer quantity);

    @Query("""
            SELECT new com.wms.dto.InventoryResponse(
                i.productId, p.name, p.sku, i.locationCode, w.name, i.quantity, i.updatedAt
            )
            FROM Inventory i, Product p, Location l, Warehouse w
            WHERE p.id = i.productId
              AND l.code = i.locationCode
              AND w.id = l.warehouseId
              AND (:keyword IS NULL OR p.name LIKE %:keyword% OR p.sku LIKE %:keyword%)
              AND (:warehouseId IS NULL OR l.warehouseId = :warehouseId)
            ORDER BY i.updatedAt DESC
            """)
    Page<InventoryResponse> searchInventory(
            @Param("keyword") String keyword,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable);
}
