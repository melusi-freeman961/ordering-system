package com.kasigrill.ordering_system.resturant;

import com.kasigrill.ordering_system.config.CustomerNotificationService;
import com.kasigrill.ordering_system.customer.*;
import com.kasigrill.ordering_system.menuitem.*;
import com.kasigrill.ordering_system.order.*;
import com.kasigrill.ordering_system.ors.OpenRouteServiceClient;
import com.kasigrill.ordering_system.ors.RouteResponse;
import com.kasigrill.ordering_system.whatsapp.MetaCatalogService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.kasigrill.ordering_system.customer.BotState.*;
import static com.kasigrill.ordering_system.customer.CustomerStatus.NEW;
import static com.kasigrill.ordering_system.customer.CustomerStatus.RETURNING;
import static com.kasigrill.ordering_system.order.OrderStatus.*;

@Service
public class StoreService {

    private static final LocalTime OPENING_TIME = LocalTime.of(10, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(23, 50);
    private static int orderNumber;
    private final BigDecimal serviceFee=BigDecimal.valueOf(5);
    private final BigDecimal deliveryFlatFee=BigDecimal.valueOf(25);
    private final BigDecimal amountPerKm=BigDecimal.valueOf(10);
    private final CustomerNotificationService customerNotService;
    private final CustomerRepository customerRepository;
    private final SessionRepository sessionRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final Map<String, Customer> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, List<OrderItem>> activeSessionsOrderItems = new ConcurrentHashMap<>();
    private final Map<String, CustomerOrder> activeSessionsOrders = new ConcurrentHashMap<>();

    private final Map<String, RouteResponse> activeSessionRouteResponse = new ConcurrentHashMap<>();

    private final ApplicationEventPublisher eventPublisher;
    String helpNumber;
    private MetaCatalogService metaCatalogService;
    private OpenRouteServiceClient orsClient;

    private Double stLatitude;
    private Double stLongitude;

    public StoreService(CustomerNotificationService service1
            , CustomerRepository customerRepository
            , SessionRepository sessionRepository
            , MenuRepository menuRepository
            , OrderRepository orderRepository
            , MetaCatalogService metaCatalogService
            , ApplicationEventPublisher eventPublisher
            , OpenRouteServiceClient orsClient) {
        this.customerNotService = service1;
        this.customerRepository = customerRepository;
        this.sessionRepository = sessionRepository;
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.helpNumber = "0721982705";
        this.metaCatalogService = metaCatalogService;
        this.orsClient = orsClient;
        this.eventPublisher = eventPublisher;
        this.stLongitude=25.6215;
        this.stLatitude=-33.9604;

    }


    public boolean isStoreOpen() {
        LocalTime now = LocalTime.now(ZoneId.of("Africa/Johannesburg"));

        return !now.isBefore(OPENING_TIME) && !now.isAfter(CLOSING_TIME);

    }


    public void publishCustomerMessage(CustomerMessage message) {

        if (!isStoreOpen()) {
            customerNotService.publishStoreClosedStatus(message.getCustomerIdentifier());
            return;
        }

        //get customer if they have an active session
        Customer customer = activeSessions.get(message.getCustomerIdentifier());

        CustomerSession session=null;
        //customers don't have active session
        if (customer == null) {

            customer = customerRepository.findByCustomerIdentifierId(message.getCustomerIdentifier());

            //Customer is new
            if (customer == null) {

                customer = new Customer();
                customer.setStatus(NEW);
                customer.setCustomerIdentifierId(message.getCustomerIdentifier());


            } else {

                //Returning customer
                customer.setStatus(RETURNING);
                session=customer.getSession();
            }

            //assign active session to customer
            activeSessions.put(message.getCustomerIdentifier(), customer);

            //after allocating an active session we send them the menu
            boolean sent = customerNotService.sendMainMenu(message.getCustomerIdentifier());


            // if we successfully sent the menu we change the bot state
            if (sent) {

                if(session==null){
                    session = new CustomerSession();
                }
                session.setState(AWAITING_MAIN_MENU_INPUT);

                customer.setSession(session);
                session.setCustomer(customer);


            }

            return;
        }


        //customer has an active session
        BotState state = customer.getSession().getState();

        if (state.name().equalsIgnoreCase(String.valueOf(AWAITING_MAIN_MENU_INPUT))) {
            boolean handled = customerNotService.handleMainManuInput(message);
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

            Map<String, Object> locationData = (Map<String, Object>) message.getCustomerMessage();

            Double csLatitude = (Double) locationData.get("latitude");
            Double csLongitude = (Double) locationData.get("longitude");

            RouteResponse routeResponse = calculateRoute(stLongitude, stLatitude, csLongitude, csLatitude);


            activeSessionRouteResponse.put(message.getCustomerIdentifier(), routeResponse);

            String driverMapsLink = "https://www.google.com/maps/search/?api=1&query=" + csLatitude + "," + csLongitude;

            customer.setLocation(driverMapsLink);
            customer.getSession().setState(AWAITING_TERMINATION_INPUT);

            placeOrder(customer.getCustomerIdentifierId());


            boolean sent = customerNotService.publishTerminationConfirmation(message.getCustomerIdentifier());
            return;
        }

        if (state.name().equalsIgnoreCase(String.valueOf(BotState.AWAITING_TERMINATION_INPUT))) {

            customer.getSession().setState(AWAITING_TERMINATION_INPUT);
        }

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

    @Transactional
    public void addOderItem(String customerIdentifier, String sku, int quan) {

        //get the manu item from db using sku
        MenuItem item = menuRepository.findBySku(sku);


        Customer customer = customerRepository.findByCustomerIdentifierId(customerIdentifier);

        //get the active customer session
        if (customer == null) {
            customer = activeSessions.get(customerIdentifier);
        }


        //get all the order items corresponding to this specific active customer session,else create a bucket to store them
        List<OrderItem> orderItems = activeSessionsOrderItems.computeIfAbsent(customerIdentifier, k -> new ArrayList<>());

        //item is found in the db
        if (item != null) {

            //get the active order for this customer session
            CustomerOrder order = activeSessionsOrders.get(customerIdentifier);

            //no order bucket exists
            if (order == null) {

                //create order
                order = new CustomerOrder();
                order.setStatus(String.valueOf(OrderStatus.PENDING_PAYMENT));
                order.setOrderNumber(generateOrderNumber());
                order.setCustomer(customer);
                customer.getOrders().add(order);

                //create order item
                OrderItem orderItem = new OrderItem();
                orderItem.setMenuItem(item);
                orderItem.setQuantity(quan);
                orderItems.add(orderItem);
                orderItem.setOrder(order);

                order.getOrderItems().add(orderItem);

                BigDecimal totalAmount = item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));

                order.setOrderAmount(totalAmount);

                activeSessionsOrders.put(customerIdentifier, order);
                orderItems.add(orderItem);
            } else {

                OrderItem orderItem = new OrderItem();
                orderItem.setMenuItem(item);
                orderItem.setQuantity(quan);
                orderItems.add(orderItem);
                orderItem.setOrder(order);

                order.getOrderItems().add(orderItem);

                BigDecimal totalAmount = order.getOrderAmount().add(item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));

                order.setOrderAmount(totalAmount);

                orderItems.add(orderItem);

            }
        }
    }

    @Transactional
    private void placeOrder(String customerIdentifier) {
        Customer customer = activeSessions.get(customerIdentifier);

        if (customer == null) return;

        CustomerOrder order = activeSessionsOrders.get(customerIdentifier);

        customer = customerRepository.save(customer);

        eventPublisher.publishEvent(new CustomerCreatedEvent(order));
    }

    @Transactional
    public void publishOrder(String customerIdentifier) {

        Customer customer = activeSessions.get(customerIdentifier);

        CustomerStatus status = customer.getStatus();

        if (status != null) {
            if (status.equals(NEW)) {
                boolean requested = customerNotService.requestCustomerName(customerIdentifier);

                if (requested) {
                    customer.getSession().setState(AWAITING_CUSTOMER_NAME);
                }

            } else if (status.equals(RETURNING)) {

                boolean requested = customerNotService.requestLocation(customerIdentifier);

                if (requested) {
                    customer.getSession().setState(AWAITING_CUSTOMER_LOCATION);
                }

            }
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


        String customerIdentifier = message.getCustomerIdentifier();
        Customer customer = activeSessions.get(customerIdentifier);

        CustomerOrder order = activeSessionsOrders.get(message.getCustomerIdentifier());

        if (order != null) {
            order.setOrderAmount(BigDecimal.valueOf(0));
        }


        //customer doesn't have an active session
        if (customer == null) {

            customer = customerRepository.findByCustomerIdentifierId(message.getCustomerIdentifier());

            //a new customer
            if (customer == null) {
                publishCustomerMessage(message);
            } else {


                //it's a returning customer
                boolean sent = customerNotService.sendMainMenu(message.getCustomerIdentifier());

                if (sent) {

                    CustomerSession session = customer.getSession();
                    customer.setStatus(RETURNING);

                    if (session != null) {
                        session.setState(AWAITING_MAIN_MENU_INPUT);
                    }

                    activeSessions.put(customerIdentifier, customer);
                }
            }
        } else {

            //an active customer requesting menu again

            boolean sent = customerNotService.sendMainMenu(message.getCustomerIdentifier());
            CustomerSession session = customer.getSession();

            if (sent) {
                if (session != null) {
                    session.setState(AWAITING_MAIN_MENU_INPUT);
                }
            }

        }

    }

    public MenuItemResponse addMenuItem(MenuItemRequest request) {
        MenuItem item = new MenuItem();
        item.setPrice(request.getPrice());
        item.setAvailable(request.isAvailable());
        item.setDisc(request.getDisc());
        item.setTitle(request.getTitle());
        item.setImgUrl(request.getImgUrl());
        item.setWebsiteLink(request.getWebsiteLink());

        String sku = generateSku(request.getTitle());

        if (!sku.isBlank()) {
            item.setSku(sku);


            menuRepository.save(item);

            eventPublisher.publishEvent(new MenuItemCreatedEvent(request, sku));


        }

        return new MenuItemResponse("", "", false);
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

        activeSessionsOrders.remove(customerIdentifier);
        activeSessionsOrderItems.remove(customerIdentifier);

        if (customer != null) {

            customer.setStatus(RETURNING);
            CustomerSession session = customer.getSession();

            if (session != null) {
                CustomerMessage message = new CustomerMessage() {
                    @Override
                    public String getCustomerIdentifier() {
                        return customerIdentifier;
                    }

                    @Override
                    public Object getCustomerMessage() {
                        return null;
                    }
                };
                menuRequestedAgain(message);
            }
        }

    }

    public OrderDto getOrderDetails(String customerIdentifier) {
        CustomerOrder order = activeSessionsOrders.get(customerIdentifier);

        if (order != null) {
            return new OrderDto(order.getOrderNumber(), order.getStatus(), order.getCreatedDate());
        }
        return null;
    }

    public List<OrderResponse> getAllOrders() {
        List<CustomerOrder> orders = orderRepository.findAll();


        List<OrderResponse> orderResponses = new ArrayList<>();

        for (CustomerOrder order : orders) {
            List<OrderItem> orderItems = order.getOrderItems();

            List<OrderItemResponse> itemResponses = new ArrayList<>();
            for (OrderItem item : orderItems) {
                itemResponses.add(new OrderItemResponse(item.getMenuItem().getTitle(), item.getQuantity()));
            }

            orderResponses.add(new OrderResponse(order.getId(), String.valueOf(order.getOrderNumber()), order.getCustomer().getName(), order.getCustomer().getMobile(), order.getOrderAmount(), order.getStatus(), itemResponses));
        }

        return orderResponses;
    }


    @Transactional
    public OrderResponse updateStatus(Long id, String status) {
        Optional<CustomerOrder> order = orderRepository.findById(id);

        if (order.isPresent()) {
            CustomerOrder updatedOrder = order.get();
            updatedOrder.setStatus(status);
            CustomerOrder savedOrder = orderRepository.save(updatedOrder);

            eventPublisher.publishEvent(new OrderStatusUpdatedEvent(savedOrder, status));
            List<OrderItemResponse> orderItemResponses = new ArrayList<>();
            for (OrderItem item : savedOrder.getOrderItems()) {
                orderItemResponses.add(new OrderItemResponse(item.getMenuItem().getTitle(), item.getQuantity()));

            }

            return new OrderResponse(id, String.valueOf(updatedOrder.getOrderNumber())
                    , updatedOrder.getCustomer().getName()
                    , updatedOrder.getCustomer().getMobile()
                    , savedOrder.getOrderAmount()
                    , updatedOrder.getStatus()
                    , orderItemResponses);

        }
        return null;
    }


    public void publishStatusUpdate(CustomerOrder order, String status) {

        Customer customer = order.getCustomer();


        if (status.equalsIgnoreCase(ACCEPTED.name())) {
            String message = "✅ Your order-#" + order.getOrderNumber() + " was accepted";

            customerNotService.publishStatusUpdateToUser(customer.getCustomerIdentifierId(), message);


        } else if (status.equalsIgnoreCase(PREPARING.name())) {

            String message = "\uD83D\uDC68\u200D\uD83C\uDF73 Your order-#" + order.getOrderNumber() + " is being prepared.";
            customerNotService.publishStatusUpdateToUser(customer.getCustomerIdentifierId(), message);

        } else if (status.equalsIgnoreCase(READY.name())) {

            String message = "\uD83D\uDCE6 Your order-#" + order.getOrderNumber() + " is ready for pickup, searching for a driver...";
            customerNotService.publishStatusUpdateToUser(customer.getCustomerIdentifierId(), message);

        }
    }

    @Async
    private RouteResponse calculateRoute(
            double restaurantLongitude,
            double restaurantLatitude,
            double customerLongitude,
            double customerLatitude
    ) {
        return orsClient.calculateRoute(
                restaurantLongitude,
                restaurantLatitude,
                customerLongitude,
                customerLatitude
        );
    }

    private BigDecimal calculateDeliveryFee(double km){

        BigDecimal amount=amountPerKm.multiply(BigDecimal.valueOf(km));
        return deliveryFlatFee.add(amount);
    }
}
