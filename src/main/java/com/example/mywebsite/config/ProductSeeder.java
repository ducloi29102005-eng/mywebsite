package com.example.mywebsite.config;

import com.example.mywebsite.model.Product;
import com.example.mywebsite.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class ProductSeeder {

    @Bean
    CommandLineRunner initProducts(ProductRepository productRepository) {
        return args -> {
            // Kiểm tra nếu collection products chưa có dữ liệu thì mới thêm
            if (productRepository.count() == 0) {
                productRepository.saveAll(Arrays.asList(
                        new Product("Tượng Jinx Unlocked Statue", "statue", "Liên Minh Huyền Thoại", 1850000, "BÁN CHẠY", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop"),
                        new Product("Áo Hoodie Panthera Cyberpunk Ed.", "apparel", "Panthera Hub", 890000, "MỚI", "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=600&auto=format&fit=crop"),
                        new Product("Mô hình súng Vandal Prime 1:1", "replica", "Valorant", 2400000, "GIỚI HẠN", "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=600&auto=format&fit=crop"),
                        new Product("Bàn di chuột XL Panthera Stealth", "accessories", "Panthera Gear", 450000, "HOT", "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop"),
                        new Product("Nón Snapback Champion Edition", "apparel", "Esports", 380000, "", "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=600&auto=format&fit=crop"),
                        new Product("Đĩa than Original Soundtrack Vinyl", "art", "Riot Games Music", 1150000, "ĐẶC BIỆT", "https://images.unsplash.com/photo-1539185441755-769473a23570?w=600&auto=format&fit=crop"),
                        new Product("Tượng Yasuo Tinh Võ Sư (Special)", "statue", "Liên Minh Huyền Thoại", 2950000, "GIỚI HẠN", "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&auto=format&fit=crop"),
                        new Product("Móc chìa khóa Dao Butterfly VCT", "accessories", "Valorant", 220000, "MỚI", "https://images.unsplash.com/photo-1582139329536-e7284fece509?w=600&auto=format&fit=crop")
                ));
                System.out.println("========== ĐÃ THÊM 8 SẢN PHẨM MẪU VÀO MONGODB ==========");
            }
        };
    }
}