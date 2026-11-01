package Mapper;

import com.platform.userservice.User;
import dto_response.UserResponse;

public class UserMapper {

    public static UserResponse toResponse(User user ){
        return new UserResponse(
                user.getId() ,
                user.getName() ,
                user.getEmail()
        );
    }







}
