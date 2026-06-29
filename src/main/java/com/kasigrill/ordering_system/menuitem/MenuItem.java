package com.kasigrill.ordering_system.menuitem;

import com.kasigrill.ordering_system.order.OrderItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Data
public class MenuItem {

    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    @Id
    Long id;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    boolean available;

    BigDecimal price;

    @Column(nullable = false)
    String status;

    @OneToMany(
            mappedBy = "menuItem"
    )
    private List<OrderItem> orderItems = new ArrayList<>();


}
