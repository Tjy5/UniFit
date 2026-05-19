package com.suios.admin.upload.service;

import com.suios.admin.upload.config.UploadProperties;
import com.suios.admin.upload.dto.UploadFileResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final UploadProperties uploadProperties;

    public FileStorageService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public UploadFileResponse store(MultipartFile file) throws IOException {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        String extension = getExtension(originalFilename);
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String storedPath = datePath + "/" + UUID.randomUUID() + extension;
        Path destination = Path.of(uploadProperties.getPath(), storedPath);

        Files.createDirectories(destination.getParent());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return UploadFileResponse.builder()
                .url(uploadProperties.getUrlPrefix() + "/" + storedPath.replace('\\', '/'))
                .storedPath(storedPath.replace('\\', '/'))
                .originalFilename(originalFilename)
                .build();
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
