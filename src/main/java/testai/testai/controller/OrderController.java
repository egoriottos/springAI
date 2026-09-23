package testai.testai.controller;

import testai.testai.dto.OrderDto;
import testai.testai.dto.OrderItemDto;
import testai.testai.model.Order;
import testai.testai.model.enums.OrderStatus;
import testai.testai.service.OrderService;
import testai.testai.utils.TestGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
//    private final OrderService orderService;
//
//    public record CreateOrderRequest(OrderDto order, List<OrderItemDto> items) {
//    }
//
//    @PostMapping
//    public void createOrder(@RequestBody CreateOrderRequest request) {
//        orderService.createOrder(request.order(), request.items());
//    }
//
//    @PostMapping("/{orderId}/cancel")
//    public void cancelOrder(@PathVariable Long orderId) {
//        orderService.cancelOrder(orderId);
//    }
//
//    @GetMapping("/{orderId}/total")
//    public BigDecimal calculateTotal(@PathVariable Long orderId) {
//        return orderService.calculateTotal(orderId);
//    }
//
//    @PostMapping("/{orderId}/discount")
//    public void applyDiscount(@PathVariable Long orderId, @RequestBody BigDecimal percent) {
//        orderService.applyDiscount(orderId, percent);
//    }
//
//    @GetMapping
//    public List<Order> findOrdersByStatus(@RequestParam OrderStatus status) {
//        return orderService.findOrdersByStatus(status);
//    }
//
//    @PostMapping("/{orderId}/items")
//    public void addItemToOrder(@PathVariable Long orderId, @RequestBody OrderItemDto item) {
//        orderService.addItemToOrder(orderId, item);
//    }
}
