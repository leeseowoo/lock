package com.example.lock.optimistic;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double price;

    // 낙관적 락을 위한 버전 필드
    @Version
    private Integer version;
}
