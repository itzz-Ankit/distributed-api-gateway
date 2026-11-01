package com.platform.orderservice;
import Dto_req.CreateOrderRequest;
import Dto_req.UpdateOrderRequest;
import Dto_response.OrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderRepository orderRepository ;
  private final OrderService orderService ;


  public OrderController(OrderRepository orderRepository  , OrderService orderService){
      this.orderRepository = orderRepository ;
      this.orderService = orderService  ;
  }

    @PostMapping
    public ResponseEntity<OrderResponse> irgp(@Valid @RequestBody CreateOrderRequest request) {

      OrderResponse response = orderService.createOrder(request);

      return ResponseEntity.status(HttpStatus.CREATED).body(response) ;









    }

    @GetMapping("/{id}")
   public ResponseEntity<OrderResponse> getorderById(@PathVariable String id ){
    return ResponseEntity.ok(orderService.getOrderById(id));
  }

    @GetMapping
    public List<OrderResponse> getAllOrders(){
        return orderService.getAllOrders();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable  String id ){

      orderService.deleteOrder(id);
      return ResponseEntity.noContent().build() ;
    }



    @PutMapping("/{id}")
    public  ResponseEntity<OrderResponse> updateOrder(@PathVariable String id  , @Valid @RequestBody UpdateOrderRequest request){

      return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse>updateOrderStatus(@PathVariable String id , @RequestParam String status  ){
      return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
  }





}
