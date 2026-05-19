// src/main/java/com/authguard/usersystem/mapper/ISAddressMapper.java
package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ISAddressMapper {

    /**
     * 根据地址ID查询地址信息
     * @param id 地址ID
     * @return 地址实体，如果找不到返回 null
     */
    SAddress selectById(Long id);

    /**
     * 根据用户ID查询该用户的所有地址
     * @param userId 用户ID
     * @return 地址列表，按是否默认降序、ID降序排列
     */
    List<SAddress> selectByUserId(Long userId);

    /**
     * 插入新的地址信息
     * @param address 要插入的地址对象 (ID应为null，插入后会被MyBatis填充)
     * @return 影响的行数 (通常是1)
     */
    int insertAddress(SAddress address);

    /**
     * 更新地址信息 (不允许修改 user_id)
     * @param address 包含要更新的字段和地址ID的地址对象
     * @return 影响的行数
     */
    int updateAddress(SAddress address);

    /**
     * 根据地址ID和用户ID删除地址 (确保用户只能删自己的地址)
     * @param id 地址ID
     * @param userId 用户ID
     * @return 影响的行数
     */
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 将指定用户的所有地址设置为非默认
     * @param userId 用户ID
     * @return 影响的行数
     */
    int unsetDefaultForUser(Long userId);

    /**
     * 根据地址ID和用户ID设置地址的默认状态 (防止用户设置别人的地址为默认)
     * @param id 地址ID
     * @param userId 用户ID
     * @param isDefault true 设置为默认, false 设置为非默认
     * @return 影响的行数
     */
    int updateDefaultStatusByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("isDefault") boolean isDefault);

}
