package com.authguard.usersystem.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SizeRecommendationRequest {
    @Min(value = 3, message = "年龄不能小于3")
    @Max(value = 30, message = "年龄不能大于30")
    private Integer messageUserAge;

    @Pattern(regexp = "^(男|女|MALE|FEMALE|M|F)?$", message = "性别取值不合法")
    private String messageUserSex;

    private Long uniformId;

    @DecimalMin(value = "50.0", message = "height不能小于50")
    @DecimalMax(value = "250.0", message = "height不能大于250")
    private BigDecimal height;

    @DecimalMin(value = "10.0", message = "weight不能小于10")
    @DecimalMax(value = "200.0", message = "weight不能大于200")
    private BigDecimal weight;

    @DecimalMin(value = "20.0", message = "chest不能小于20")
    @DecimalMax(value = "200.0", message = "chest不能大于200")
    private BigDecimal chest;

    @DecimalMin(value = "20.0", message = "waist不能小于20")
    @DecimalMax(value = "200.0", message = "waist不能大于200")
    private BigDecimal waist;

    @DecimalMin(value = "20.0", message = "hip不能小于20")
    @DecimalMax(value = "200.0", message = "hip不能大于200")
    private BigDecimal hip;

    @DecimalMin(value = "10.0", message = "shoulder不能小于10")
    @DecimalMax(value = "100.0", message = "shoulder不能大于100")
    private BigDecimal shoulder;

    @Size(max = 32, message = "source不能超过32个字符")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "source只能包含字母、数字、下划线或中划线")
    private String source;
}
