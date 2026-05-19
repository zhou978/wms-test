package com.wms.repository;

import com.wms.entity.OutboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {

    @Query("SELECT COUNT(o) FROM OutboundOrder o WHERE o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    Long countByCreatedAtBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
