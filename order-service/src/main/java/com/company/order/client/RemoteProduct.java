package com.company.order.client;

public record RemoteProduct(
        Long id,
        String name,
        Double price,
        Integer stock,
        Boolean active
) {}
