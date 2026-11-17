package com.example.mywebsite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String userId;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String gmail;

    private String password;
    private String role; //Khởi tạo mặc định là "user" cho người đăng kí mới
    private String status = "ACTIVE";
    private String banReason;
    //Gen Getter và Setter cho 2 biến này
    private String otpCode;
    private LocalDateTime otpExpiryTime;

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


    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public java.time.LocalDateTime getOtpExpiryTime() {
        return otpExpiryTime;
    }

    public void setOtpExpiryTime(java.time.LocalDateTime otpExpiryTime) {
        this.otpExpiryTime = otpExpiryTime;
    }
    //Getter và Setter cho trường gmail
    public String getGmail(){
        return gmail;
    }

    public void setGmail(String gmail){
        this.gmail = gmail;
    }
}
