package com.example.mywebsite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String userId;
    private String username;
    private String password;
    private String role; //Khởi tạo mặc định là "user" cho người đăng kí mới
    private String status = "ACTIVE";
    private String banReason;

    // Getter và Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() {return role;}
    public void setRole(String role){ this.role =role; }

    public String getStatus(){ return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String lockReason) {
        this.banReason = lockReason;
    }
}
