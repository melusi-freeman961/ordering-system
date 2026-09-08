package com.kasigrill.ordering_system.ors;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class RouteRequest {

    private double restaurantLongitude;
    private double restaurantLatitude;

    private double customerLongitude;
    private double customerLatitude;

}
