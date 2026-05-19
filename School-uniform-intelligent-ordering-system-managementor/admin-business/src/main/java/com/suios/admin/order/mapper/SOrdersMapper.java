package com.suios.admin.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.order.entity.SOrders;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SOrdersMapper extends BaseMapper<SOrders> {

    @Select("""
            <script>
            SELECT o.*,
                   ua.user_account AS user_account,
                   a.recipient_name AS recipient_name,
                   a.phone_number AS phone_number,
                   CONCAT_WS(' ', a.province, a.city, a.district, a.street_address) AS full_address
            FROM s_orders o
            LEFT JOIN user_account ua ON o.user_id = ua.user_id
            LEFT JOIN s_address a ON o.address_id = a.id
            <where>
                <if test="userId != null">
                    AND o.user_id = #{userId}
                </if>
                <if test="status != null">
                    AND o.status = #{status}
                </if>
                <if test="paymentStatus != null and paymentStatus != ''">
                    AND o.payment_status = #{paymentStatus}
                </if>
                <if test="shippingStatus != null and shippingStatus != ''">
                    AND o.shipping_status = #{shippingStatus}
                </if>
            </where>
            ORDER BY o.order_date DESC, o.id DESC
            </script>
            """)
    IPage<SOrders> selectPageWithDetails(Page<SOrders> page,
                                         @Param("userId") Long userId,
                                         @Param("status") Long status,
                                         @Param("paymentStatus") String paymentStatus,
                                         @Param("shippingStatus") String shippingStatus);

    @Select("""
            <script>
            SELECT o.*,
                   ua.user_account AS user_account,
                   a.recipient_name AS recipient_name,
                   a.phone_number AS phone_number,
                   CONCAT_WS(' ', a.province, a.city, a.district, a.street_address) AS full_address
            FROM s_orders o
            LEFT JOIN user_account ua ON o.user_id = ua.user_id
            LEFT JOIN s_address a ON o.address_id = a.id
            <where>
                <if test="userId != null">
                    AND o.user_id = #{userId}
                </if>
                <if test="status != null">
                    AND o.status = #{status}
                </if>
                <if test="paymentStatus != null and paymentStatus != ''">
                    AND o.payment_status = #{paymentStatus}
                </if>
                <if test="shippingStatus != null and shippingStatus != ''">
                    AND o.shipping_status = #{shippingStatus}
                </if>
            </where>
            ORDER BY o.order_date DESC, o.id DESC
            </script>
            """)
    List<SOrders> selectListWithDetails(@Param("userId") Long userId,
                                        @Param("status") Long status,
                                        @Param("paymentStatus") String paymentStatus,
                                        @Param("shippingStatus") String shippingStatus);

    @Select("""
            SELECT o.*,
                   ua.user_account AS user_account,
                   a.recipient_name AS recipient_name,
                   a.phone_number AS phone_number,
                   CONCAT_WS(' ', a.province, a.city, a.district, a.street_address) AS full_address
            FROM s_orders o
            LEFT JOIN user_account ua ON o.user_id = ua.user_id
            LEFT JOIN s_address a ON o.address_id = a.id
            WHERE o.id = #{id}
            """)
    SOrders selectDetailById(@Param("id") Long id);
}
