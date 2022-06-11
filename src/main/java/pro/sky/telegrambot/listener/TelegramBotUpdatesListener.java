package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.Notification;
import pro.sky.telegrambot.repository.NotificationRepository;

import javax.annotation.PostConstruct;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private static ResourceBundle MY_BUNDLE;

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;

    private final NotificationRepository notificationRepository;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationRepository notificationRepository) {
        this.telegramBot = telegramBot;
        this.notificationRepository = notificationRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            String languageCode = update.message().from().languageCode();
            String messageText = update.message().text();
            Long chatId = update.message().chat().id();
            Locale locale = new Locale(languageCode, languageCode);
            MY_BUNDLE = ResourceBundle.getBundle("Labels", locale);
            switch (messageText) {
                case "/start":
                    sendMessage(chatId, MY_BUNDLE.getString("START_TEXT"));
                    break;
                case "/help":
                    sendMessage(chatId, MY_BUNDLE.getString("HELP_TEXT"));
                    break;
                default:
                    parseNotification(chatId, messageText);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void parseNotification(Long chatId, String messageText) {
        String[] textMessageArr = messageText.split(" ", 3);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyykk:mm");
        try {
            LocalDateTime parsedDateTime = LocalDateTime.parse(textMessageArr[0] + textMessageArr[1], formatter);
            notificationRepository.save(new Notification(chatId, textMessageArr[2], parsedDateTime));
            sendMessage(chatId,MY_BUNDLE.getString("NOTIFICATION_SAVED_TEXT"));
        } catch (DateTimeException | ArrayIndexOutOfBoundsException e) {
            logger.warn("Can't parse date! {}", e.getMessage());
            sendMessage(chatId, MY_BUNDLE.getString("INVALID_TEXT"));
        }
    }

    private void sendMessage(Long chatId, String textMessage) {
        logger.info("Processing sendMessage: {}", textMessage);
        telegramBot.execute(new SendMessage(chatId, textMessage));
    }

    private void sendMessage(Long chatId, String textMessage, Keyboard keyboard) {
        logger.info("Processing sendMessage: {}", textMessage);
        telegramBot.execute(new SendMessage(chatId, textMessage).replyMarkup(keyboard));
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotification() {
        LocalDateTime dateTimeNow = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<Notification> notifications = notificationRepository.getNotificationByNotificationDate(dateTimeNow);
        if (!notifications.isEmpty()) {
            notifications.forEach(notification -> sendMessage(notification.getChat_id(),notification.getNotificationMessage()));
        }
    }
}
