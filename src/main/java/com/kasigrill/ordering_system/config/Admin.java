package com.kasigrill.ordering_system.config;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Admin {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    Long Id;

    @Column(nullable = false)
    String status;
}
