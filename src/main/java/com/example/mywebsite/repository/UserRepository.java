package com.example.mywebsite.repository;

import com.example.mywebsite.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserId(String userId);

    Optional<User> findByUsername(String username);
    //Hỗ trợ tìm kiếm người dùng theo mã userId
    // Tìm kiếm người dùng theo địa chỉ Gmail đã liên kết
    Optional<User> findByGmail(String gmail);

    // Kiểm tra xem Gmail này đã được tài khoản nào dùng chưa (tránh trùng lặp)
    boolean existsByGmail(String gmail);
}



