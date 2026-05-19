package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.*;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Location;
import com.wms.entity.OutboundOrder;
import com.wms.entity.Product;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundOrderService {

    private final InventoryRepository inventoryRepository;
    private final OutboundOrderRepository outboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public OutboundOrderResponse createOutboundOrder(OutboundOrderCreateRequest request) {
        String orderNo = generateOutboundOrderNo();

        OutboundOrder outboundOrder = OutboundOrder.builder()
                .orderNo(orderNo)
                .customerName(request.getCustomerName())
                .status("PENDING")
                .build();
        outboundOrder = outboundOrderRepository.save(outboundOrder);

        List<InboundOrderItem> orderItems = new ArrayList<>();
        for (InboundItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException(
                            String.format("商品不存在，商品ID: %d", item.getProductId())));

            Location location = locationRepository.findByCode(item.getLocationCode())
                    .orElseThrow(() -> new BusinessException(
                            String.format("库位不存在，库位编码: %s", item.getLocationCode())));

            int updatedRows = inventoryRepository.decreaseQuantityIfEnough(
                    product.getId(), location.getCode(), item.getQuantity());
            if (updatedRows == 0) {
                throw new BusinessException(String.format(
                        "库存不足，商品ID: %d，库位编码: %s，出库数量: %d",
                        product.getId(), location.getCode(), item.getQuantity()));
            }

            InboundOrderItem orderItem = InboundOrderItem.builder()
                    .orderId(outboundOrder.getId())
                    .productId(product.getId())
                    .locationCode(location.getCode())
                    .quantity(item.getQuantity())
                    .build();
            orderItems.add(orderItem);
        }

        inboundOrderItemRepository.saveAll(orderItems);
        outboundOrder.setStatus("COMPLETED");
        outboundOrder = outboundOrderRepository.save(outboundOrder);

        return toOutboundOrderResponse(outboundOrder, orderItems);
    }

    private OutboundOrderResponse toOutboundOrderResponse(OutboundOrder order, List<InboundOrderItem> items) {
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

        return OutboundOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String generateOutboundOrderNo() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "OUT-" + today + "-";

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Long todayCount = outboundOrderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        return prefix + String.format("%03d", todayCount + 1);
    }
}
