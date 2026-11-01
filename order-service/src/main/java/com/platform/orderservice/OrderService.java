package com.platform.orderservice;

import Dto_req.CreateOrderRequest;
import Dto_req.UpdateOrderRequest;
import Dto_response.OrderResponse;
import Mapper.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static java.util.stream.Collectors.toList;


@Service
public class OrderService {

    private final OrderRepository orderRepository ;

    public OrderService(OrderRepository orderRepository){
        this.orderRepository = orderRepository ;
    }

    public OrderResponse createOrder(CreateOrderRequest request){

        Order order  = OrderMapper.toEntity(request)  ;
        order.setStatus("Placed");

        Order savedOrder = orderRepository.save(order)  ;
        return OrderMapper.toResponse(savedOrder)  ;
    }

    public OrderResponse updateOrder(String id , UpdateOrderRequest request ){

        Order existingorder = orderRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"))  ;
        OrderMapper.updateEntity(existingorder , request )  ;

        Order updateOrder = orderRepository.save(existingorder) ;

        return OrderMapper.toResponse(updateOrder) ;

    }

    public OrderResponse updateOrderStatus(String id , String status  ){

        Order order = orderRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND , "order not found")) ;
        order.setStatus(status);


        Order updatedOrder = orderRepository.save(order) ;

        return OrderMapper.toResponse(updatedOrder) ;






    }

    public OrderResponse getOrderById( String id ) {

        Order order = orderRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND , "order not found")) ;
        return OrderMapper.toResponse(order);

    }
    public void deleteOrder(String id ){
        if (!orderRepository.existsById(id)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND , "order not found ")  ;

        }
        orderRepository.deleteById(id);


    }

    public List<OrderResponse> getAllOrders(){
        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::toResponse)
                .collect(toList()) ;
    }


}
