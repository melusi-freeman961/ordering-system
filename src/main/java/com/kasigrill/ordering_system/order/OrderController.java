package com.kasigrill.ordering_system.order;

import com.kasigrill.ordering_system.resturant.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/v2/orders")
public class OrderController {

    @Autowired
    StoreService storeService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<OrderResponse> allOrders = storeService.getAllOrders();

        return ResponseEntity.status(201).body(allOrders);
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusDTO request) {

        OrderResponse updatedOrder =
                storeService.updateStatus(id, request.getStatus());

        if(updatedOrder!=null){
            return ResponseEntity.ok(updatedOrder);
        }
        return ResponseEntity.badRequest().build();
    }
}
