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

    @ManyToOne(
            optional = false
    )
    @JoinColumn(
            name = "menuItem"
            , nullable = false
    )
    private MenuItem menuItem;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    CustomerOrder order;

}
