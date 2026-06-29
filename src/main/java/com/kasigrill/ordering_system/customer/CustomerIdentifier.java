package com.kasigrill.ordering_system.customer;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class CustomerIdentifier {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    Long id;

    @Column(unique = true)
    private String channelId;

    @OneToOne(mappedBy = "identifier")
    private Customer customer;
}
