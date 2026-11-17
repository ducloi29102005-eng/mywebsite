package com.example.mywebsite.repository;

import com.example.mywebsite.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    // Tìm sản phẩm theo danh mục và hỗ trợ sắp xếp
    Page<Product> findByCategory(String category, Pageable pageable);
}