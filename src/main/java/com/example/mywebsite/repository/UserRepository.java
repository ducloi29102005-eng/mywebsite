package com.example.mywebsite.repository;

import com.example.mywebsite.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    //Hỗ trợ tìm kiếm người dùng theo mã userId
    Optional<User> findByUserId(String userId);

}

