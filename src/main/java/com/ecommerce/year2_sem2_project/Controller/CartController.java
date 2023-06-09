package com.ecommerce.year2_sem2_project.Controller;

import com.ecommerce.year2_sem2_project.Model.Entity.Order;
import com.ecommerce.year2_sem2_project.Model.Entity.OrderedItem;
import com.ecommerce.year2_sem2_project.Model.Entity.Product;
import com.ecommerce.year2_sem2_project.Model.Service.OrderService;
import com.ecommerce.year2_sem2_project.Model.Service.ProductService;
import com.ecommerce.year2_sem2_project.Model.Payment_Strategy.CreditCardPaymentStrategy;
import com.ecommerce.year2_sem2_project.Model.Payment_Strategy.PayPalPaymentStrategy;
import com.ecommerce.year2_sem2_project.Model.Payment_Strategy.PaymentContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/// Контролер Spring MVC, відповідальний за обробку операцій, пов'язаних з кошиком та замовленнями
@Controller
@SessionAttributes("cart")
public class CartController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentContext paymentContext;

    @Autowired
    public CartController(PaymentContext paymentContext) {
        this.paymentContext = paymentContext;
    }

    public CartController(ProductService productService, OrderService orderService, PaymentContext paymentContext) {
        this.productService = productService;
        this.orderService = orderService;
        this.paymentContext = paymentContext;
    }

    @ModelAttribute("cart")
    public List<Product> initializeCart() {
        return new ArrayList<>();
    }

    /// Відображає вид кошика, показуючи вміст кошика та загальну вартість
    @GetMapping("/cart")
    public String showCart(Model model, @ModelAttribute("cart") List<Product> cart) {
        double totalPrice = 0;
        for (Product product : cart) {
            totalPrice += product.getPrice();
        }
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", totalPrice);
        return "cart";
    }

    /// Додає товар з вказаним ідентифікатором до кошика
    @PostMapping("/cart/add/{productId}")
    public String addToCart(@PathVariable("productId") Long productId, @ModelAttribute("cart") List<Product> cart) {
        Product product = productService.getProductById(productId);
        if (product != null) {
            cart.add(product);
        }
        return "redirect:/cart";
    }

    /// Видаляє товар з вказаним ідентифікатором з кошика
    @PostMapping("/cart/remove/{productId}")
    public String removeFromCart(@PathVariable("productId") Long productId, @ModelAttribute("cart") List<Product> cart) {
        Product productToRemove = null;
        for (Product product : cart) {
            if (product.getId().equals(productId)) {
                productToRemove = product;
                break;
            }
        }
        if (productToRemove != null) {
            cart.remove(productToRemove);
        }
        return "redirect:/cart";
    }

    /// Відображає форму для створення замовлення.
    @GetMapping("/cart/order")
    public String showOrderForm(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "order-form";
    }

    /// Створює замовлення на основі наданої інформації та обробляє платіж.
    @PostMapping("/cart/order/create")
    public String createOrder(@RequestParam("customerName") String customerName,
                              @RequestParam("email") String email,
                              @RequestParam("address") String address,
                              @RequestParam("paymentMethod") String paymentMethod,
                              @ModelAttribute("cart") List<Product> cart,
                              HttpSession session,
                              Model model) {

        long totalPrice = 0;
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setEmail(email);
        order.setAddress(address);

        List<OrderedItem> orderedItems = new ArrayList<>();

        for (Product product : cart) {
            OrderedItem orderedItem = new OrderedItem();
            orderedItem.setProduct(product);
            orderedItem.setQuantity(1);
            orderedItem.setPrice(product.getPrice());

            orderedItems.add(orderedItem);
            totalPrice += orderedItem.getPrice();
        }

        order.setTotalPrice(totalPrice);
        order.setOrderedItems(orderedItems);

        switch (paymentMethod) {
            case "creditCard":
                paymentContext.setPaymentStrategy(new CreditCardPaymentStrategy());
                break;
            case "payPal":
                paymentContext.setPaymentStrategy(new PayPalPaymentStrategy());
                break;
        }

        paymentContext.makePayment(totalPrice);

        orderService.createOrder(order);

        model.addAttribute("order", order);

        cart.clear();

        return "order-success";
    }
}

