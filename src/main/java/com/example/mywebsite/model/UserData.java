package com.example.mywebsite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_data")
public class UserData {
    @Id
    private String id;
    private String userId; // Khóa liên kết

    private String gmail; // Gmail liên kết

    // Khởi tạo Getter / Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getGmail() { return gmail; }
    public void setGmail(String gmail) { this.gmail = gmail; }
}