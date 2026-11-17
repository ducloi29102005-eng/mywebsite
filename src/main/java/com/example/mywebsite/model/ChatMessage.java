package com.example.mywebsite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;

    private String roomId;       // Dùng chính userId của khách hàng làm mã phòng chat
    private String senderId;     // ID người gửi
    private String senderName;   // Tên hiển thị
    private String senderRole;   // "USER" hoặc "ADMIN" / "SHOP_MANAGER"
    private String content;      // Nội dung tin nhắn
    private LocalDateTime timestamp;
    private boolean isRead;      // Trạng thái đã đọc (để báo tin nhắn mới cho Admin)

    public ChatMessage() {}

    public ChatMessage(String roomId, String senderId, String senderName, String senderRole, String content) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }

    // Bạn tự generate đầy đủ Getter và Setter nhé
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}