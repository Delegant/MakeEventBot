package pro.sky.telegrambot.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chat_id;

    private String notificationMessage;

    private LocalDateTime notificationDate;

    public Long getChat_id() {
        return chat_id;
    }

    public Notification() {
    }

    public Notification(Long chat_id, String notificationMessage, LocalDateTime notificationDate) {
        this.notificationMessage = notificationMessage;
        this.notificationDate = notificationDate;
        this.chat_id = chat_id;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

}

