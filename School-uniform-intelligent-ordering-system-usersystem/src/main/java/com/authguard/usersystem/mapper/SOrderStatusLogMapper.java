package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SOrderStatusLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SOrderStatusLogMapper {

    int insert(SOrderStatusLog log);

    List<SOrderStatusLog> selectByOrderId(@Param("orderId") Long orderId);

    SOrderStatusLog selectLatestByOrderIdAndEventType(@Param("orderId") Long orderId,
                                                      @Param("eventType") String eventType);

    int countByOrderId(@Param("orderId") Long orderId);
}
