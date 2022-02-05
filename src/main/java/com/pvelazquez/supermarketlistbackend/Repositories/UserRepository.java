package com.pvelazquez.supermarketlistbackend.Repositories;

import com.pvelazquez.supermarketlistbackend.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
