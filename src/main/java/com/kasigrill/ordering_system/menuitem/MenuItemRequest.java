package com.kasigrill.ordering_system.menuitem;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MenuItemRequest {
    String title;
    boolean available;
    BigDecimal price;
    String disc;
    String imgUrl;
    String websiteLink;

    public MenuItemRequest(String title, boolean available, BigDecimal price, String disc, String imgUrl, String websiteLink) {
        this.title = title;
        this.available = available;
        this.price = price;
        this.disc = disc;
        this.imgUrl = imgUrl;
        this.websiteLink = websiteLink;
    }
}
