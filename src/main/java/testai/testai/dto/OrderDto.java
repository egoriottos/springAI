package testai.testai.dto;

import testai.testai.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record OrderDto(String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        List<Long> orderItemIds) {
}
