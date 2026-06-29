package com.kasigrill.ordering_system.order;

import com.kasigrill.ordering_system.customer.Customer;
import com.kasigrill.ordering_system.customer.CustomerSession;
import com.kasigrill.ordering_system.telegram.VendorMessageData;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
public class CustomerOrder {

    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Id
    Long id;

    @Column(
            nullable = false
    )
    String status;


    @ManyToOne(
            optional = false, cascade = CascadeType.PERSIST
    )
    @JoinColumn(
            name = "contact"
    )
    Customer customer;
    @Column(
            nullable = false
    )
    LocalDateTime createdDate = LocalDateTime.now();


    @OneToOne(mappedBy = "order")
    OrderItem orderItem;


    @OneToOne(
            mappedBy = "customerOrder"
    )
    @ToString.Exclude
    CustomerSession session;
    @Embedded
    private VendorMessageData vendorMessageData;
}
