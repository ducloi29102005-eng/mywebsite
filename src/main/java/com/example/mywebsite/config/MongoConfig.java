package com.example.mywebsite.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        // Tên database bạn muốn trỏ tới
        return "intern-db";
    }

    @Override
    public MongoClient mongoClient() {
        // Khởi tạo kết nối trực tiếp bằng chuỗi URI
        String uri = "mongodb://devintern:Y0BnEziM9vhQNfiga8LLDSQDu0Xi2cW@47.130.4.64:27017/intern-db?authSource=intern-db&directConnection=true";
        return MongoClients.create(uri);
    }
}
