package com.pvelazquez.supermarketlistbackend.Utilities;

import com.auth0.jwt.algorithms.Algorithm;

public class Utility {
    private static Utility utility_instance = null;

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
}
