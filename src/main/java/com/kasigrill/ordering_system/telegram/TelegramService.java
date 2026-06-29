package com.kasigrill.ordering_system.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasigrill.ordering_system.config.VendorNotificationService;
import com.kasigrill.ordering_system.customer.IncomingVendorMessage;
import com.kasigrill.ordering_system.order.OrderDto;
import com.kasigrill.ordering_system.order.OrderStatus;
import com.kasigrill.ordering_system.resturant.StoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TelegramService implements VendorNotificationService {


    private final StoreService storeService;
    private final TelegramGateway telegramGateway;
    private final ObjectMapper objectMapper;
    @Value("${telegram.chat.id}")
    private String chatId;

    public TelegramService(TelegramGateway telegramGateway
            , @Lazy StoreService storeService) {
        this.telegramGateway = telegramGateway;
        this.objectMapper = new ObjectMapper();
        this.storeService = storeService;
    }


    @Override
    public VendorMessageData sendOrder(OrderDto order) {
        String name = order.customerName();
        String orderNumber = order.orderNumber();
        String orderItemName = order.orderItemName();
        String mapLink = order.mapLink();
        String channelId = order.customerChannelId();

        return telegramGateway.sendKitchenProductionTicket(name, orderNumber, orderItemName, mapLink, channelId);
    }

    @Override
    public void publicOrderNumber(IncomingVendorMessage message, String orderNumber) {

        TelegramMessageDto messageDto = (TelegramMessageDto) message;
        String status = messageDto.status();
        String messageId = messageDto.getMessageId();
        String callbackQueryId = messageDto.callbackId();

        handleButtonClick(status, orderNumber, messageId, callbackQueryId);
    }

//    @Override
//    public boolean requestItemName() {
//        return false;
//    }
//
//    @Override
//    public boolean requestItemPrice() {
//        return false;
//    }


    private InlineKeyboardMarkup createNewKeyBoardMarkup(String customerNumber) {
        List<List<InlineKeyboardButton>> updatedMatrix = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ READY");
        backBtn.setCallbackData("READY_" + customerNumber);

        row.add(backBtn);
        updatedMatrix.add(row);

        InlineKeyboardMarkup newMarkup = new InlineKeyboardMarkup();
        newMarkup.setKeyboard(updatedMatrix);
        return newMarkup;
    }

    public void deserializeAndProcessMessage(Map<String, Object> payload) {
        Update update = objectMapper.convertValue(payload, Update.class);
        if (update == null) return;

        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            String messageId = String.valueOf(update.getCallbackQuery().getMessage().getMessageId());
            String[] s = data.split("_");
            TelegramMessageDto telegramMessageDto = new TelegramMessageDto(s[0], messageId, update.getCallbackQuery().getId());
            storeService.processVendorMessage(telegramMessageDto);

            return;
        }



//        if (update.getMessage().getNewChatMembers() != null || update.getMessage().getLeftChatMember() != null) {
//            return;
//        }

        if (update.getMessage() != null && update.getMessage().hasText()) {
            String message = update.getMessage().getText();


            if (message.trim().equalsIgnoreCase("/start")) {
                return;
            }

            if (Objects.equals(String.valueOf(update.getMessage().getChatId()), chatId)) {
                if (message.trim().equalsIgnoreCase("add item")) {
                    requestItemName();

                } else {
                    storeService.processMenu(message);
                }
            }


        }
    }

    private void requestItemName() {
        telegramGateway.promptForItemName();
    }

    @Override
    public boolean requestItemPrice() {
        return telegramGateway.promptForItemPrice();
    }

    @Override
    public boolean sendItemAddedConfirmation() {
        return telegramGateway.informItemAdded();
    }

    public void handleButtonClick(String status, String orderNumber, String id, String callbackQueryId) {


        Integer messageId = Integer.valueOf(id);

        if (status.trim().equalsIgnoreCase(OrderStatus.READY.name())) {
            telegramGateway.orderCompleted(orderNumber, messageId);
        } else if (status.equalsIgnoreCase(OrderStatus.PREPARING.name())) {
            telegramGateway.editInlineKeyboard(messageId, createNewKeyBoardMarkup(status));
        } else if (status.equalsIgnoreCase(OrderStatus.REJECT.name())) {
            telegramGateway.rejectOrderMessage(messageId, orderNumber);

        }

        telegramGateway.answerCallbackQuery(callbackQueryId, "Action processed!");

    }
}
