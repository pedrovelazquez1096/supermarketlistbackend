package com.pvelazquez.supermarketlistbackend.Controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvelazquez.supermarketlistbackend.Models.Response;
import com.pvelazquez.supermarketlistbackend.Models.Role;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Services.UserService;
import com.pvelazquez.supermarketlistbackend.Utilities.Utility;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static java.util.Map.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<Response> getUsers(){
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(now())
                        .data(of("users", userService.getUsers()))
                        .messange("Users retrieved")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    @PostMapping("/user/save")
    public ResponseEntity<Response> saveUser(@RequestBody User user) throws Exception{
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(now())
                        .data(of("user", userService.saveUser(user)))
                        .messange("User created")
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build()
        );
    }

    @PostMapping("/user/assignRole")
    public ResponseEntity<Response> assignRoleToUser(@RequestBody RoleToUserForm form){

        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(now())
                        .data(of("assigned_role", userService.addRoleToUser(form.getEmail(), form.getRoleName())))
                        .messange("Role assigned to user successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    @PostMapping("/role/save")
    public ResponseEntity<Response> saveRole(@RequestBody Role role) throws Exception{
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(now())
                        .data(of("role", userService.saveRole(role)))
                        .messange("Role created successfully")
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build()
        );
    }

    @GetMapping("/token/refresh")
    public void refreshToken(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String authorizationHeader = req.getHeader(AUTHORIZATION);
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
            try {
                String refreshToken = authorizationHeader.substring("Bearer ".length());
                Utility utility = Utility.getInstance();
                Algorithm algorithm = utility.getAlgorithm();
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(refreshToken);
                String email = decodedJWT.getSubject();
                User user = userService.getUser(email);

                String accessToken = JWT.create()
                        .withSubject(user.getEmail())
                        .withExpiresAt(new Date(System.currentTimeMillis() + 300 * 60 * 1000))
                        .withIssuer(req.getRequestURL().toString())
                        .withClaim("roles",user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                        .sign(algorithm);
                Map<String, String> tokens = new HashMap<>();
                tokens.put("access_token", accessToken);
                tokens.put("refresh_token", refreshToken);
                res.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(res.getOutputStream(),tokens);
            }catch (Exception e){
                res.setHeader("error", e.getMessage());
                res.setStatus(FORBIDDEN.value());
                //res.sendError(FORBIDDEN.value());
                Map<String, String> error = new HashMap<>();
                error.put("error_message", e.getMessage());
                res.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(res.getOutputStream(),error);
            }
        }else {
            throw new RuntimeException("Refresh token is missing");
        }
    }
}

@Data
class RoleToUserForm{
    private String email;
    private String roleName;
}
