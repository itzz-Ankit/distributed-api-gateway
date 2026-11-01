package Dto_response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class OrderResponse {

     private String id;
    private String userId;
    private String productName;
    private Long quantity;
    private Double price;
    private String status;



// public OrderResponse UpdateOrder(String id, UpdateOrderRequest request) {
//
//        Order existingOrder = orderRepository.findById(id)
//              .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND, "Order not found"));
//
//        OrderMapper.updateEntity(existingOrder, request);
//
//        Order updatedOrder = orderRepository.save(existingOrder);
//
//        return OrderMapper.toResponse(updatedOrder);
}
