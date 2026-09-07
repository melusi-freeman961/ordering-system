package com.kasigrill.ordering_system.customer;

import com.kasigrill.ordering_system.order.CustomerOrder;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Customer {

    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Id
    Long id;

    String mobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CustomerStatus status;

    String name;

    String location;

    @OneToMany(
            mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true
    )
    @ToString.Exclude
    List<CustomerOrder> orders = new ArrayList<>();

    @OneToOne(
            mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true
    )
    @ToString.Exclude
    CustomerSession session;

    @Column(
            nullable = false,
            unique = true
    )
    String customerIdentifierId;


}
