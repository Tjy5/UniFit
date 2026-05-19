// src/main/java/com/authguard/usersystem/service/ISAddressService.java
package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.SAddress;
// Import DTO if you create one for create/update
// import com.authguard.usersystem.dto.AddressDto;

import java.util.List;

public interface ISAddressService {

    /**
     * 获取指定用户的所有收货地址
     * @param userId 用户ID
     * @return 地址列表
     */
    List<SAddress> listAddressesByUserId(Long userId);

    /**
     * 获取指定ID的地址详情，并校验是否属于该用户
     * @param addressId 地址ID
     * @param userId 用户ID (用于校验)
     * @return 地址实体
     * @throws SecurityException 如果地址不属于该用户
     * @throws IllegalArgumentException 如果地址不存在
     */
    SAddress getAddressById(Long addressId, Long userId);

    /**
     * 为指定用户创建新地址
     * @param address 要创建的地址对象 (userId 应该由 Service 设置, id 应为 null)
     * @param userId 当前登录用户的ID
     * @return 创建成功后的地址对象 (包含生成的ID)
     */
    SAddress createAddress(SAddress address, Long userId);
    // Alternative using DTO:
    // SAddress createAddress(AddressDto addressDto, Long userId);

    /**
     * 更新指定地址的信息，并校验所有权
     * @param addressId 要更新的地址ID
     * @param address   包含更新信息的地址对象 (忽略 id 和 userId)
     * @param userId    当前登录用户的ID (用于校验)
     * @return 更新后的地址对象
     * @throws SecurityException 如果地址不属于该用户
     * @throws IllegalArgumentException 如果地址不存在
     */
    SAddress updateAddress(Long addressId, SAddress address, Long userId);
    // Alternative using DTO:
    // SAddress updateAddress(Long addressId, AddressDto addressDto, Long userId);


    /**
     * 删除指定地址，并校验所有权
     * @param addressId 要删除的地址ID
     * @param userId    当前登录用户的ID (用于校验)
     * @return true 如果删除成功, false 如果未找到或无权限
     */
    boolean deleteAddress(Long addressId, Long userId);

    /**
     * 设置指定地址为用户的默认地址
     * @param addressId 要设为默认的地址ID
     * @param userId    当前登录用户的ID (用于校验)
     * @return true 如果设置成功, false 如果失败
     * @throws SecurityException 如果地址不属于该用户
     * @throws IllegalArgumentException 如果地址不存在
     */
    boolean setDefaultAddress(Long addressId, Long userId);
}
