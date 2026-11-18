package com.example.mywebsite.service;

import com.example.mywebsite.model.*;
import com.example.mywebsite.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShopService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    // 1. Lấy User ID từ Principal (Hỗ trợ cả form thường và Google OAuth2)
    public String getUserId(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken) {
            String email = (String) ((OAuth2User) ((OAuth2AuthenticationToken) principal).getPrincipal()).getAttribute("email");
            return userRepository.findByGmail(email).map(User::getUserId).orElse(null);
        } else {
            return userRepository.findByUsername(principal.getName()).map(User::getUserId).orElse(null);
        }
    }

    // 2. Nạp thông tin chung của User & Số lượng giỏ hàng lên Header
    public void populateUserHeaderData(String userId, Model model) {
        if (userId != null) {
            userRepository.findByUserId(userId).ifPresent(user -> model.addAttribute("currentUser", user));
            userProfileRepository.findByUserId(userId).ifPresent(profile -> model.addAttribute("userProfile", profile));

            int cartCount = cartRepository.findByUserId(userId)
                    .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                    .orElse(0);
            model.addAttribute("cartCount", cartCount);
        }
    }

    // 3. Lấy danh sách sản phẩm phân trang & sắp xếp
    public Page<Product> getProductsPage(int page, int size, String sort, String category) {
        Sort mongoSort = Sort.by(Sort.Direction.DESC, "id");
        if ("priceAsc".equals(sort)) {
            mongoSort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("priceDesc".equals(sort)) {
            mongoSort = Sort.by(Sort.Direction.DESC, "price");
        }

        int currentPage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(currentPage - 1, size, mongoSort);

        if (category == null || "all".equalsIgnoreCase(category)) {
            return productRepository.findAll(pageable);
        } else {
            return productRepository.findByCategory(category, pageable);
        }
    }

    // 4. Lấy chi tiết 1 sản phẩm
    public Product getProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    // 5. Thêm sản phẩm vào giỏ hàng
    public void addProductToCart(String userId, String productId, String size, int quantity) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            Cart cart = cartRepository.findByUserId(userId).orElse(new Cart(userId));
            boolean found = false;

            for (CartItem item : cart.getItems()) {
                if (item.getProductId().equals(productId) && item.getSize().equals(size)) {
                    item.setQuantity(item.getQuantity() + quantity);
                    found = true;
                    break;
                }
            }
            if (!found) {
                cart.getItems().add(new CartItem(productId, product.getName(), product.getImage(), product.getPrice(), quantity, size));
            }
            cartRepository.save(cart);
        }
    }

    // 6. Lấy giỏ hàng của User
    public Cart getCartByUserId(String userId) {
        return cartRepository.findByUserId(userId).orElse(new Cart(userId));
    }

    // 7. Tính tổng tiền giỏ hàng
    public double calculateCartTotal(Cart cart) {
        return cart.getItems().stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
    }

    // 8. Xóa 1 sản phẩm khỏi giỏ hàng
    public void removeProductFromCart(String userId, String productId, String size) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().removeIf(item -> item.getProductId().equals(productId) && item.getSize().equals(size));
            cartRepository.save(cart);
        });
    }

    // 9. Xử lý thanh toán và tạo đơn hàng (CẬP NHẬT)
    public Order processCheckout(String userId, String receiverName, String phone, String address, String paymentMethod) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return null;
        }

        double total = calculateCartTotal(cart); // Tính năng cũ: Tính tổng tiền

        // Tạo đơn hàng với thông tin người nhận và trạng thái mặc định là "PENDING"
        Order newOrder = new Order(
                userId,
                receiverName,
                phone,
                address,
                paymentMethod,
                new java.util.ArrayList<>(cart.getItems()),
                total,
                java.time.LocalDateTime.now(),
                "PENDING"
        );
        Order savedOrder = orderRepository.save(newOrder);

        // Tính năng cũ: Xóa sạch giỏ hàng sau khi đặt thành công
        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }
    // 10. Lấy thông tin đơn hàng (Có kiểm tra quyền sở hữu)
    public Order getOrderForUser(String orderId, String userId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return order;
    }

    // 11. Lấy lịch sử đơn hàng của User
    public List<Order> getUserOrderHistory(String userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    // 12. Tạo dữ liệu test sản phẩm
    public void generateTestProducts(int countToAdd) {
        for (int i = 1; i <= countToAdd; i++) {
            long count = productRepository.count() + 1;
            Product p = new Product(
                    "Sản phẩm thử nghiệm #" + count,
                    "apparel",
                    "Panthera Series",
                    500000 + (count * 10000),
                    "MỚI",
                    "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=600&auto=format&fit=crop"
            );
            productRepository.save(p);
        }
    }
        // 13. Khách hàng tương tác với đơn hàng (Hủy đơn hoặc Xác nhận nhận hàng)
        public void handleUserOrderAction(String userId, String orderId, String action) {
            Order order = orderRepository.findById(orderId).orElse(null);

            // Kiểm tra đơn hàng có tồn tại và thuộc về đúng User này không
            if (order != null && order.getUserId().equals(userId)) {
                // Khách chỉ được HỦY khi đơn đang ở trạng thái PENDING
                if ("CANCEL".equals(action) && "PENDING".equals(order.getStatus())) {
                    order.setStatus("CANCELLED");
                    orderRepository.save(order);
                }
                // Khách xác nhận ĐÃ NHẬN HÀNG khi đơn đang SHIPPING
                else if ("COMPLETE".equals(action) && "SHIPPING".equals(order.getStatus())) {
                    order.setStatus("COMPLETED");
                    orderRepository.save(order);
                }
            }
        }
    }
