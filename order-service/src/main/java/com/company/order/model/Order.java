package com.company.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("orders")
public record Order(
        @Id
        @Column("ID")
        Long id,

        @Column("PRODUCT_ID")
        Long productId,

        @Column("QUANTITY")
        Integer quantity,

        @Column("TOTAL")
        Double total,

        @Column("STATUS")
        OrderStatus status
) {}
