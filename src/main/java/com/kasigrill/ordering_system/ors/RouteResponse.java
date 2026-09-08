package com.kasigrill.ordering_system.ors;

public class RouteResponse {
    private double distanceKm;
    private double durationMinutes;

    public RouteResponse(double distanceKm, double durationMinutes) {
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getDurationMinutes() {
        return durationMinutes;
    }
}
