package com.example.mywebsite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_binding_apps")
public class GameLinked {
    @Id
    private String id;
    private String userId; // Khóa liên kết
    private List<AppDetail> app;

    private List<String> linkedGames = new ArrayList<>(); // Danh sách 5 game

    // Khởi tạo Getter / Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<AppDetail> getApp() { return app; }
    public void setApp(List<AppDetail> app) { this.app = app; }
    public List<String> getLinkedGames() { return linkedGames; }
    public void setLinkedGames(List<String> linkedGames) { this.linkedGames = linkedGames; }

    // --- CÁC CLASS CON ĐỂ MAP VỚI JSON ---

    public static class AppDetail {
        private String appId;
        private AccountInfo gmail;
        private AccountInfo apple;
        private AccountInfo facebook;
        // Getters và Setters
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }

        public AccountInfo getGmail() { return gmail; }
        public void setGmail(AccountInfo gmail) { this.gmail = gmail; }

        public AccountInfo getApple() { return apple; }
        public void setApple(AccountInfo apple) { this.apple = apple; }

        public AccountInfo getFacebook() { return facebook; }
        public void setFacebook(AccountInfo facebook) { this.facebook = facebook; }
    }

    public static class AccountInfo {
        private String id;
        private String uid;
        private String createTime;

        // Getters và Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }

        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
    }
}


