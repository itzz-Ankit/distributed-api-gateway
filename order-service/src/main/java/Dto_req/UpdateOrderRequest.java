package Dto_req;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
public class UpdateOrderRequest {


    @NotBlank(message = "product name is required")
    private String productName ;

    @NotNull(message = "quantity is req")
    @Positive( message = " must be positive")
    private Long quantity ;


    @NotNull(message = " price is req")
    @Positive(message = "must be positive")
    private Double price ;



}
