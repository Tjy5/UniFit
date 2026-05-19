package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.ShoppingCartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ShoppingCartItemMapper {
    int insert(ShoppingCartItem item);

    ShoppingCartItem selectByUserIdAndUniformIdAndSizeId(@Param("userId") Long userId, @Param("uniformId") Long uniformId, @Param("sizeId") Long sizeId);

    List<ShoppingCartItem> selectByUserId(Long userId);

    int updateQuantity(ShoppingCartItem item);

    int deleteById(Long cartItemId);

    int deleteByUserId(Long userId);

    int deleteByUserIdAndCartItemId(@Param("userId") Long userId, @Param("cartItemId") Long cartItemId);
}
