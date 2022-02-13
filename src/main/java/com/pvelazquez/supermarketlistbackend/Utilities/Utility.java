package com.pvelazquez.supermarketlistbackend.Utilities;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Models.UserSignUp;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.stream.Collectors;

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

    public String getRefreshToken(HttpServletRequest request, org.springframework.security.core.userdetails.User user, Algorithm algorithm) {
        return JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + 21600 * 60 * 1000))
                .withIssuer(request.getRequestURL().toString())
                .sign(algorithm);
    }

    public String getAccessToken(HttpServletRequest request, org.springframework.security.core.userdetails.User user, Algorithm algorithm) {
        return JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + 300 * 60 * 1000))
                .withIssuer(request.getRequestURL().toString())
                .withClaim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .sign(algorithm);
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
