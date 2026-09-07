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
    private BotState state;

    @OneToOne(
            optional = false
    )
    @JoinColumn(
            name = "customerId"
    )
    @ToString.Exclude
    Customer customer;

}
