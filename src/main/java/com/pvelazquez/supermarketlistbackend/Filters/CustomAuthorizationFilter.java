package com.pvelazquez.supermarketlistbackend.Filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvelazquez.supermarketlistbackend.Models.Response;
import com.pvelazquez.supermarketlistbackend.Utilities.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.stream;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class CustomAuthorizationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
        if(req.getServletPath().equals("/api/signin") || req.getServletPath().equals("/api/token/refresh") ||
                req.getServletPath().equals("/api/signup/registration") ||
                req.getServletPath().equals("/api/signup/confirmation") ||
                req.getServletPath().equals("/api/signup/newcode") ||
                (req.getServletPath().contains("/api/images/profile") && (!req.getServletPath().contains("/api/images/profile/add")))){
            filterChain.doFilter(req,res);
        }else{
            String authorizationHeader = req.getHeader(AUTHORIZATION);
            if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
                try {
                    String token = authorizationHeader.substring("Bearer ".length());
                    Utility utility = Utility.getInstance();
                    Algorithm algorithm = utility.getAlgorithm();
                    JWTVerifier verifier = JWT.require(algorithm).build();
                    DecodedJWT decodedJWT = verifier.verify(token);
                    String email = decodedJWT.getSubject();

                    String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
                    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    stream(roles).forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email,null,authorities);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    filterChain.doFilter(req,res);
                }catch (Exception e){
                    log.info("Error logging in: {}", e.getMessage());
                    res.setHeader("error", e.getMessage());
                    res.setStatus(FORBIDDEN.value());

                    Response response1 = createResponse(e.getMessage(),FORBIDDEN,"Error while validating token");

                    res.setContentType(APPLICATION_JSON_VALUE);
                    new ObjectMapper().writeValue(res.getOutputStream(),response1);
                }
            }else {
                Response response1 = createResponse("Bearer token not found",BAD_REQUEST,"Token doesn't begins with Bearer");

                res.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(res.getOutputStream(),response1);
            }
        }
    }

    private Response createResponse(String error, HttpStatus httpStatus, String msg){
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("error", error);
        Response response1 = new Response();

        response1.setStatusCode(httpStatus.value());
        response1.setStatus(httpStatus);
        response1.setMessange(msg);
        response1.setData(errorMap);

        return response1;
    }
}

