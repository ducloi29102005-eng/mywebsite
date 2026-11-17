package com.example.mywebsite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private String category;
    private String game;
    private double price;
    private String tag;
    private String image;
    private int stockQuantity;

    // THÊM TRƯỜNG NÀY: Lưu danh sách kích cỡ (VD: ["Freesize"] hoặc ["S", "M", "L", "XL"])
    private List<String> sizes = new ArrayList<>();

    public Product() {}

    public Product(String name, String category, String game, double price, String tag, String image) {
        this.name = name;
        this.category = category;
        this.game = game;
        this.price = price;
        this.tag = tag;
        this.image = image;
        this.sizes = List.of("Freesize");
    }
    // Getter & Setter cho sizes
    public List<String> getSizes() {
        if (sizes == null || sizes.isEmpty()) {
            return List.of("Freesize");
        }
        return sizes;
    }

    public void setSizes(List<String> sizes) {
        this.sizes = sizes;
    }
    public int getStockQuantity() {
        return stockQuantity;
    }

    // Getters và Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}