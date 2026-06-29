package com.kasigrill.ordering_system.telegram;

import com.kasigrill.ordering_system.order.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramGateway {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.group.id}")
    private String groupId;

    @Value("${telegram.chat.id}")
    private String chatId;

    private static InlineKeyboardMarkup getInlineKeyboardMarkup(String contactNumber) {
        InlineKeyboardButton btnReject = new InlineKeyboardButton();
        btnReject.setText(OrderStatus.REJECT.name());
        btnReject.setCallbackData(OrderStatus.REJECT.name() + "_" + contactNumber);

        InlineKeyboardButton btnPreparing = new InlineKeyboardButton();
        btnPreparing.setText(OrderStatus.PREPARING.name());
        btnPreparing.setCallbackData(OrderStatus.PREPARING.name() + "_" + contactNumber);

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(btnReject);
        row.add(btnPreparing);

        List<List<InlineKeyboardButton>> keyboardMatrix = new ArrayList<>();
        keyboardMatrix.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboardMatrix);
        return markup;
    }

    public String compileCompleteMessageText(String orderNumber) {
        return "📦 **Order " + orderNumber + " is Complete!**\n\n" +
                "Thank you for your service. Since this order is finalized, " +
                "you can safely delete and clear this chat history to keep your inbox organized.";
    }

    public String compileCanceledOrderMessageText(String orderNumber) {
        return "📦 **Order " + orderNumber + " has been canceled!**\n\n" +
                "Thank you for your service " +
                "you can safely delete and clear this chat history to keep your inbox organized.";
    }

    public VendorMessageData sendKitchenProductionTicket(String customerName, String orderNumber, String item, String mapLink, String contactNumber) {
        // Construct the official Telegram API URL endpoint
        String telegramUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";


        SendMessage message = new SendMessage();
        message.setChatId(groupId);
        String markdownTicket = "📢 *NEW PAID ORDER READY TO COOK!* \n"
                + "🔢 *Order ID:* " + orderNumber + "\n"
                + "───────────────────\n"
                + "👤 *Customer:* " + customerName + "\n"
                + "🍟 *Order Item:* " + item + "\n"
                + "───────────────────\n"
                + "🛵 *Motorbike Navigation Route:* \n" + mapLink;
        message.setText(markdownTicket);

        InlineKeyboardMarkup markup = getInlineKeyboardMarkup(contactNumber);
        message.setReplyMarkup(markup);
        message.setParseMode("Markdown");

        try {
            TelegramWebhookRegistrar.TelegramResponse response = restTemplate.postForObject(telegramUrl, message, TelegramWebhookRegistrar.TelegramResponse.class);

            if (response != null && response.ok()) {
                Message telegramMessage = response.result();

                Integer messageId = telegramMessage.getMessageId();

                return new VendorMessageData(String.valueOf(messageId));
            }
        } catch (Exception e) {

            System.err.println("CRITICAL ERROR: Telegram failed to dispatch ticket: " + e.getMessage());
        }
        return null;
    }

    public void editInlineKeyboard(Integer messageId, InlineKeyboardMarkup newMarkup) {
        // 1. Note the different endpoint: /editMessageReplyMarkup
        String url = "https://api.telegram.org/bot" + botToken + "/editMessageReplyMarkup";

        // 2. Build the Edit payload
        EditMessageReplyMarkup editPayload = new EditMessageReplyMarkup();
        editPayload.setChatId(groupId);
        editPayload.setMessageId(messageId); // Tell Telegram WHICH message to update
        editPayload.setReplyMarkup(newMarkup); // Pass the new layout

        // 3. Send the POST request
        try {
            restTemplate.postForObject(url, editPayload, String.class);
        } catch (Exception e) {
            System.err.println("Error editing keyboard: " + e.getMessage());
        }
    }

    public void editTelegramMessage(String messageText, Integer messageId, InlineKeyboardMarkup markup) {

        String url = "https://api.telegram.org/bot" + botToken + "/editMessageText";


        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setMessageId(messageId);
        editMessageText.setChatId(groupId);
        editMessageText.setParseMode("Markdown");
        editMessageText.setText(messageText);

        try {
            restTemplate.postForObject(url, editMessageText, String.class);
        } catch (Exception e) {
            System.err.println("Error editing message text: " + e.getMessage());
        }
    }


    public void answerCallbackQuery(String callbackQueryId, String toastMessage) {
        String url = "https://api.telegram.org/bot" + botToken + "/answerCallbackQuery";
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        answer.setText(toastMessage);

        restTemplate.postForObject(url, answer, String.class);
    }

    public void orderCompleted(String orderNumber, Integer messageId) {

        editTelegramMessage(compileCompleteMessageText(orderNumber), messageId, null);
    }

    public void rejectOrderMessage(Integer messageId, String orderNumber) {
        editTelegramMessage(compileCanceledOrderMessageText(orderNumber), messageId, null);
    }


    public boolean promptForItem(String text) {

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        SendMessage message=SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try{
            TelegramWebhookRegistrar.TelegramResponse response = restTemplate.postForObject(url, message, TelegramWebhookRegistrar.TelegramResponse.class);

            if (response != null && response.ok()) {
               return true;
            }

        }
        catch (Exception e) {
            System.err.println("Error sending chat message: " + e.getMessage());
        }
        return false;
    }

    public void promptForItemName() {
        promptForItem("Awesome! Let's add a new item. What's the name of the dish?");
    }
    public boolean promptForItemPrice() {
       return promptForItem("Got it! How much does i cost? (e.g 65 or 150)");
    }

    public boolean informItemAdded() {
       return promptForItem("Success! Your item has been added.");
    }
}
