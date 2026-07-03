package com.kasigrill.ordering_system.shipday;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class ShipDayDeliveryService {

    private final RestClient restClient;


    public ShipDayDeliveryService(@Value("${shipday.api.key}") String shipDayApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.shipday.com")
                // Manually inject the exact header key format Shipday expects
                .defaultHeader("Authorization", "Basic " + shipDayApiKey.trim())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ShipDayResponse dispatchOrder(ShipDayOrderRequest orderPayload){

        try{
            return restClient.post()
                    .uri("/orders")
                    .body(orderPayload)
                    .retrieve()
                    .body(ShipDayResponse.class);

        }
        catch (Exception e){
            e.printStackTrace();

            throw new RuntimeException("Failed to dispatch order: " + e.getMessage(), e);
        }
    }
}
