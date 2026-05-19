package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SUniform;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SUniformMapper {
    // 查询所有状态为0的校服
    List<SUniform> selectAllActiveUniforms();

    // 新增：根据 ID 查询单个校服的方法
    SUniform selectSUniformById(Long id);

    // 新增：根据 ID 列表查询多个校服的方法
    List<SUniform> selectSUniformByIds(@Param("ids") List<Long> ids);

    // ⭐ 新增方法: 更新校服的平均评分和评论总数
    int updateUniformRatingAndCount(@Param("uniformId") Long uniformId,
                                    @Param("averageRating") Double averageRating,
                                    @Param("reviewCount") Integer reviewCount);

    int updateUniformCategoryKey(@Param("uniformId") Long uniformId,
                                 @Param("categoryKey") String categoryKey);
}
