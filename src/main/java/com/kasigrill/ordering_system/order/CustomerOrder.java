package com.kasigrill.ordering_system.order;

import com.kasigrill.ordering_system.customer.Customer;
import com.kasigrill.ordering_system.customer.CustomerSession;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class CustomerOrder {

    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Id
    Long id;

    @Column(nullable = false)
    int orderNumber;

    @Column(nullable = false)
    BigDecimal orderAmount;

    @Column(
            nullable = false
    )
    String status;


    @ManyToOne(
            optional = false, cascade = CascadeType.PERSIST
    )
    @JoinColumn(
            name = "customerId"
    )
    Customer customer;
    @Column(
            nullable = false
    )
    LocalDateTime createdDate = LocalDateTime.now();


    @OneToMany(
            mappedBy = "order"
    )
    @ToString.Exclude
    List<OrderItem> orderItems=new ArrayList<>();


    @OneToOne(
            mappedBy = "customerOrder"
    )
    @ToString.Exclude
    CustomerSession session;

}
