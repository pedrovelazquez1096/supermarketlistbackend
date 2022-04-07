package com.pvelazquez.supermarketlistbackend.Repositories;

import com.pvelazquez.supermarketlistbackend.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    List<User> findUsersByLanguageAndCountry(String language, String country);
    void deleteByEmail(String email);
}
