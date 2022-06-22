package com.pvelazquez.supermarketlistbackend.Services;

import com.pvelazquez.supermarketlistbackend.Models.Role;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Repositories.RoleRepository;
import com.pvelazquez.supermarketlistbackend.Repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional @Slf4j
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        if(user == null) {
            throw new UsernameNotFoundException("User not found in the database");
        }else if(user.getIsLocked()){
            throw new UsernameNotFoundException("User found in the database yet not confirmed");
        }else {
            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getName())));
            return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
        }
    }

    public User saveUser(User user) throws Exception{
        User userTemp = userRepository.findByEmail(user.getEmail());
        if(userTemp != null){
            throw new Exception("Email already in use");
        }else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        }
    }

    public User updateUser(User user, Long id) throws Exception {
        if (userRepository.findById(id).isPresent()) {
            user.setId(id);
            return userRepository.save(user);
        } else {
            throw new Exception("User not found");
        }
    }

    public String deleteUser(String email){
        try {
            userRepository.deleteByEmail(email);
            return "user deleted";
        }catch (Exception e){
            return "user not found";
        }
    }

    public Role saveRole(Role role) throws Exception{
        Role roleTemp = roleRepository.findByName(role.getName());
        if(roleTemp != null)
            throw new Exception("Role already exists");
        return roleRepository.save(role);
    }

    public String addRoleToUser(String email, String roleName){
        User user = userRepository.findByEmail(email);
        Role role = roleRepository.findByName(roleName);
        try {
            user.getRoles().add(role); //Due to Transactional library we don't need to call userRepo to save the user
            return "User: " + email + " assigned to: " + roleName;
        }catch (Exception e){
            return "Error assigning role to user";
        }
    }

    public User getUser(String email){
        return userRepository.findByEmail(email);
    }

    public List<User> getUsers(){
        return userRepository.findAll();
    }

    public User addProfileImageToUser(String email, String profileImageURL)throws Exception{
        User userTemp = userRepository.findByEmail(email);
        if(userTemp == null)
            throw new Exception("User not found");

        userTemp.setProfileImageURL(profileImageURL);

        return userRepository.save(userTemp);
    }
}
