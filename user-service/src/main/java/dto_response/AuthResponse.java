package dto_response;

public class AuthResponse {


    private String message ;
    private String accessToken ;
    private String tokenType ;


    public  AuthResponse(String message , String accessToken  , String tokenType ) {

        this.message = message;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }
         public String getMessage(){
            return message ;

        }

        public  String getAccessToken(){
            return accessToken ;
        }

        public String getTokenType(){
            return tokenType ;
        }

    }


