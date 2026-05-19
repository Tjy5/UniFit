package com.authguard.usersystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class SizeFeedbackRequest {
    @Size(max = 32, message = "recommendedSize不能超过32个字符")
    private String recommendedSize;

    @Size(max = 32, message = "purchasedSize不能超过32个字符")
    private String purchasedSize;

    @NotBlank(message = "satisfaction不能为空")
    @Pattern(regexp = "FIT|TOO_LARGE|TOO_SMALL", message = "satisfaction只能为FIT、TOO_LARGE或TOO_SMALL")
    private String satisfaction;

    @Size(max = 10, message = "issueParts最多包含10项")
    private List<String> issueParts = new ArrayList<>();

    @Size(max = 500, message = "note不能超过500个字符")
    private String note;
}
