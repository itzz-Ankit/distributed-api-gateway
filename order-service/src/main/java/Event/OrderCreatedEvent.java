package Event;

import lombok.Data;


@Data
public class OrderCreatedEvent {

    private String orderId ;
    private String userId ;
    private String  productName ;
    private Long quantity ;
    private Double price ;
    private String status ;

}
