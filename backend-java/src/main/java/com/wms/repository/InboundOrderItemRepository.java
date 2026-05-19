package com.wms.repository;

import com.wms.entity.InboundOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Author: zmq
 * @Date: 2026/5/19 22:47
 * @Version: v1.0.0
 * @Description: TODO
 **/
@Repository
public interface InboundOrderItemRepository extends JpaRepository<InboundOrderItem, Long> {
}
