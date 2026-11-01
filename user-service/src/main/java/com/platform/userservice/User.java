package com.platform.userservice;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

// id , name , password , email


@Document(collection = "users")
public class User {

    @Id
    private String id  ;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NotBlank(message = "name is required")
    private String name ;

    @NotBlank(message = "password is required")
    @Size(min = 8  , message = "Password must be at least 8 characters" , max = 248939  )
    private String password ;

    @NotBlank(message = "email is required")
    @Email(message = "dont enter the temporary email ")
    private String email ;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



}
