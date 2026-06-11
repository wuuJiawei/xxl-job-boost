package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobAlarmChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobAlarmChannelMapper {

    List<XxlJobAlarmChannel> findAll();

    List<XxlJobAlarmChannel> findAllEnabled();

    List<XxlJobAlarmChannel> findByIds(@Param("ids") List<Integer> ids);

    List<XxlJobAlarmChannel> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("name") String name,
                                      @Param("type") String type,
                                      @Param("enabled") int enabled);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("name") String name,
                      @Param("type") String type,
                      @Param("enabled") int enabled);

    int save(XxlJobAlarmChannel channel);

    int update(XxlJobAlarmChannel channel);

    int remove(@Param("id") int id);

    XxlJobAlarmChannel load(@Param("id") int id);
}
