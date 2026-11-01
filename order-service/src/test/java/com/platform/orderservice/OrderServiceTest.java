package com.platform.orderservice;

import Dto_req.CreateOrderRequest;
import Dto_response.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderPersistsOrderWithPlacedStatus() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductName("Keyboard");
        request.setQuantity(2L);
        request.setPrice(49.99);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-1");
            return order;
        });

        OrderResponse response = orderService.createOrder(request);

        assertEquals("order-1", response.getId());
        assertEquals("Keyboard", response.getProductName());
        assertEquals("Placed", response.getStatus());
        assertEquals(2L, response.getQuantity().longValue());
    }

    @Test
    void getMissingOrderReturnsNotFound() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> orderService.getOrderById("missing"));
    }
}
