package com.company.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(

        @NotNull(message = "El id de producto es obligatorio.")
        Long productId,

        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad mínima debe ser 1.")
        Integer quantity
) {}
