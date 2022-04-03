package com.pvelazquez.supermarketlistbackend.Controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvelazquez.supermarketlistbackend.Models.Response;
import com.pvelazquez.supermarketlistbackend.Models.Role;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Models.UserSignUp;
import com.pvelazquez.supermarketlistbackend.Utilities.EmailSender;
import com.pvelazquez.supermarketlistbackend.Services.UserService;
import com.pvelazquez.supermarketlistbackend.Utilities.Utility;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final EmailSender emailSender;
    private final Utility utility = Utility.getInstance();

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

    @PostMapping("/signup/registration")
    public ResponseEntity<?> signupUser(@RequestBody UserSignUp userSignUpForm) throws Exception {
        if(!utility.validateEmail(userSignUpForm.getEmail()))
            return ResponseEntity.badRequest().body("Invalid Email");
        if(utility.validatePassword(userSignUpForm.getPassword()) != null)
            return ResponseEntity.badRequest().body(utility.validatePassword(userSignUpForm.getPassword()));

        User user = userService.getUser(userSignUpForm.getEmail());
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/signup/registration").toUriString());

        if(user == null)
        {
            user = utility.convertUserSignUpToUserModel(userSignUpForm);
            emailSender.sendEmail(user.getEmail(), "Confirm your email", utility.buildBodyEmailConfirmation(user.getName(), utility.generateActivationLink(user.getEmail(), user.getVerificationCode()), user.getVerificationCode()));
            user = userService.saveUser(user);
            userService.addRoleToUser(user.getEmail(), "USER");
            log.info("User: {} Verification code: {} expiration date: {}", user.getEmail(), user.getVerificationCode(), user.getCodeExpirationDate());
            return ResponseEntity.created(uri).body("Please check your email for the verification code");
        }else
            return ResponseEntity.status(CONFLICT).body("Email already in use");
    }

    @PostMapping("/signup/newcode")
    public ResponseEntity<?> sendNewVerificationCode(@RequestParam("email")String email) throws Exception {
        if(!utility.validateEmail(email))
            return ResponseEntity.badRequest().body("Invalid Email");
        User user = userService.getUser(email);
        if(user == null){
            return ResponseEntity.notFound().build();
        }else {
            log.info("user found, account status: {}", user.getIsLocked());
            if(user.getIsLocked()) {
                user.setVerificationCode(utility.generateVerificationCode());
                user.setCodeExpirationDate(utility.generate10MinCode());
                user = userService.updateUser(user, user.getId());
                emailSender.sendEmail(user.getEmail(),"Confirm your email", utility.buildBodyEmailConfirmation(user.getName(), utility.generateActivationLink(user.getEmail(),user.getVerificationCode()), user.getVerificationCode()));
                log.info("User {} resets verification code: {}", user.getEmail(), user.getVerificationCode());
                return ResponseEntity.accepted().body("Please check your email for the verification code");
            }else
                return ResponseEntity.status(CONFLICT).body("Account already in unlocked");
        }
    }

    @GetMapping("/signup/confirmation")
    public ResponseEntity<?> unlockAccount(@RequestParam("email")String email, @RequestParam("code")String code) throws Exception {
        if(!utility.validateEmail(email))
            return ResponseEntity.badRequest().body("Invalid Email");
        User user = userService.getUser(email);
        if(user != null){
            if(user.getIsLocked())
                if(user.getCodeExpirationDate().after(utility.getCurrentTimestamp()))
                    if(user.getVerificationCode().equals(code)){
                        user.setIsLocked(false);
                        log.info("User: {} unlocked they account", user.getEmail());
                        user = userService.updateUser(user, user.getId());
                        emailSender.sendEmail(user.getEmail(),"Account Activated", utility.buildBodyEmailActivation(user.getName()));
                        return ResponseEntity.accepted().body("Activated");
                    }else
                        return ResponseEntity.status(NOT_ACCEPTABLE).body("Confirmation code incorrect");
                else
                    return ResponseEntity.status(NOT_ACCEPTABLE).body("Confirmation code has expired");
            else
                return ResponseEntity.status(CONFLICT).body("Account already unlocked");
        }else
            return ResponseEntity.notFound().build();
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
                Response response1 = new Response();
                response1.setStatusCode(200);
                response1.setStatus(OK);
                response1.setMessange("Tokens generated");
                response1.setData(tokens);
                res.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(res.getOutputStream(),response1);
            }catch (Exception e){
                res.setHeader("error", e.getMessage());
                res.setStatus(FORBIDDEN.value());

                Map<String, String> error = new HashMap<>();
                error.put("error_message", e.getMessage());

                Response response1 = new Response();
                response1.setStatusCode(FORBIDDEN.value());
                response1.setStatus(FORBIDDEN);
                response1.setMessange("Error generating token");
                response1.setData(error);

                res.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(res.getOutputStream(),response1);
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
