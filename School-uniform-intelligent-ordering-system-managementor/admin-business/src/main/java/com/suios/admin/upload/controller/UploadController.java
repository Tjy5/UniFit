package com.suios.admin.upload.controller;

import com.suios.admin.common.result.R;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.upload.dto.UploadFileResponse;
import com.suios.admin.upload.service.FileStorageService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping
    @OperLog(module = "文件上传", operation = "UPLOAD")
    public R<UploadFileResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return R.success(fileStorageService.store(file));
    }
}
