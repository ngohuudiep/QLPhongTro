package com.qlphongtro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class RoomImageResourceConfig implements WebMvcConfigurer {

    private final String resourceLocation;

    public RoomImageResourceConfig(@Value("${app.upload.room-images-dir:${user.home}/qlphongtro-data/room-images}") String storageDirectory) {
        Path directory = Path.of(storageDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Không thể khởi tạo thư mục lưu ảnh phòng trọ.", exception);
        }
        String location = directory.toUri().toString();
        this.resourceLocation = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/room-images/**").addResourceLocations(resourceLocation);
    }
}
