package com.pvelazquez.supermarketlistbackend.Controllers;

import com.pvelazquez.supermarketlistbackend.Models.ProfileImage;
import com.pvelazquez.supermarketlistbackend.Models.Response;
import com.pvelazquez.supermarketlistbackend.Models.User;
import com.pvelazquez.supermarketlistbackend.Services.ProfileImageService;
import com.pvelazquez.supermarketlistbackend.Services.UserService;
import com.pvelazquez.supermarketlistbackend.Utilities.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.*;
import static java.util.Map.*;

@RestController
@RequestMapping("/api/images/profile")
public class ProfileImageController {
    private final Utility utility = Utility.getInstance();

    @Autowired
    private ProfileImageService profileImageService;

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public ResponseEntity<Response> addProfileImage(@RequestParam("image") MultipartFile image, @RequestParam String email) throws Exception {
        User user = userService.getUser(email);

        System.out.println(user.getProfileImageURL()!= null);
        System.out.println(!user.getProfileImageURL().isEmpty());
        if(!user.getProfileImageURL().isEmpty()) {
            String imageId = user.getProfileImageURL().split("/")[7];
            user.setProfileImageURL("");
            profileImageService.deleteProfileImage(imageId);
        }

        user = userService.addProfileImageToUser(email, profileImageService.addProfileImage(image));
        return utility.createResponseEntity("profileImageURL", user.getProfileImageURL(), "User profile image assigned to " + user.getEmail(), OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getProfileImage(@PathVariable String id, Model model){
        ProfileImage profileImage = profileImageService.getProfileImage(id).isPresent() ? profileImageService.getProfileImage(id).get() : null;
        if(profileImage == null)
            return null;
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(now())
                        .data(of("image", Base64.getEncoder().encodeToString(profileImage.getImage().getData())))
                        .messange("Profile image retrieved")
                        .statusCode(OK.value())
                        .status(OK)
                        .build()
        );
    }

    @GetMapping(value = "/download/{id}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @ResponseBody
    public byte[] downloadProfileImage(@PathVariable String id){
        ProfileImage profileImage = profileImageService.existsProfileImage(id) ? profileImageService.getProfileImageFull(id) : null;

        if(profileImage == null)
            return null;

        return profileImage.getImage().getData();
    }
}
