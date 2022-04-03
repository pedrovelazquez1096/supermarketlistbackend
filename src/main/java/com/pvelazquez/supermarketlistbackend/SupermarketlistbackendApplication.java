package com.pvelazquez.supermarketlistbackend;

import com.pvelazquez.supermarketlistbackend.Models.Role;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.util.ArrayList;
@Slf4j
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
			log.info("Roles added");
			userService.saveUser(new User(null,"Pedro", "admin@mail.com","1234","Mexico","Español", false, "1234", new Timestamp(System.currentTimeMillis()),new ArrayList<>()));
			log.info("Admin added");
			userService.addRoleToUser("admin@mail.com", "ADMIN");
			userService.addRoleToUser("admin@mail.com", "USER");
			log.info("Roles added to admin");
		};
	}
*/
	@Bean
	PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}


}
