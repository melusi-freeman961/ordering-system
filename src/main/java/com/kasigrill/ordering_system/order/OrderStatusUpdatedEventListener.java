package com.kasigrill.ordering_system.order;

import com.kasigrill.ordering_system.resturant.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderStatusUpdatedEventListener {

    @Autowired
    StoreService service;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderStatusUpdatedEvent event) {
        CustomerOrder order=event.order();
        String status= event.status();
        service.publishStatusUpdate(order,status);
    }
}
