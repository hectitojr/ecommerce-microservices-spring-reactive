package com.company.product.dto;

public record ProductAvailabilityResponse(
        Long productId,
        String name,
        Integer requestedQty,
        Integer currentStock,
        Boolean available,
        String message
) {}
