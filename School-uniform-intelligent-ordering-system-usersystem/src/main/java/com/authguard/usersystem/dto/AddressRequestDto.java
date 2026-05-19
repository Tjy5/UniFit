package com.authguard.usersystem.dto;

import com.authguard.usersystem.entity.SAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequestDto {
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 64, message = "收货人姓名不能超过64个字符")
    private String recipientName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^[0-9+\\- ]{6,32}$", message = "手机号格式不合法")
    private String phoneNumber;

    @NotBlank(message = "省份不能为空")
    @Size(max = 64, message = "省份不能超过64个字符")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 64, message = "城市不能超过64个字符")
    private String city;

    @NotBlank(message = "区县不能为空")
    @Size(max = 64, message = "区县不能超过64个字符")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址不能超过200个字符")
    private String streetAddress;

    private Boolean isDefault;

    public SAddress toEntity() {
        SAddress address = new SAddress();
        address.setRecipientName(recipientName);
        address.setPhoneNumber(phoneNumber);
        address.setProvince(province);
        address.setCity(city);
        address.setDistrict(district);
        address.setStreetAddress(streetAddress);
        address.setIsDefault(isDefault);
        return address;
    }
}
