package com.kasigrill.ordering_system.order;

import com.kasigrill.ordering_system.menuitem.MenuItem;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class OrderItem {

    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Id
    Long id;

    int quantity;
    @ManyToOne(
            cascade = CascadeType.PERSIST
    )
    @JoinColumn(
            name = "order_id"
    )
    CustomerOrder order;
    @ManyToOne(
            optional = false
    )
    @JoinColumn(
            name = "menuItem"
            , nullable = false
    )
    private MenuItem menuItem;

}
