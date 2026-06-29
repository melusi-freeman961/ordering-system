package com.kasigrill.ordering_system.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

@Component
public class TelegramWebhookRegistrar {

    private final RestClient restClient;
    @Value("${telegram.bot.token}")
    private String botToken;
    @Value("${telegram.bot.webhook}")
    private String baseWebhookUrl;

    // Inject Spring's standard RestClient builder
    public TelegramWebhookRegistrar(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhookOnStartup() {

        String telegramApiUrl = "https://api.telegram.org/bot" + botToken + "/setWebhook";

        String fullWebhookUrl = baseWebhookUrl + "/" + botToken;

        Map<String, String> requestBody = Map.of("url", fullWebhookUrl);

        try {
            // Execute the equivalent of your cURL command
            TelegramResponse response = restClient.post()
                    .uri(telegramApiUrl)
                    .body(requestBody)
                    .retrieve()
                    .body(TelegramResponse.class);


        } catch (Exception e) {

        }
    }


    public record TelegramResponse(boolean ok, String description, Message result) {
    }
}
