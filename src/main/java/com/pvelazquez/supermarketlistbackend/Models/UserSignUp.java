package com.pvelazquez.supermarketlistbackend.Models;

import lombok.Data;

@Data
public class UserSignUp {
        private String name;
        private String email;
        private String password;
        private String country;
        private String State;
        private String language;
}
