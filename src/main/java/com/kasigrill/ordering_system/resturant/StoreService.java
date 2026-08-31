package com.kasigrill.ordering_system.resturant;

import com.kasigrill.ordering_system.config.CustomerNotificationService;
import com.kasigrill.ordering_system.customer.*;
import com.kasigrill.ordering_system.menuitem.MenuItem;
import com.kasigrill.ordering_system.menuitem.MenuItemRequest;
import com.kasigrill.ordering_system.menuitem.MenuRepository;
import com.kasigrill.ordering_system.order.*;
import com.kasigrill.ordering_system.shipday.ShipDayOrderRequest;
import com.kasigrill.ordering_system.whatsapp.MetaCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.kasigrill.ordering_system.customer.BotState.*;

@Service
public class StoreService {

    private static final LocalTime OPENING_TIME = LocalTime.of(10, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(23, 50);
    private static int orderNumber;
    private final CustomerNotificationService customerNotService;
    private final CustomerRepository customerRepository;
    private final SessionRepository sessionRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final Map<String, Customer> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, List<OrderItem>> activeSessionsOrderItems = new ConcurrentHashMap<>();
    private final Map<String, CustomerOrder> activeSessionsOrders = new ConcurrentHashMap<>();
    String helpNumber;
    private MetaCatalogService metaCatalogService;


    public StoreService(CustomerNotificationService service1
            , CustomerRepository customerRepository
            , SessionRepository sessionRepository
            , MenuRepository menuRepository
            , OrderRepository orderRepository
            , MetaCatalogService metaCatalogService) {
        this.customerNotService = service1;
        this.customerRepository = customerRepository;
        this.sessionRepository = sessionRepository;
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.helpNumber = "0721982705";
        this.metaCatalogService = metaCatalogService;

    }


    public boolean isStoreOpen() {
        LocalTime now = LocalTime.now(ZoneId.of("Africa/Johannesburg"));

//        return !now.isBefore(OPENING_TIME) && !now.isAfter(CLOSING_TIME);
        return true;
    }


    public void publishCustomerMessage(CustomerMessage message) {

        if (!isStoreOpen()) {
            customerNotService.publishStoreClosedStatus(message.getCustomerIdentifier());
            return;
        }

        Customer customer = activeSessions.get(message.getCustomerIdentifier());

        if (customer == null) {
            customer = new Customer();
            customer.setCustomerIdentifierId(message.getCustomerIdentifier());
            activeSessions.put(message.getCustomerIdentifier(), customer);

            boolean sent = customerNotService.sendMainMenu(message.getCustomerIdentifier());

            if (sent) {
                CustomerSession session = customer.getSession();

                if (session == null) {
                    session = new CustomerSession();
                    customer.setSession(session);
                    session.setCustomer(customer);
                }
                session.setState(AWAITING_MAIN_MENU_INPUT);

            }

            return;
        }

        BotState state = customer.getSession().getState();

        if (state.name().equalsIgnoreCase(String.valueOf(GREETING))) {
            boolean sent = customerNotService.sendMainMenu(message.getCustomerIdentifier());

            if (sent) {
                CustomerSession session = customer.getSession();
                session.setState(AWAITING_MAIN_MENU_INPUT);
            }

            return;
        }

        if (state.name().equalsIgnoreCase(String.valueOf(AWAITING_MAIN_MENU_INPUT))) {
            boolean handled = customerNotService.handleMainManuInput(message);

            if (handled) {

            }

            return;
        }

        if (state.name().equalsIgnoreCase(String.valueOf(BotState.AWAITING_CUSTOMER_NAME))) {
            customer.setName((String) message.getCustomerMessage());
            boolean requested = customerNotService.requestMobileNumber(message);

            if (requested) {
                customer.getSession().setState(AWAITING_CUSTOMER_NUMBER);
            }
            return;
        }

        if (state.name().equalsIgnoreCase(String.valueOf(BotState.AWAITING_CUSTOMER_NUMBER))) {
            customer.setMobile((String) message.getCustomerMessage());
            boolean requested = customerNotService.requestLocation(message.getCustomerIdentifier());

            if (requested) {
                customer.getSession().setState(AWAITING_CUSTOMER_LOCATION);
            }
        }

        if (state.name().equalsIgnoreCase(String.valueOf(BotState.AWAITING_CUSTOMER_LOCATION))) {
            customer.setLocation((String) message.getCustomerMessage());

            placeOrder(customer.getCustomerIdentifierId());

            customer.getSession().setState(AWAITING_TERMINATION_INPUT);

            boolean sent = customerNotService.publishTerminationConfirmation(message.getCustomerIdentifier());
            return;
        }

        if (state.name().equalsIgnoreCase(String.valueOf(BotState.AWAITING_TERMINATION_INPUT))) {

            customer.getSession().setState(AWAITING_TERMINATION_INPUT);
        }

    }


    private ShipDayOrderRequest createDeliveryDto(CustomerOrder order) {

        Customer customer = order.getCustomer();
        String address = customer.getLocation();
        String orderNumber = "#Kasi2-6" + order.getId();
        String customerName = customer.getName();
        String customerPhoneNumber = customer.getMobile();
        String restaurantName = "Kasi grill";
        String restaurantAddress = address;
//        double totalOrderCost = Double.parseDouble(String.valueOf(order.getOrderItem().getMenuItem().getPrice()));
        String deliveryInstructions = "hhfjwekjjijiuehh";


        ShipDayOrderRequest request = new ShipDayOrderRequest();
        request.setCustomerAddress(address);
        request.setDeliveryInstructions(deliveryInstructions);
        request.setCustomerName(customerName);
        request.setOrderNumber(orderNumber);
        request.setRestaurantAddress(restaurantAddress);
        request.setCustomerPhoneNumber(customerPhoneNumber);
        request.setRestaurantName(restaurantName);
//        request.setTotalOrderCost(totalOrderCost);

        return request;
    }


    @Transactional(readOnly = true)
    public List<OrderDto> publishCustomerOrders(String customerIdentifier) {
        Customer customer = customerRepository.findByCustomerIdentifierId(customerIdentifier);

        if (customer == null) return new ArrayList<>();


        List<OrderDto> orders = new ArrayList<>();
        List<CustomerOrder> orders1 = customer.getOrders();
        for (CustomerOrder order : orders1) {
            orders.add(new OrderDto(order.getOrderNumber(), order.getStatus(), order.getCreatedDate()));
        }

        CustomerSession session = customer.getSession();
        session.setState(ON_ORDERS_REQUESTED);

        sessionRepository.save(session);
        customer.setSession(session);
        customerRepository.save(customer);
        return orders;
    }

    private int generateOrderNumber() {
        orderNumber++;
        return orderNumber;
    }

    public void addOderItem(String customerIdentifier, String sku, int quan) {
        MenuItem item = menuRepository.findBySku(sku);

        List<OrderItem> orderItems = activeSessionsOrderItems.computeIfAbsent(customerIdentifier, k -> new ArrayList<>());
        if (item != null) {

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItem(item);
            orderItem.setQuantity(quan);
            orderItems.add(orderItem);
        }
    }

    private void placeOrder(String customerIdentifier) {
        Customer customer = activeSessions.get(customerIdentifier);

        if (customer == null) return;

        List<OrderItem> orderItems = activeSessionsOrderItems.get(customerIdentifier);

        CustomerOrder order = new CustomerOrder();
        order.setStatus(String.valueOf(OrderStatus.PENDING_PAYMENT));
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        customer.getOrders().add(order);

        BigDecimal totalAmount = BigDecimal.valueOf(0);


        for (OrderItem orderItem : orderItems) {
            order.getOrderItems().add(orderItem);
            MenuItem item = orderItem.getMenuItem();

            totalAmount = item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        }

        order.setOrderAmount(totalAmount);
        activeSessionsOrders.put(customerIdentifier, order);

        CustomerSession session = customer.getSession();

        sessionRepository.save(session);
        orderRepository.save(order);
        customerRepository.save(customer);

    }

    @Transactional
    public void publishOrder(String customerIdentifier) {

        boolean requested = customerNotService.requestCustomerName(customerIdentifier);

        if (requested) {

            Customer customer = activeSessions.get(customerIdentifier);
            customer.getSession().setState(AWAITING_CUSTOMER_NAME);
        }

    }

    public boolean publishHelp(String customerIdentifier) {
        boolean sent = customerNotService.sendHelp(helpNumber, customerIdentifier);


        if (sent) {
            Customer customer = customerRepository.findByCustomerIdentifierId(customerIdentifier);

            if (customer != null) {
                CustomerSession session = customer.getSession();
                if (session != null) {
                    session.setState(ON_HELPLINE_REQUESTED);
                }
            }
        }
        return sent;
    }

    public void menuRequestedAgain(CustomerMessage message) {
        boolean sent = customerNotService.sendMainMenu(message.getCustomerIdentifier());

        if (sent) {
            Customer customer = customerRepository.findByCustomerIdentifierId(message.getCustomerIdentifier());

            if (customer != null) {
                CustomerSession session = customer.getSession();
                if (session != null) {
                    session.setState(AWAITING_MAIN_MENU_INPUT);
                }
            }
        }
    }

    public void addMenuItem(MenuItemRequest request) {
        MenuItem item = new MenuItem();
        item.setPrice(request.getPrice());
        item.setAvailable(request.isAvailable());
        item.setDisc(request.getDisc());
        item.setTitle(request.getTitle());
        item.setImgUrl(request.getImgUrl());
        item.setWebsiteLink(request.getWebsiteLink());

        String sku = generateSku(request.getTitle());
        item.setSku(sku);

        menuRepository.save(item);

        metaCatalogService.pushToWhatsAppCatalog(request, sku);
    }

    private String generateSku(String title) {
        String prefix = title.replaceAll("[^A-Za-z]", "").toUpperCase();
        prefix = prefix.length() >= 3 ? prefix.substring(0, 3) : (prefix + "XXX").substring(0, 3);
        return prefix + "-" + System.currentTimeMillis() % 100000;
    }

    public boolean terminateProcess(String customerIdentifier) {

        activeSessions.remove(customerIdentifier);
        activeSessionsOrderItems.remove(customerIdentifier);
        activeSessionsOrders.remove(customerIdentifier);

        Customer customer = activeSessions.get(customerIdentifier);
        List<OrderItem> orderItems = activeSessionsOrderItems.get(customerIdentifier);

        return customer == null && orderItems == null;
    }

    public void backToManu(String customerIdentifier) {

        Customer customer = activeSessions.get(customerIdentifier);
        if (customer != null) {
            CustomerSession session = customer.getSession();

            if (session != null) {
                session.setState(AWAITING_TERMINATION_INPUT);
            }
        }

    }

    public OrderDto getOrderDetails(String customerIdentifier) {
        CustomerOrder order = activeSessionsOrders.get(customerIdentifier);

        if(order!=null){
            return  new OrderDto(order.getOrderNumber(),order.getStatus(),order.getCreatedDate());
        }
        return null;
    }
}
