package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.common.PageResult;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderResponse;
import com.wms.dto.InventoryResponse;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.dto.OutboundOrderResponse;
import com.wms.service.InventoryService;
import com.wms.service.OutboundOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final OutboundOrderService outboundOrderService;

    @PostMapping("/inbound-orders")
    public ApiResponse<InboundOrderResponse> createInboundOrder(
            @Valid @RequestBody InboundOrderCreateRequest request) {
        InboundOrderResponse order = inventoryService.createInboundOrder(request);
        return new ApiResponse<>(201, "入库单创建成功", order);
    }

    @PostMapping("/outbound-orders")
    public ApiResponse<OutboundOrderResponse> createOutboundOrder(
            @Valid @RequestBody OutboundOrderCreateRequest request) {
        OutboundOrderResponse order = outboundOrderService.createOutboundOrder(request);
        return new ApiResponse<>(201, "出库单创建成功", order);
    }

    @GetMapping("/inventory")
    public ApiResponse<PageResult<InventoryResponse>> queryInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(inventoryService.queryInventory(keyword, warehouseId, page, pageSize));
    }
}
