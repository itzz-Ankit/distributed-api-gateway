package Dto_req;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class CreateOrderRequest {

    // productName , quantity , price


    @NotBlank
    @NotNull(message = "name is essential ")
    private String productName ;

    @NotNull
    @Positive
    private Long quantity  ;



    public String getProductName() {

        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    @NotNull
    @Positive
    private Double price  ;

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
