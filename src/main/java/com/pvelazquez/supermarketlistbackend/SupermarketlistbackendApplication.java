package com.pvelazquez.supermarketlistbackend;

import com.pvelazquez.supermarketlistbackend.Models.Role;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;

@SpringBootApplication
public class SupermarketlistbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupermarketlistbackendApplication.class, args);
	}
	/*
	@Bean
	CommandLineRunner run(UserService userService){
		return args -> {
			userService.saveRole(new Role(null,"ADMIN"));
			userService.saveRole(new Role(null,"USER"));

			userService.saveUser(new User(null,"Pedro", "admin@mail.com","1234","Mexico","Español",new ArrayList<>()));
			userService.saveUser(new User(null,"Eduardo", "user@mail.com","1234","Mexico","Español",new ArrayList<>()));

			userService.addRoleToUser("admin@mail.com", "ADMIN");
			userService.addRoleToUser("admin@mail.com", "USER");
			userService.addRoleToUser("user@mail.com", "USER");
		};
	}
	*/
	@Bean
	PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}


}
