package com.pvelazquez.supermarketlistbackend.Utilities;

import com.auth0.jwt.algorithms.Algorithm;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Models.UserSignUp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;

public class Utility {
    private static Utility utility_instance = null;
    private static Random generator = null;
    private Utility(){

    }

    public static Utility getInstance(){
        if(utility_instance == null)
            utility_instance = new Utility();
        return utility_instance;
    }

    public Algorithm getAlgorithm(){
        return Algorithm.HMAC256("EsteEsElSecreteKeyParaLaGeneraciondelToken".getBytes());
    }

    public String generateVerificationCode(){
        if(generator == null)
            generator = new Random();

        return Integer.toString(generator.nextInt(10000));
    }

    public Timestamp generate10Min(){
        return new Timestamp(System.currentTimeMillis() + 600000);
    }

    public Timestamp getCurrentTimestamp(){
        return new Timestamp(System.currentTimeMillis());
    }

    public User convertUserSignUpToUserModel(UserSignUp userSignUp){
        User user = new User();
        user.setId(null);
        user.setName(userSignUp.getName());
        user.setEmail(userSignUp.getEmail());
        user.setPassword(userSignUp.getPassword());
        user.setCountry(userSignUp.getCountry());
        user.setLanguage(userSignUp.getLanguage());
        user.setIsLocked(true);
        user.setVerificationCode(generateVerificationCode());
        user.setCodeExpirationDate(generate10Min());
        user.setRoles(new ArrayList<>());
        return user;
    }
}
