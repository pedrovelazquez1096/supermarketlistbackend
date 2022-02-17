package com.pvelazquez.supermarketlistbackend.Controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        if(!utility.validateEmail(userSignUpForm.getEmail()))
            return ResponseEntity.badRequest().body("Invalid Email");

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
    public ResponseEntity<?> assignRoleToUser(@RequestBody RoleToUserForm form){
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