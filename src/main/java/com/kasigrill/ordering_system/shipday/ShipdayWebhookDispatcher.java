package com.kasigrill.ordering_system.shipday;

import com.kasigrill.ordering_system.customer.Customer;
import com.kasigrill.ordering_system.customer.CustomerCreatedEvent;
import com.kasigrill.ordering_system.order.CustomerOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ShipdayWebhookDispatcher {

    @Autowired
    ShipDayDeliveryService shipDayService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(CustomerCreatedEvent event) {
        CustomerOrder savedOrder = event.order();


        ShipDayOrderRequest deliveryDto = createDeliveryDto(savedOrder);
        shipDayService.dispatchOrder(deliveryDto);
    }
    private ShipDayOrderRequest createDeliveryDto(CustomerOrder order) {

        Customer customer = order.getCustomer();
        String address = customer.getLocation();

        String customerName = customer.getName();
        String customerPhoneNumber = customer.getMobile();
        String restaurantName = "GoBite";
        String restaurantAddress = address;
        double totalOrderCost = Double.parseDouble(String.valueOf(order.getOrderAmount()));
        String deliveryInstructions = "hhfjwekjjijiuehh";


        ShipDayOrderRequest request = new ShipDayOrderRequest();
        request.setCustomerAddress(address);
        request.setDeliveryInstructions(deliveryInstructions);
        request.setCustomerName(customerName);
        request.setOrderNumber(order.getOrderNumber());
        request.setRestaurantAddress(restaurantAddress);
        request.setCustomerPhoneNumber(customerPhoneNumber);
        request.setRestaurantName(restaurantName);
        request.setTotalOrderCost(totalOrderCost);

        return request;
    }
}
