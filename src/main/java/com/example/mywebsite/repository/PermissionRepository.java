package com.example.mywebsite.repository;

import com.example.mywebsite.model.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {

    // Thêm dòng này để Spring Data tự động tạo query tìm kiếm theo Name
    Optional<Permission> findByName(String name);
}