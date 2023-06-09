package com.ecommerce.year2_sem2_project.Model.Service;

import com.ecommerce.year2_sem2_project.Model.DAO.OrderRepository;
import com.ecommerce.year2_sem2_project.Model.Entity.Order;
import com.ecommerce.year2_sem2_project.Model.Entity.OrderedItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/// Реалізація сервісу замовлень
@Service
@Scope("singleton")
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public void createOrder(Order order) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            orderRepository.save(order);
        });
        executorService.shutdown();
    }

    @Override
    public List<Order> getAllOrders() {
        int cores = Runtime.getRuntime().availableProcessors();
        int batchSize = 1000;
        List<Order> allProducts = new ArrayList<>();

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(cores);
            List<Callable<List<Order>>> tasks = new ArrayList<>();

            long totalProducts = orderRepository.count();
            long totalPages = (long) Math.ceil((double) totalProducts / batchSize);

            for (int i = 0; i < totalPages; i++) {
                int page = i;
                Callable<List<Order>> task = () -> {
                    Pageable pageable = PageRequest.of(page, batchSize);
                    return orderRepository.findAll(pageable).getContent();
                };
                tasks.add(task);
            }

            List<Future<List<Order>>> futures = executorService.invokeAll(tasks);
            executorService.shutdown();

            for (Future<List<Order>> future : futures) {
                allProducts.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }

        Collections.reverse(allProducts);

        return allProducts;
    }

    public double calculateTotalPrice(Order order) {
        double totalPrice = 0.0;

        List<OrderedItem> orderedItems = order.getOrderedItems();
        for (OrderedItem orderedItem : orderedItems) {
            double itemPrice = orderedItem.getProduct().getPrice();
            int quantity = orderedItem.getQuantity();
            double itemTotalPrice = itemPrice * quantity;
            totalPrice += itemTotalPrice;
        }

        return totalPrice;
    }

    @Override
    public Order getOrderById(Long orderId) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        return optionalOrder.orElse(null);
    }
}

