package com.kasigrill.ordering_system.whatsapp;

import com.kasigrill.ordering_system.menuitem.MenuItemRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class MetaCatalogService {

    // Your System User Token (the one with catalog_management permission)
    @Value("${whatsapp.access.token}")
    private String accessToken;

    // Your specific Meta Catalog ID (found in Commerce Manager)
    @Value("${meta.catalog.id}")
    private String catalogId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void pushToWhatsAppCatalog(MenuItemRequest item, String generatedSku) {
        // The Meta Graph API endpoint for adding products to a catalog
        String url = "https://graph.facebook.com/v25.0/" + catalogId + "/products";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> request = getMapHttpEntity(item, generatedSku, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("✅ Item successfully pushed to WhatsApp Catalog! Meta Response: " + response.getBody());
        } catch (Exception e) {
            System.err.println("❌ Failed to push to Meta Catalog: " + e.getMessage());
        }
    }

    private static HttpEntity<Map<String, Object>> getMapHttpEntity(MenuItemRequest item, String generatedSku, HttpHeaders headers) {
        Map<String, Object> payload = new HashMap<>();

        // 1. The exact SKU we discussed earlier (e.g., "KG-BUR-001")
        payload.put("retailer_id", generatedSku);

        // 2. Map your dashboard data to Meta's required fields
        payload.put("name", item.getTitle());
        payload.put("description", item.getDisc());
        payload.put("image_url", item.getImgUrl());
        payload.put("url", item.getWebsiteLink());

        // Meta expects the price as an integer in cents (e.g., R150.00 becomes 15000)
        int priceInCents = Integer.parseInt(String.valueOf(item.getPrice().multiply(BigDecimal.valueOf(100))));
        payload.put("price", priceInCents);
        payload.put("currency", "ZAR");

        // Meta strictly requires these specific string values
        payload.put("availability", item.isAvailable() ? "in stock" : "out of stock");
        payload.put("condition", "new");

        return new HttpEntity<>(payload, headers);

    }
}
