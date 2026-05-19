package com.suios.admin.upload.dto;

import lombok.Builder;

@Builder
public record UploadFileResponse(
        String url,
        String storedPath,
        String originalFilename
) {
}
