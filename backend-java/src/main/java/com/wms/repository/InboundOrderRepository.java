package com.wms.repository;

import com.wms.entity.InboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
/**
 * 入库单 Repository — 候选人需要实现
 */
@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {

    @Query("SELECT COUNT(o) FROM InboundOrder o WHERE o.createdAt >= :startOfDay AND o.createdAt < :endOfDay")
    Long countByCreatedAtBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);
    
}
