/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.data.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "queryable_items")
class QueryableItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int score;

    @Column(name = "recorded_at", nullable = false)
    private Instant timestamp;

    protected QueryableItemEntity() {
    }

    QueryableItemEntity(String name, String category, int score) {
        this(name, category, score, Instant.EPOCH);
    }

    QueryableItemEntity(String name, String category, int score, Instant timestamp) {
        this.name = name;
        this.displayName = name;
        this.category = category;
        this.score = score;
        this.timestamp = timestamp;
    }

    String name() {
        return name;
    }
}
