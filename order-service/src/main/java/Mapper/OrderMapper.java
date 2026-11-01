package Mapper;

import Dto_req.CreateOrderRequest;
import Dto_req.UpdateOrderRequest;
import com.platform.orderservice.Order;
import Dto_response.OrderResponse;


public class OrderMapper {


    // create order request
    public static Order toEntity(CreateOrderRequest request){

        Order order = new Order() ;

        // productName , quantity , price

        order.setProductName(request.getProductName())  ;
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());

        return order ;
    }
    // update order request

    public static void updateEntity(Order order , UpdateOrderRequest request) {
        order.setProductName(request.getProductName())  ;
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());


    }
    // order -> response
    public static OrderResponse toResponse(Order order ){
        return new OrderResponse(
                order.getId() ,
                order.getUserId(),
                order.getProductName() ,
                order.getQuantity() ,
                order.getPrice() ,
                order.getStatus()
        );
    }




}
