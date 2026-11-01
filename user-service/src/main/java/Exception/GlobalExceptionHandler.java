package Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception e ){

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occured")  ;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?>  handleValidationError(MethodArgumentNotValidException ex ){

        return ResponseEntity.badRequest().body("Please enter valid details ") ;
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleStatusException( ResponseStatusException responseStatusException) {
        return ResponseEntity.status(responseStatusException.getStatus()).body(responseStatusException.getReason());
    }

}
/*
  1) 200 OK — request successful hui.

 2)   400 Bad Request — input galat hai.

 3)  401 Unauthorized — login credentials invalid hain.

       4)   404 Not Found — requested resource nahi mila.

       5)  500 Internal Server Error — unexpected server error.



6)    503 Service Unavailable — server/service filhaal request handle nahi kar sakti.


 */
