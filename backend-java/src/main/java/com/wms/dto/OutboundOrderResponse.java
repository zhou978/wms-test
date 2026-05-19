package com.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundOrderResponse {
    private Long id;
    private String orderNo;
    private String customerName;
    private String status;
    private List<InboundOrderItemResponse> items;
    private LocalDateTime createdAt;
}
