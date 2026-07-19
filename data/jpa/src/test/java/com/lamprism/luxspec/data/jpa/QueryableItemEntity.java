package com.lamprism.luxspec.data.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "queryable_items")
class QueryableItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int score;

    protected QueryableItemEntity() {
    }

    QueryableItemEntity(String name, String category, int score) {
        this.name = name;
        this.category = category;
        this.score = score;
    }

    String name() {
        return name;
    }
}
