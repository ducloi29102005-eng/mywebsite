package com.example.mywebsite.repository;

import com.example.mywebsite.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    // Lấy danh sách đơn hàng của User sắp xếp từ mới nhất đến cũ nhất
    List<Order> findByUserIdOrderByOrderDateDesc(String userId);
}