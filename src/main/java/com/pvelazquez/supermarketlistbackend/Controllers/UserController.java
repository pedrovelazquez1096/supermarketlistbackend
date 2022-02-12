package com.pvelazquez.supermarketlistbackend.Controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvelazquez.supermarketlistbackend.Models.Role;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Models.UserSignUp;
import com.pvelazquez.supermarketlistbackend.Services.UserService;
import com.pvelazquez.supermarketlistbackend.Utilities.Utility;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final Utility utility = Utility.getInstance();

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(){
        return ResponseEntity.ok().body(userService.getUsers());
    }

    @PostMapping("/user/save")
    public ResponseEntity<User> saveUser(@RequestBody User user) throws Exception{
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/user/save").toUriString());
        return ResponseEntity.created(uri).body(userService.saveUser(user));
    }

    @PostMapping("/signup/registration")
    public ResponseEntity<?> signupUser(@RequestBody UserSignUp userSignUpForm) throws Exception {
        User user = userService.getUser(userSignUpForm.getEmail());
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/signup/registration").toUriString());
        if(user == null)
        {
            user = utility.convertUserSignUpToUserModel(userSignUpForm);
            //TODO
            //extract verification code an use a utily class to send an email with it
            user = userService.saveUser(user);
        }else{
            Long id = user.getId();
            user = utility.convertUserSignUpToUserModel(userSignUpForm);
            user = userService.updateUser(user,id);
        }
        return ResponseEntity.ok("Check your email for the verification code");
    }

    @PostMapping("/user/assigneRole")
    public ResponseEntity<?> assigneRoleToUser(@RequestBody RoleToUserForm form){
        userService.addRoleToUser(form.getEmail(), form.getRoleName());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/role/save")
    public ResponseEntity<Role> saveRole(@RequestBody Role role) throws Exception{
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/role/save").toUriString());
        return ResponseEntity.created(uri).body(userService.saveRole(role));
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