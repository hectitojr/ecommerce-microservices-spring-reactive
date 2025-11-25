package com.company.product.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("products")
public record Product(
        @Id
        @Column("ID")
        Long id,

        @Column("NAME")
        String name,

        @Column("PRICE")
        Double price,

        @Column("STOCK")
        Integer stock,

        @Column("ACTIVE")
        Boolean active
) {}

