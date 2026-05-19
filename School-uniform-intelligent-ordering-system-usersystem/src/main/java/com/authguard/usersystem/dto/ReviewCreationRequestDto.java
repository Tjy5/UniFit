package com.authguard.usersystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewCreationRequestDto {

    @NotNull(message = "校服ID不能为空")
    private Long uniformId;

    @NotNull(message = "订单项ID不能为空")
    private Long orderItemId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Byte rating;

    @Size(max = 1000, message = "评论内容不能超过1000字符")
    private String content;

    private Boolean isAnonymous = false; // 默认为非匿名
}
