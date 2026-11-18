package com.example.mywebsite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "orders")
public class Order {

    @Id
    private String id;
    private String userId;

    // Thông tin người nhận hàng (Mới thêm)
    private String receiverName;
    private String phone;
    private String address;
    private String paymentMethod; // VD: "COD" (Thanh toán khi nhận hàng), "VNPAY", ...

    private List<CartItem> items;
    private double totalAmount;
    private LocalDateTime orderDate;

    // Trạng thái đơn hàng: PENDING, PACKED, SHIPPING, COMPLETED, CANCELLED
    private String status;

    // Constructors
    public Order() {
    }

    // Cập nhật Constructor đầy đủ
    public Order(String userId, String receiverName, String phone, String address,
                 String paymentMethod, List<CartItem> items, double totalAmount,
                 LocalDateTime orderDate, String status) {
        this.userId = userId;
        this.receiverName = receiverName;
        this.phone = phone;
        this.address = address;
        this.paymentMethod = paymentMethod;
        this.items = items;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.status = status;
    }

    // Getters và Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}