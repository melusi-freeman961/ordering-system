package com.kasigrill.ordering_system.customer;

public class CustomerMessageImp implements CustomerMessage{

   public  Object message;
   public String customerIdentifierId;

    @Override
    public String getCustomerIdentifier() {
        return customerIdentifierId;
    }

    @Override
    public Object getCustomerMessage() {
        return message;
    }
}
