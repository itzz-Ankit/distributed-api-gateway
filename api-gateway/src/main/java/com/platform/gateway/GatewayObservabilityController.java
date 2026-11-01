package com.platform.gateway;


import Dto.request;
import Dto.response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // controller  + response body
@RequestMapping("/gateway/observability")

public class GatewayObservabilityController {

    private final Service service ;
    public GatewayObservabilityController (Service service ){
        this.service = service ;
    }

    @PostMapping
    public response getObservability(@RequestBody request request){
        return service.getObservability(request) ;
    }



}
