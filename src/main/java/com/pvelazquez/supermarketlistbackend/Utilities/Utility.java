package com.pvelazquez.supermarketlistbackend.Utilities;

import com.auth0.jwt.algorithms.Algorithm;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Models.UserSignUp;

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

    public String generateVerificationCode(int upperLimit){
        if(generator == null)
            generator = new Random();

        return Integer.toString(generator.nextInt(upperLimit));
    }

    public User convertUserSignUpToUserModel(UserSignUp userSignUp){
        User user = new User();
        user.setId(null);
        user.setName(userSignUp.getName());
        user.setEmail(userSignUp.getEmail());
        user.setPassword(userSignUp.getPassword());
        user.setCountry(userSignUp.getCountry());
        user.setLanguage(userSignUp.getLanguage());
        user.setLocked(true);
        user.setVerificacionCode(generateVerificationCode(10000));
        user.setRoles(new ArrayList<>());
        return user;
    }
}
