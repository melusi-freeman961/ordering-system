package com.kasigrill.ordering_system.telegram;

import com.kasigrill.ordering_system.resturant.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/telegram")
public class TelegramWebhookController {
    @Autowired
    TelegramService telegramService;

    @PostMapping("/webhook/{token}")
    public ResponseEntity<Void> receiveUpdate(@RequestBody Map<String, Object> payload) {

        System.out.println(payload.keySet()+" +++++++++++++++");
        telegramService.deserializeAndProcessMessage(payload);
        return ResponseEntity.ok().build();
    }

}
