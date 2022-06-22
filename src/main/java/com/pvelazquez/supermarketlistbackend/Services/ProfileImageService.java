package com.pvelazquez.supermarketlistbackend.Services;

import com.pvelazquez.supermarketlistbackend.Constants.SuperMarketListConstants;
import com.pvelazquez.supermarketlistbackend.Models.ProfileImage;
import com.pvelazquez.supermarketlistbackend.Repositories.ProfileImageRepository;
import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.pvelazquez.supermarketlistbackend.Constants.SuperMarketListConstants.*;

@Service
public class ProfileImageService {
    @Autowired
    private ProfileImageRepository profileImageRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public String addProfileImage(MultipartFile file) throws IOException{
        ProfileImage profileImage = new ProfileImage();

        profileImage.setName(file.getOriginalFilename());

        profileImage.setImage(new Binary(BsonBinarySubType.BINARY, file.getBytes()));
        profileImage.setCreationDate(LocalDateTime.now());
        profileImage.setContentType(file.getContentType());
        profileImage.setSize(file.getSize());

        profileImage = profileImageRepository.insert(profileImage);
        return "http://" + DOMAIN + "/api/images/profile/download/" + profileImage.getId();
    }

    public Optional<ProfileImage> getProfileImage(String id){
        return profileImageRepository.findById(id);
    }

    public Boolean existsProfileImage(String id){
        return profileImageRepository.existsById(id);
    }
    public ProfileImage getProfileImageFull(String id){
        return mongoTemplate.findById(id, ProfileImage.class);
    }

    public Boolean deleteProfileImage(String id){
        if(!profileImageRepository.existsById(id))
            return false;

        profileImageRepository.deleteById(id);
        return true;
    }

}
