package com.kasigrill.ordering_system.whatsapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook")
public class WhatsAppWebhookController {

    @Autowired
    WhatsAppService whatsAppService;

    @Autowired
    TaskExecutor taskExecutor;

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {

        System.out.println("checking................................");

        taskExecutor.execute(() -> {
            whatsAppService.deserializeAndProcessWhatsappMessage(payload);
        });

        return ResponseEntity.ok().build();

    }

}
