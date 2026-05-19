package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.common.PageResult;
import com.wms.dto.*;
import com.wms.entity.*;
import com.wms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================
 *  候选人需要实现以下两个方法：
 * ============================================
 *
 * 1. createInboundOrder() — 入库单创建（任务1）
 *    要求：
 *    - 生成入库单号（格式 IN-YYYYMMDD-XXX）
 *    - 校验商品和库位是否存在
 *    - 在事务中同时创建入库单和更新库存
 *    - 参数校验已在 DTO 层通过 @Valid 处理
 *
 * 2. queryInventory() — 库存查询（任务2）
 *    要求：
 *    - 支持按商品名称/SKU模糊搜索
 *    - 支持按仓库筛选
 *    - 支持分页
 *    - 返回关联的商品名称和仓库名称
 *    - 注意性能：使用 JOIN 查询而非 N+1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional
    public InboundOrderResponse createInboundOrder(InboundOrderCreateRequest request) {
        String orderNo = generateInboundOrderNo();

        InboundOrder inboundOrder = InboundOrder.builder()
                .orderNo(orderNo)
                .supplierName(request.getSupplierName())
                .status("DRAFT")
                .build();

        inboundOrder = inboundOrderRepository.save(inboundOrder);

        List<InboundOrderItem> orderItems = new ArrayList<>();

        for (InboundItemRequest item : request.getItems()) {
            productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException(
                            String.format("商品不存在，商品ID: %d", item.getProductId())));

            Location location = locationRepository.findByCode(item.getLocationCode())
                    .orElseThrow(() -> new BusinessException(
                            String.format("库位不存在，库位编码: %s", item.getLocationCode())));

            InboundOrderItem orderItem = InboundOrderItem.builder()
                    .orderId(inboundOrder.getId())
                    .productId(item.getProductId())
                    .locationCode(location.getCode())
                    .quantity(item.getQuantity())
                    .build();
            orderItems.add(orderItem);
        }

        inboundOrderItemRepository.saveAll(orderItems);
        for (InboundOrderItem orderItem : orderItems) {
            updateInventory(orderItem.getProductId(), orderItem.getLocationCode(), orderItem.getQuantity());
        }

        //后续可以使用枚举值，避免魔法值
        inboundOrder.setStatus("COMPLETED");
        inboundOrder = inboundOrderRepository.save(inboundOrder);

        return toInboundOrderResponse(inboundOrder, orderItems);
    }

    public PageResult<InventoryResponse> queryInventory(String keyword, Long warehouseId,
                                                         int page, int pageSize) {
        if (warehouseId != null && !warehouseRepository.existsById(warehouseId)) {
            throw new BusinessException(String.format("仓库不存在，仓库ID: %d", warehouseId));
        }

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<InventoryResponse> result = inventoryRepository.searchInventory(
                kw,
                warehouseId,
                PageRequest.of(safePage - 1, safePageSize));

        return new PageResult<>(
                result.getContent(),
                result.getTotalElements(),
                safePage,
                safePageSize);
    }

    private void updateInventory(Long productId, String locationCode, Integer quantity) {
        int updatedRows = inventoryRepository.increaseQuantity(productId, locationCode, quantity);
        if (updatedRows > 0) {
            return;
        }

        try {
            inventoryRepository.saveAndFlush(Inventory.builder()
                    .productId(productId)
                    .locationCode(locationCode)
                    .quantity(quantity)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 并发场景下，其他事务可能已插入同一 productId + locationCode 的库存记录。
            // 此时回退为数据库原子递增，避免读-改-写导致库存增量丢失。
            updatedRows = inventoryRepository.increaseQuantity(productId, locationCode, quantity);
            if (updatedRows == 0) {
                throw e;
            }
        }
    }

    private InboundOrderResponse toInboundOrderResponse(InboundOrder order, List<InboundOrderItem> items) {
        Map<Long, String> productNames = productRepository.findAllById(
                        items.stream().map(InboundOrderItem::getProductId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        List<InboundOrderItemResponse> itemResponses = items.stream()
                .map(item -> InboundOrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(productNames.get(item.getProductId()))
                        .quantity(item.getQuantity())
                        .locationCode(item.getLocationCode())
                        .build())
                .toList();

        return InboundOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .supplierName(order.getSupplierName())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String generateInboundOrderNo() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "IN-" + today + "-";

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Long todayCount = inboundOrderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        return prefix + String.format("%03d", todayCount + 1);
    }
}
