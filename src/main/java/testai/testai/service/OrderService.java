package testai.testai.service;

import jakarta.persistence.EntityNotFoundException;
import testai.testai.dto.OrderDto;
import testai.testai.dto.OrderItemDto;
import testai.testai.model.Order;
import testai.testai.model.OrderItem;
import testai.testai.model.enums.OrderStatus;
import testai.testai.repository.OrderItemRepository;
import testai.testai.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

//    public void createOrder(OrderDto orderDto, List<OrderItemDto> orderItemDtos) {
//        List<OrderItem> savedItems = orderItemDtos.stream()
//                .map(dto -> orderItemRepository.save(
//                        OrderItem.builder()
//                                .productName(dto.productName())
//                                .quantity(dto.quantity())
//                                .price(dto.price())
//                                .build()))
//                .toList();
//
//        Order newOrder = Order.builder()
//                .customerName(orderDto.customerName())
//                .status(orderDto.status())
//                .totalAmount(orderDto.totalAmount())
//                .orderItems(savedItems)
//                .build();
//
//        orderRepository.save(newOrder);
//    }
//
//
//    public void cancelOrder(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден: " + orderId));
//
//        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
//            throw new IllegalStateException("Заказ " + orderId + " уже в статусе " + order.getStatus());
//        }
//
//        order.setStatus(OrderStatus.CANCELLED);
//        orderRepository.save(order);
//    }
//
//    public BigDecimal calculateTotal(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден: " + orderId));
//
//        return order.getOrderItems().stream()
//                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//
//    public void applyDiscount(Long orderId, BigDecimal percent) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден: " + orderId));
//
//        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
//                percent.divide(BigDecimal.valueOf(100)));
//
//        order.setTotalAmount(order.getTotalAmount().multiply(discountMultiplier));
//        orderRepository.save(order);
//    }
//
//    public List<Order> findOrdersByStatus(OrderStatus status) {
//        return orderRepository.findByStatus(status);
//    }

    public void addItemToOrder(Long orderId, OrderItemDto itemDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден: " + orderId));

        OrderItem newItem = orderItemRepository.save(
                OrderItem.builder()
                        .productName(itemDto.productName())
                        .quantity(itemDto.quantity())
                        .price(itemDto.price())
                        .build());

        order.getOrderItems().add(newItem);
        order.setTotalAmount(order.getTotalAmount().add(
                newItem.getPrice().multiply(BigDecimal.valueOf(newItem.getQuantity()))));

        orderRepository.save(order);
    }
}
