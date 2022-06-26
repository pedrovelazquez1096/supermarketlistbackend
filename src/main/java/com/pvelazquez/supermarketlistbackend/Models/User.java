package com.pvelazquez.supermarketlistbackend.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import static javax.persistence.FetchType.EAGER;
import static javax.persistence.GenerationType.*;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = AUTO)
    private Long id;
    private String name;
    @Column(unique=true)
    private String email;
    private String password;
    private String country;
    private String state;
    private String language;
    private Boolean isLocked;
    private String verificationCode;
    private Timestamp codeExpirationDate;
    private Timestamp joiningDate;
    private Timestamp confirmationDate;
    private String profileImageURL;
    @ManyToMany(fetch = EAGER)
    private Collection<Role> roles = new ArrayList<>();
}
