package com.pvelazquez.supermarketlistbackend.Repositories;

import com.pvelazquez.supermarketlistbackend.Models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}