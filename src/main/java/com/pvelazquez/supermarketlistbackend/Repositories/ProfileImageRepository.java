package com.pvelazquez.supermarketlistbackend.Repositories;

import com.pvelazquez.supermarketlistbackend.Models.ProfileImage;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface ProfileImageRepository  extends MongoRepository<ProfileImage, String> {
}
