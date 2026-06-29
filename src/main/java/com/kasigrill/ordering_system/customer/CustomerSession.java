package com.kasigrill.ordering_system.customer;

import com.kasigrill.ordering_system.order.CustomerOrder;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
public class CustomerSession {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerSessionState state;

    @OneToOne(
            optional = false, cascade = CascadeType.ALL
    )
    @JoinColumn(
            name = "customer"
    )
    @ToString.Exclude
    Customer customer;


    @OneToOne(
            cascade = CascadeType.PERSIST
    )
    @JoinColumn(
            name = "orderId"
    )
    @ToString.Exclude
    CustomerOrder customerOrder;
}
