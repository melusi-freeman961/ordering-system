package com.kasigrill.ordering_system.config;

import com.kasigrill.ordering_system.customer.CustomerMessage;

public interface CustomerNotificationService {



    void publishStoreClosedStatus(String channelId);


    boolean sendMainMenu(String customerIdentifier);



    boolean handleMainManuInput(CustomerMessage message);


    boolean sendHelp(String helpNumber, String customerIdentifier);

    boolean requestMobileNumber(CustomerMessage message);

    boolean requestLocation(String customerIdentifier);

    boolean requestCustomerName(String customerIdentifier);

    boolean publishTerminationConfirmation(String customerIdentifier);

    void publishStatusUpdateToUser(String order, String status);
}
