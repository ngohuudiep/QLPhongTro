package com.qlphongtro.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RoomImageStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_COUNT = 5;
    private static final long MAX_PIXEL_COUNT = 20_000_000L;
    private static final String URL_PREFIX = "/room-images/";

    private final Path storageDirectory;

    public RoomImageStorageService(@Value("${app.upload.room-images-dir:${user.home}/qlphongtro-data/room-images}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public List<String> store(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<MultipartFile> selected = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (selected.size() > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("Chỉ được tải tối đa 5 ảnh mỗi lần.");
        }

        List<Path> createdFiles = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        try {
            Files.createDirectories(storageDirectory);
            for (MultipartFile file : selected) {
                byte[] content = file.getBytes();
                String extension = validatedExtension(file, content);
                Path destination = storageDirectory.resolve(UUID.randomUUID() + extension).normalize();
                if (!destination.startsWith(storageDirectory)) {
                    throw new IllegalStateException("Đường dẫn lưu ảnh không hợp lệ.");
                }
                Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                createdFiles.add(destination);
                urls.add(URL_PREFIX + destination.getFileName());
            }
            return urls;
        } catch (IOException | RuntimeException exception) {
            for (Path createdFile : createdFiles) {
                try {
                    Files.deleteIfExists(createdFile);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("Không thể lưu ảnh phòng trọ.", exception);
        }
    }

    public void deleteManagedFile(String imageUrl) {
        Path imagePath = managedPath(imageUrl);
        if (imagePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xóa ảnh phòng trọ: " + imageUrl, exception);
        }
    }

    public boolean isManagedUrl(String imageUrl) {
        return managedPath(imageUrl) != null;
    }

    private String validatedExtension(MultipartFile file, byte[] content) {
        if (content.length == 0 || content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Mỗi ảnh phải có dung lượng tối đa 5 MB.");
        }
        String originalName = file.getOriginalFilename();
        String extension = originalName == null || originalName.lastIndexOf('.') < 0
                ? "" : originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String detectedExtension;
        if (startsWith(content, new int[]{0xFF, 0xD8, 0xFF})) {
            detectedExtension = ".jpg";
            validateDecodableImage(content, "JPEG");
        } else if (startsWith(content, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            detectedExtension = ".png";
            validateDecodableImage(content, "PNG");
        } else if (isWebp(content)) {
            detectedExtension = ".webp";
        } else {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, JPEG, PNG hoặc WEBP hợp lệ.");
        }
        boolean matches = extension.equals(detectedExtension)
                || (detectedExtension.equals(".jpg") && extension.equals(".jpeg"));
        if (!matches) {
            throw new IllegalArgumentException("Phần mở rộng tệp không khớp với định dạng ảnh.");
        }
        return detectedExtension;
    }

    private static boolean isWebp(byte[] content) {
        if (content.length < 20
                || content[0] != 'R' || content[1] != 'I' || content[2] != 'F' || content[3] != 'F'
                || content[8] != 'W' || content[9] != 'E' || content[10] != 'B' || content[11] != 'P') {
            return false;
        }
        long riffSize = littleEndianUnsignedInt(content, 4);
        long chunkSize = littleEndianUnsignedInt(content, 16);
        boolean validChunk = content[12] == 'V' && content[13] == 'P' && content[14] == '8'
                && (content[15] == ' ' || content[15] == 'L' || content[15] == 'X');
        return riffSize == content.length - 8L
                && validChunk
                && chunkSize > 0
                && chunkSize <= content.length - 20L;
    }

    private static long littleEndianUnsignedInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFFL)
                | ((bytes[offset + 1] & 0xFFL) << 8)
                | ((bytes[offset + 2] & 0xFFL) << 16)
                | ((bytes[offset + 3] & 0xFFL) << 24);
    }

    private void validateDecodableImage(byte[] content, String format) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Tệp ảnh " + format + " không đọc được.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_PIXEL_COUNT) {
                    throw new IllegalArgumentException("Kích thước ảnh vượt quá giới hạn cho phép.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Tệp ảnh " + format + " không đọc được.", exception);
        }
    }

    private static boolean startsWith(byte[] bytes, int[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private Path managedPath(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return null;
        }
        String fileName = imageUrl.substring(URL_PREFIX.length());
        if (!fileName.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)")) {
            return null;
        }
        Path imagePath = storageDirectory.resolve(fileName).normalize();
        return imagePath.startsWith(storageDirectory) ? imagePath : null;
    }
}
