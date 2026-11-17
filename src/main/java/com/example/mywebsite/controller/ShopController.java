package com.example.mywebsite.controller;

import com.example.mywebsite.model.Product;
import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.ProductRepository;
import com.example.mywebsite.repository.UserProfileRepository;
import com.example.mywebsite.repository.UserRepository;

// 5 DÒNG CẦN THÊM VÀO ĐẦU FILE:
import com.example.mywebsite.model.Cart;
import com.example.mywebsite.model.CartItem;
import com.example.mywebsite.repository.CartRepository;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.mywebsite.model.Order;
import com.example.mywebsite.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.security.Principal;
import java.util.List;

@Controller
public class ShopController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProductRepository productRepository;// Gọi Repository của MongoDB

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    private String getUserId(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken) {
            String email = (String) ((org.springframework.security.oauth2.core.user.OAuth2User)
                    ((OAuth2AuthenticationToken) principal).getPrincipal()).getAttribute("email");
            return userRepository.findByGmail(email).map(User::getUserId).orElse(null);
        } else {
            return userRepository.findByUsername(principal.getName()).map(User::getUserId).orElse(null);
        }
    }

    @GetMapping("/shop")
    public String showShopPage(@RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "12") int size, // 1 trang đầy = 12 sản phẩm
                               @RequestParam(value = "sort", defaultValue = "dateDesc") String sort,
                               @RequestParam(value = "category", required = false) String category,
                               Principal principal,
                               Model model) {
        // 1. Load thông tin User cho Header & Giỏ hàng
        String userId = getUserId(principal);
        if (userId != null) {
            userRepository.findByUserId(userId).ifPresent(user -> model.addAttribute("currentUser", user));
            userProfileRepository.findByUserId(userId).ifPresent(profile -> model.addAttribute("userProfile", profile));

            int cartCount = cartRepository.findByUserId(userId)
                    .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                    .orElse(0);
            model.addAttribute("cartCount", cartCount);
        }
        // 2. Logic sắp xếp
        Sort mongoSort = Sort.by(Sort.Direction.DESC, "id");
        if ("priceAsc".equals(sort)) {
            mongoSort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("priceDesc".equals(sort)) {
            mongoSort = Sort.by(Sort.Direction.DESC, "price");
        }

        // Đảm bảo số trang >= 1 (PageRequest trong Spring tính từ trang 0)
        int currentPage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(currentPage - 1, size, mongoSort);

        // 3. Truy vấn MongoDB lấy dữ liệu phân trang
        Page<Product> productPage;
        if (category == null || "all".equalsIgnoreCase(category)) {
            productPage = productRepository.findAll(pageable);
        } else {
            productPage = productRepository.findByCategory(category, pageable);
        }

        int totalPages = productPage.getTotalPages();
        if (totalPages == 0) totalPages = 1;

        // 4. Gửi dữ liệu ra View
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("currentSort", sort);
        model.addAttribute("selectedCategory", category != null ? category : "all");
        model.addAttribute("totalProducts", productPage.getTotalElements());

        return "shop";
    }
    // 1. TRANG CHI TIẾT SẢN PHẨM (Mô phỏng ảnh Riot Merch)
    @GetMapping("/shop/product/{id}")
    public String productDetail(@PathVariable("id") String id, Principal principal, Model model) {
        String userId = getUserId(principal);
        if (userId != null) {
            userRepository.findByUserId(userId).ifPresent(user -> model.addAttribute("currentUser", user));
            userProfileRepository.findByUserId(userId).ifPresent(profile -> model.addAttribute("userProfile", profile));
            // Đếm số lượng trong giỏ
            int cartCount = cartRepository.findByUserId(userId)
                    .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum()).orElse(0);
            model.addAttribute("cartCount", cartCount);
        }

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return "redirect:/shop";

        model.addAttribute("product", product);
        return "product-detail"; // Mở file product-detail.html
    }

    // 2. THÊM VÀO GIỎ HÀNG
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") String productId,
                            @RequestParam(value = "size", defaultValue = "Freesize") String size,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            @RequestParam(value = "action", defaultValue = "add") String action,
                            Principal principal) {
        String userId = getUserId(principal);
        if (userId == null) return "redirect:/login"; // Bắt buộc đăng nhập để mua hàng

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            Cart cart = cartRepository.findByUserId(userId).orElse(new Cart(userId));
            boolean found = false;

            // Nếu đã có sp + cùng size thì cộng dồn số lượng
            for (CartItem item : cart.getItems()) {
                if (item.getProductId().equals(productId) && item.getSize().equals(size)) {
                    item.setQuantity(item.getQuantity() + quantity);
                    found = true; break;
                }
            }
            if (!found) {
                cart.getItems().add(new CartItem(productId, product.getName(), product.getImage(), product.getPrice(), quantity, size));
            }
            cartRepository.save(cart);
        }

        // Bấm "Mua ngay" thì tới thẳng giỏ hàng, bấm "Thêm vào giỏ" thì load lại trang
        if ("buy_now".equals(action)) {
            return "redirect:/cart";
        }
        return "redirect:/shop/product/" + productId + "?added=true";
    }

    // 3. XEM GIỎ HÀNG
    @GetMapping("/cart")
    public String viewCart(Principal principal, Model model) {
        String userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        userRepository.findByUserId(userId).ifPresent(user -> model.addAttribute("currentUser", user));
        userProfileRepository.findByUserId(userId).ifPresent(profile -> model.addAttribute("userProfile", profile));

        Cart cart = cartRepository.findByUserId(userId).orElse(new Cart(userId));
        model.addAttribute("cart", cart);

        double total = cart.getItems().stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        model.addAttribute("total", total);
        model.addAttribute("cartCount", cart.getItems().stream().mapToInt(CartItem::getQuantity).sum());

        return "cart"; // Mở file cart.html
    }

    // 4. XÓA SẢN PHẨM KHỎI GIỎ
    @PostMapping("/cart/remove")
    public String removeCartItem(@RequestParam("productId") String productId,
                                 @RequestParam("size") String size,
                                 Principal principal) {
        String userId = getUserId(principal);
        if (userId != null) {
            cartRepository.findByUserId(userId).ifPresent(cart -> {
                cart.getItems().removeIf(item -> item.getProductId().equals(productId) && item.getSize().equals(size));
                cartRepository.save(cart);
            });
        }
        return "redirect:/cart";
    }
    // 5. TIẾN HÀNH ĐẶT HÀNG (CHECKOUT)
    @PostMapping("/checkout")
    public String processCheckout(Principal principal) {
        String userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        double total = cart.getItems().stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();

        // 1. Tạo đơn hàng mới và lưu vào collection 'orders' trong MongoDB
        Order newOrder = new Order(
                userId,
                new ArrayList<>(cart.getItems()),
                total,
                LocalDateTime.now(),
                "PAID"
        );
        Order savedOrder = orderRepository.save(newOrder);

        // 2. Làm sạch giỏ hàng trong MongoDB sau khi thanh toán thành công
        cart.getItems().clear();
        cartRepository.save(cart);

        // Chuyển hướng tới trang thông báo thanh toán thành công
        return "redirect:/order-success/" + savedOrder.getId();
    }

    // 6. TRANG THANH TOÁN THÀNH CÔNG
    @GetMapping("/order-success/{orderId}")
    public String showOrderSuccess(@PathVariable("orderId") String orderId, Principal principal, Model model) {
        String userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        userRepository.findByUserId(userId).ifPresent(user -> model.addAttribute("currentUser", user));
        userProfileRepository.findByUserId(userId).ifPresent(profile -> model.addAttribute("userProfile", profile));

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUserId().equals(userId)) {
            return "redirect:/shop";
        }

        model.addAttribute("order", order);
        return "order-success";
    }

    // 7. TRANG LỊCH SỬ MUA HÀNG
    @GetMapping("/orders")
    public String showOrderHistory(Principal principal, Model model) {
        String userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        userRepository.findByUserId(userId).ifPresent(user -> model.addAttribute("currentUser", user));
        userProfileRepository.findByUserId(userId).ifPresent(profile -> model.addAttribute("userProfile", profile));

        int cartCount = cartRepository.findByUserId(userId)
                .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum()).orElse(0);
        model.addAttribute("cartCount", cartCount);

        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        model.addAttribute("orders", orders);

        return "orders";
    }
    // Endpoint test: Tự động thêm 5 sản phẩm mới vào MongoDB để kiểm tra nhảy trang
    @GetMapping("/shop/add-test-products")
    public String addTestProducts() {
        for (int i = 1; i <= 5; i++) {
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
        return "redirect:/shop"; // Quay lại shop để thấy số trang tăng lên
    }
}