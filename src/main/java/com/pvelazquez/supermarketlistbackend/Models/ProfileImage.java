package com.pvelazquez.supermarketlistbackend.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.Binary;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Document(collection = "profileImages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImage {
    @Id
    private String id;
    private String name;
    private LocalDateTime creationDate;
    private Binary image;
    private String contentType;
    private long size;
}
