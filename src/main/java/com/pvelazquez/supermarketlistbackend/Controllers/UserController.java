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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
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
        return utility.createResponseEntity("users",
                userService.getUsers(), "user retrived", OK);
    }

    @GetMapping("/users/me")
    public ResponseEntity<Response> getMe(HttpServletRequest req, HttpServletResponse res){
        User user = userService.getUserByToken(req);

        return utility.createResponseEntity("me", user, "user retrieved", OK);
    }

    @DeleteMapping("/user/delete/{email}")
    public ResponseEntity<Response> deleteUser(@PathVariable String email){
        String msgCode = userService.deleteUser(email);
        return utility.createResponseEntity("user", msgCode, msgCode, ACCEPTED);
    }

    @PostMapping("/user/save")
    public ResponseEntity<Response> saveUser(@RequestBody User user) throws Exception{
        return utility.createResponseEntity("user", userService.saveUser(user), "user created", CREATED);
    }

    @PostMapping("/signup/registration")
    public ResponseEntity<?> signupUser(@RequestBody UserSignUp userSignUpForm) throws Exception {
        if(!utility.validateEmail(userSignUpForm.getEmail()))
            return utility.createResponseEntity("error", "Invalid Email", "invalid email", BAD_REQUEST);
        if(utility.validatePassword(userSignUpForm.getPassword()) != null)
            return utility.createResponseEntity("error", utility.validatePassword(userSignUpForm.getPassword()), "invalid password",NOT_ACCEPTABLE);

        User user = userService.getUser(userSignUpForm.getEmail());

        if(user != null)
            return utility.createResponseEntity("error", "Email already in use", "Email already in use", CONFLICT);

        user = userService.saveUser(utility.convertUserSignUpToUserModel(userSignUpForm));
        emailSender.sendEmail(user.getEmail(), "Confirm your email", utility.buildBodyEmailConfirmation(user.getName(), utility.generateActivationLink(user.getEmail(), user.getVerificationCode()), user.getVerificationCode()));
        userService.addRoleToUser(user.getEmail(), "USER");
        log.info("User: {} Verification code: {} expiration date: {}", user.getEmail(), user.getVerificationCode(), user.getCodeExpirationDate());
        return utility.createResponseEntity("data", "Please check your email for the verification code",
                "Please check your email for the verification code", ACCEPTED);
    }

    @PostMapping("/signup/newcode")
    public ResponseEntity<?> sendNewVerificationCode(@RequestParam("email")String email) throws Exception {
        if(!utility.validateEmail(email))
            return utility.createResponseEntity("error", "invalid email", "invalid email",BAD_REQUEST);

        User user = userService.getUser(email);
        if(user == null)
            return utility.createResponseEntity("error", "User not found", "user not found", NOT_FOUND);

        log.info("user found, account status: {}", user.getIsLocked());
        if(!user.getIsLocked())
            return utility.createResponseEntity("error","Account already unlocked", "Account already unlocked", CONFLICT);

        user.setVerificationCode(utility.generateVerificationCode());
        user.setCodeExpirationDate(utility.generate10MinExpirationDate());
        user = userService.updateUser(user, user.getId());
        emailSender.sendEmail(user.getEmail(),"Confirm your email", utility.buildBodyEmailConfirmation(user.getName(), utility.generateActivationLink(user.getEmail(),user.getVerificationCode()), user.getVerificationCode()));
        log.info("User {} resets verification code: {}", user.getEmail(), user.getVerificationCode());
        return utility.createResponseEntity("data","Please check your email for the verification code",
                "Please check your email for the verification code", ACCEPTED);

    }

    @GetMapping("/signup/confirmation")
    public ResponseEntity<?> unlockAccount(@RequestParam("email")String email, @RequestParam("code")String code) throws Exception {
        if(!utility.validateEmail(email))
            return utility.createResponseEntity("error","Invalid Email", "invalid email", BAD_REQUEST);

        User user = userService.getUser(email);

        if(user == null)
            return utility.createResponseEntity("error","User Not Found","User Not Found", NOT_FOUND);
        if(!user.getIsLocked())
            return utility.createResponseEntity("error","Account already unlocked","Account already unlocked", CONFLICT);
        if(!user.getCodeExpirationDate().after(utility.getCurrentTimestamp()))
            return utility.createResponseEntity("error","Confirmation code has expired", "Confirmation code has expired", NOT_ACCEPTABLE);
        if(!user.getVerificationCode().equals(code))
            return  utility.createResponseEntity("error","Confirmation code incorrect","Confirmation code incorrect", NOT_ACCEPTABLE);

        user.setIsLocked(false);
        user.setConfirmationDate(new Timestamp(System.currentTimeMillis()));
        log.info("User: {} unlocked they account", user.getEmail());
        user = userService.updateUser(user, user.getId());
        emailSender.sendEmail(user.getEmail(),"Account Activated", utility.buildBodyEmailActivation(user.getName()));
        return utility.createResponseEntity("data",user, "Account Activated", OK);
    }

    @PostMapping("/user/assignRole")
    public ResponseEntity<Response> assignRoleToUser(@RequestBody RoleToUserForm form){
        return utility.createResponseEntity("assigned_role", userService.addRoleToUser(form.getEmail(), form.getRoleName()), "Role assigned to user successfully", OK);
    }

    @PostMapping("/role/save")
    public ResponseEntity<Response> saveRole(@RequestBody Role role) throws Exception{
        return utility.createResponseEntity("role", userService.saveRole(role),"Role created successfully", CREATED);
    }

    @GetMapping("/token/refresh")
    public void refreshToken(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String authorizationHeader = req.getHeader(AUTHORIZATION);
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
            try {
                String refreshToken = authorizationHeader.substring("Bearer ".length());
                //Utility utility = Utility.getInstance();
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
                Response response1 = new Response();
                tokens.put("access_token", accessToken);
                tokens.put("refresh_token", refreshToken);
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

@Data
class ConfirmationForm{
    private String email;
    private String code;
}