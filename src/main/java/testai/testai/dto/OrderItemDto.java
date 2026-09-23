package testai.testai.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        String productName,
        Long quantity,
        BigDecimal price) {
}
