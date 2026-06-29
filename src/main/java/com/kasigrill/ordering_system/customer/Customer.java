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

    @Column(
            unique = true
    )
    String mobile;

    String status;

    String name;

    String location;

    @OneToMany(
            mappedBy = "customer"
    )
    @ToString.Exclude
    List<CustomerOrder> orders = new ArrayList<>();

    @OneToOne(
            mappedBy = "customer"
    )
    @ToString.Exclude
    CustomerSession session;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "identifier_channel_id")
    @ToString.Exclude
    CustomerIdentifier identifier;

}
