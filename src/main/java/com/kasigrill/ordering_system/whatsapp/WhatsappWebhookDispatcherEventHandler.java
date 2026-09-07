package com.kasigrill.ordering_system.whatsapp;

import com.kasigrill.ordering_system.menuitem.MenuItemCreatedEvent;
import com.kasigrill.ordering_system.menuitem.MenuItemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WhatsappWebhookDispatcherEventHandler {

    @Autowired
    MetaCatalogService service;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(MenuItemCreatedEvent event) {
        MenuItemRequest menuItem = event.request();
        String sku=event.sku();

        service.pushToWhatsAppCatalog(menuItem,sku);

    }
}
