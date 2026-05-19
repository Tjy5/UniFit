package com.authguard.usersystem.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SizePreferenceRequest {
    private Long uniformId;

    @Size(max = 64, message = "categoryKey不能超过64个字符")
    private String categoryKey;

    @Pattern(regexp = "LOOSE|STANDARD|SLIM", message = "fitPreference只能为LOOSE、STANDARD或SLIM")
    private String fitPreference;
}
