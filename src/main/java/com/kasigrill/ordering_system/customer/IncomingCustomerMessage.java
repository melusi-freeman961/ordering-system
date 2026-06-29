package com.kasigrill.ordering_system.customer;

import java.util.Map;

public interface IncomingCustomerMessage {

    String getItemId();
    String getChannelId();
    String getMessage();
    Map<String,Object> getLocation();
}
