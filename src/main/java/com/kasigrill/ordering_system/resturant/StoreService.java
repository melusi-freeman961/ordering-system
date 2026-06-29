package com.kasigrill.ordering_system.resturant;

import com.kasigrill.ordering_system.config.*;
import com.kasigrill.ordering_system.customer.*;
import com.kasigrill.ordering_system.menuitem.MenuItem;
import com.kasigrill.ordering_system.menuitem.MenuItemDto;
import com.kasigrill.ordering_system.menuitem.MenuItemStatus;
import com.kasigrill.ordering_system.menuitem.MenuRepository;
import com.kasigrill.ordering_system.order.*;
import com.kasigrill.ordering_system.telegram.VendorMessageData;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.kasigrill.ordering_system.customer.CustomerSessionState.*;

@Service
public class StoreService {
    private static final LocalTime OPENING_TIME = LocalTime.of(10, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(23, 50);

    private final CustomerNotificationService customerNotService;
    private final VendorNotificationService vendorNotService;
    private final CustomerRepository customerRepository;
    private final SessionRepository sessionRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AdminRepository adminRepository;

    public StoreService(CustomerNotificationService service1
            , VendorNotificationService service2
            , CustomerRepository customerRepository
            , SessionRepository sessionRepository
            , MenuRepository menuRepository
            , OrderRepository orderRepository
            , OrderItemRepository orderItemRepository
            , AdminRepository adminRepository) {
        this.customerNotService = service1;
        this.customerRepository = customerRepository;
        this.sessionRepository = sessionRepository;
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.vendorNotService = service2;
        this.adminRepository = adminRepository;
    }

    public List<MenuItemDto> getMenu() {
        List<MenuItem> availableMenuItems = menuRepository.findByAvailableTrue();
        List<MenuItemDto> menuItems = new ArrayList<>();

        for (MenuItem item : availableMenuItems) {
            menuItems.add(new MenuItemDto(item.getName(), String.valueOf(item.getId()), String.valueOf(item.getPrice())));
        }
        return menuItems;
    }

    public Customer registerUser(String channelId) {
        Customer customer = new Customer();
        customer.setStatus(CustomerStatus.ACTIVE.name());
        customer.setName(null);
        customer.setMobile(null);
        customer.setLocation(null);


        CustomerIdentifier identifier = createCustomerIdentifier(channelId);
        customer.setIdentifier(identifier);
        identifier.setCustomer(customer);
        CustomerSession session = createCustomerSession(customer);
        customer.setSession(session);

        return customerRepository.save(customer);

    }

    private CustomerIdentifier createCustomerIdentifier(String channelId) {
        CustomerIdentifier identifier = new CustomerIdentifier();
        identifier.setChannelId(channelId);
        return identifier;
    }


    private CustomerSession createCustomerSession(Customer customer) {

        CustomerSession session = new CustomerSession();
        session.setCustomer(customer);
        session.setState(AWAITING_MENU_SELECTION);

        return sessionRepository.save(session);
    }

    public void processVendorMessage(IncomingVendorMessage message) {

        CustomerOrder order = orderRepository.findByVendorMessageData_MessageId(message.getMessageId());
        if (order == null) return;

        String channelId = order.getCustomer().getIdentifier().getChannelId();

        String orderNumber = "#Kasi2-6" + order.getId();
        vendorNotService.publicOrderNumber(message, orderNumber);
        boolean sent = customerNotService.sendOrderStatus(message.getStatus(), channelId, orderNumber);

        if (sent) {
            order.setStatus(message.getStatus());
            orderRepository.save(order);
        }
    }

    public boolean isStoreOpen() {
        LocalTime now = LocalTime.now(ZoneId.of("Africa/Johannesburg"));

        return !now.isBefore(OPENING_TIME) && !now.isAfter(CLOSING_TIME);
    }


    @Transactional
    public void executeCustomerMessage(IncomingCustomerMessage message) {

        if (!isStoreOpen()) {
            customerNotService.publishStoreClosedStatus(message.getChannelId());
            return;
        }

        Customer customer = customerRepository.findByIdentifierChannelId(message.getChannelId());
        if (customer == null) {

            try {

                customer = registerUser(message.getChannelId());

                if (getMenu() != null) {
                    customerNotService.sendMenu(message, getMenu());
                }

                return;

            } catch (DataIntegrityViolationException | ConstraintViolationException e) {

                customer = customerRepository.findByIdentifierChannelId(message.getChannelId());
            }
        }


        if (customer.getStatus().equalsIgnoreCase(CustomerStatus.INACTIVE.name())) {

            customer.setStatus(CustomerStatus.RETURNING.name());

            if (message.getMessage() != null && message.getMessage().equalsIgnoreCase("1")) {

                List<MenuItemDto> items = new ArrayList<>();
                for (MenuItem menuItem : menuRepository.findByAvailableTrue()) {
                    items.add(new MenuItemDto(menuItem.getName(), String.valueOf(menuItem.getId()), String.valueOf(menuItem.getPrice())));
                }
                boolean sent = customerNotService.sendMenu(message, items);

                if (sent) {
                    customer.getSession().setState(AWAITING_MENU_SELECTION);
                }

            }

            return;
        }

        CustomerSession session = customer.getSession();
        CustomerSessionState state = session.getState();

        switch (state) {
            case AWAITING_MENU_SELECTION:

                updateOrder(message, customer);

                boolean isReturning = customer.getStatus().equalsIgnoreCase(CustomerStatus.RETURNING.name());

                if (!isReturning) {
                    boolean sent = customerNotService.getName(message.getChannelId());
                    if (sent) {
                        session.setState(AWAITING_CUSTOMER_NAME);
                        sessionRepository.save(session);
                        customer.setSession(session);
                    }


                } else {

                    boolean messageSent = customerNotService.getLocation(message.getChannelId());
                    if (messageSent) {
                        session.setState(AWAITING_CUSTOMER_LOCATION);
                        sessionRepository.save(session);
                        customer.setSession(session);
                    }

                }
                customerRepository.save(customer);
                break;

            case AWAITING_CUSTOMER_NAME:

                customer.setName(message.getMessage());

                boolean messageSent = customerNotService.getCustomerNumber(message.getChannelId());

                if (messageSent) {
                    session.setState(AWAITING_CUSTOMER_NUMBER);
                    sessionRepository.save(session);
                    customer.setSession(session);
                }

                customerRepository.save(customer);
                break;

            case AWAITING_CUSTOMER_NUMBER:

                customer.setMobile(message.getMessage());

                boolean sentMessage = customerNotService.getLocation(message.getChannelId());

                if (sentMessage) {
                    session.setState(AWAITING_CUSTOMER_LOCATION);
                    sessionRepository.save(session);
                    customer.setSession(session);
                }

                customerRepository.save(customer);

                break;
            case AWAITING_CUSTOMER_LOCATION:

                Map<String, Object> locationData = message.getLocation();
                Double lat = (Double) locationData.get("latitude");
                Double lng = (Double) locationData.get("longitude");

                String locationLink = generateGpsLink(lat, lng);
                customer.setLocation(locationLink);
                customer.setStatus(CustomerStatus.INACTIVE.name());
                customerRepository.save(customer);

                CustomerOrder order = null;

                for (CustomerOrder o : customer.getOrders()) {
                    if (o.getStatus().equalsIgnoreCase(OrderStatus.PENDING.name())) {
                        order = o;
                        break;
                    }
                }

                if (order == null) return;

                OrderDto orderDto = createOrderDto(order);
                VendorMessageData vendorMessageData = vendorNotService.sendOrder(orderDto);
                if (vendorMessageData == null) return;
                order.setVendorMessageData(vendorMessageData);

                order.setStatus(OrderStatus.PLACED.name());
                orderRepository.save(order);
                customerNotService.sendOrderStatus(OrderStatus.PLACED.name(), message.getChannelId(), orderDto.orderNumber());
                customerNotService.sendOrderConfirmation(message.getChannelId());

                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }


    }

    private OrderDto createOrderDto(CustomerOrder order) {

        Customer customer = order.getCustomer();
        String channelId = customer.getIdentifier().getChannelId();
        String mapLink = customer.getLocation();
        String orderItem = order.getOrderItem().getMenuItem().getName();
        String name = customer.getName();
        String orderNumber = "#Kasi2-6" + order.getId();
        return new OrderDto(name, orderNumber, orderItem, mapLink, channelId);
    }

    private void updateOrder(IncomingCustomerMessage message, Customer customer) {
        String id = message.getItemId();
        CustomerOrder order = createOrder(customer);
        Long menuItemId = Long.parseLong(id.replace("item_", ""));
        Optional<MenuItem> menuItem = menuRepository.findById(menuItemId);

        if (menuItem.isPresent()) {
            MenuItem item = menuItem.get();
            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItem(item);
            orderItem.setOrder(order);
            order.setOrderItem(orderItem);
            customer.getOrders().add(order);
            item.getOrderItems().add(orderItem);

            menuRepository.save(item);
            orderRepository.save(order);
            orderItemRepository.save(orderItem);
            customerRepository.save(customer);
        }
    }

    private String generateGpsLink(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        // Formats to: https://www.google.com/maps/search/?api=1&query=-25.746112,28.188056
        return "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
    }

    private CustomerOrder createOrder(Customer customer) {
        CustomerOrder order = new CustomerOrder();
        order.setStatus(OrderStatus.PENDING.name());
        order.setCustomer(customer);
        return orderRepository.save(order);
    }

    @Transactional
    public void processMenu(String message) {

        List<Admin> admins = adminRepository.findAll();

        Admin admin;
        if (admins.isEmpty()) {
            admin = new Admin();
            admin.setStatus(AdminStatus.AWAITING_ITEM_NAME.name());
            adminRepository.save(admin);
        } else {
            admin = admins.getFirst();
        }

        String status = admin.getStatus();

        if (status.equalsIgnoreCase(AdminStatus.AWAITING_ITEM_NAME.name())) {

            menuRepository.deleteAllByStatus(MenuItemStatus.IN_PROGRESS.name());

            MenuItem item = new MenuItem();
            item.setName(message);
            item.setAvailable(true);
            item.setStatus(MenuItemStatus.IN_PROGRESS.name());

            menuRepository.save(item);

            boolean sent = vendorNotService.requestItemPrice();

            if (sent) {
                admin.setStatus(AdminStatus.AWAITING_ITEM_PRICE.name());
                adminRepository.save(admin);

            }

        } else if (status.equalsIgnoreCase(AdminStatus.AWAITING_ITEM_PRICE.name())) {
            List<MenuItem> menuItems = menuRepository.findByStatus(MenuItemStatus.IN_PROGRESS.name());
            MenuItem item = menuItems.getFirst();
            BigDecimal price = BigDecimal.valueOf(Long.parseLong(message));
            item.setPrice(price);

            menuRepository.save(item);

            boolean sent = vendorNotService.sendItemAddedConfirmation();

            if (sent) {
                admin.setStatus(AdminStatus.COMPLETED.name());
                item.setStatus(MenuItemStatus.COMPLETED.name());
                adminRepository.save(admin);
                menuRepository.save(item);
            }

        }
//        else if(status.equalsIgnoreCase(AdminStatus.COMPLETED.name())){
//
//        }
    }
}
