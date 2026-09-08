package com.kasigrill.ordering_system.ors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouteServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ors.api.key}")
    private String apiKey;

    @Value("${ors.base-url}")
    private String baseUrl;

    public OpenRouteServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public RouteResponse calculateRoute(
            double restaurantLongitude,
            double restaurantLatitude,
            double customerLongitude,
            double customerLatitude
    ) {

        String url = baseUrl + "/v2/directions/foot-walking";

        // ORS expects [longitude, latitude]
        List<List<Double>> coordinates = List.of(
                List.of(restaurantLongitude, restaurantLatitude),
                List.of(customerLongitude, customerLatitude)
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("coordinates", coordinates);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", apiKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        Map.class
                );

        System.out.println("ORS STATUS: " + response.getStatusCode());
        System.out.println("ORS RESPONSE: " + response.getBody());
        Map body = response.getBody();

        if (body == null) {
            throw new RuntimeException("Empty response from OpenRouteService");
        }

        List<Map<String, Object>> routes =
                (List<Map<String, Object>>) body.get("routes");

        Map<String, Object> route = routes.getFirst();

        Map<String, Object> summary =
                (Map<String, Object>) route.get("summary");

        double distanceMeters =
                ((Number) summary.get("distance")).doubleValue();

        double durationSeconds =
                ((Number) summary.get("duration")).doubleValue();

        double distanceKm = distanceMeters / 1000.0;

        double durationMinutes = durationSeconds / 60.0;

        return new RouteResponse(
                distanceKm,
                durationMinutes
        );
    }
}
