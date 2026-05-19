package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ISReviewMapper {

    int insertReview(SReview review);

    List<SReview> selectReviewsByUserId(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    long countReviewsByUserId(@Param("userId") Long userId);

    SReview selectReviewById(@Param("reviewId") Long reviewId);

    int updateReview(SReview review);

    int deleteReviewById(@Param("reviewId") Long reviewId);

    List<SReview> selectReviewsByUniformId(@Param("uniformId") Long uniformId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    long countReviewsByUniformId(@Param("uniformId") Long uniformId);

    int checkIfUserReviewedOrderItem(@Param("userId") Long userId, @Param("orderItemId") Long orderItemId);
    /**
     * 根据校服ID查询校服名称
     * @param uniformId 校服ID
     * @return 校服名称，如果不存在则返回null
     */
    String selectUniformNameById(@Param("uniformId") Long uniformId);
    // 如果你已经有一个根据ID查询整个SUniform对象的方法，也可以用那个
    // SUniform selectUniformById(@Param("uniformId") Long uniformId);
    // 更新校服评分和评论数的方法 (你已经有了)
    int updateUniformRatingAndCount(@Param("uniformId") Long uniformId,
                                    @Param("averageRating") double averageRating,
                                    @Param("reviewCount") int reviewCount);
}
