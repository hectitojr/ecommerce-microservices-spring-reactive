package com.company.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotBlank(message = "El nombre del producto es obligatorio.")
        String name,

        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo.")
        Double price,

        @NotNull(message = "El stock es obligatorio.")
        @Min(value = 0, message = "El stock no puede ser negativo.")
        Integer stock
) {}
