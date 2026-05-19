package com.authguard.usersystem.mapper;

import com.authguard.usersystem.dto.SWishlistDetailDto;
import com.authguard.usersystem.entity.SWishlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SWishlistMapper {

    /**
     * 添加收藏
     */
    int insertWishlist(SWishlist wishlist);

    /**
     * 删除收藏
     */
    int deleteWishlist(@Param("userId") Long userId, @Param("uniformId") Long uniformId);

    /**
     * 查询某个用户的收藏列表
     */
    List<SWishlistDetailDto> selectWishlistWithDetailsByUserId(Long userId);

    /**
     * 判断是否已收藏
     */
    int countByUserAndUniform(@Param("userId") Long userId, @Param("uniformId") Long uniformId);

}
