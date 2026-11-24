package com.qlphongtro;

import com.qlphongtro.service.RoomImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomImageStorageServiceTest {

    @TempDir
    Path tempDirectory;

    private RoomImageStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new RoomImageStorageService(tempDirectory.toString());
    }

    @Test
    void storesValidPngUsingGeneratedNameOutsideOriginalFilename() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "images", "room.png", "image/png", createPng());

        List<String> urls = storageService.store(List.of(image));

        assertTrue(urls.get(0).startsWith("/room-images/"));
        assertFalse(urls.get(0).contains("room.png"));
        assertTrue(Files.exists(tempDirectory.resolve(urls.get(0).substring("/room-images/".length()))));
    }

    @Test
    void rejectsContentThatDoesNotMatchImageSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "images", "payload.png", "image/png", "not an image".getBytes());

        assertThrows(IllegalArgumentException.class, () -> storageService.store(List.of(file)));
    }

    @Test
    void refusesToDeletePathsOutsideManagedImageDirectory() throws Exception {
        Path outsideFile = tempDirectory.resolveSibling("room-image-outside.txt");
        Files.writeString(outsideFile, "keep");

        storageService.deleteManagedFile("/room-images/../room-image-outside.txt");

        assertTrue(Files.exists(outsideFile));
        Files.delete(outsideFile);
    }

    private static byte[] createPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
