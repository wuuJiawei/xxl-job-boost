package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobAlarmRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobAlarmRecordMapper {

    List<XxlJobAlarmRecord> pageList(@Param("offset") int offset,
                                     @Param("pagesize") int pagesize,
                                     @Param("jobGroup") int jobGroup,
                                     @Param("channelType") String channelType,
                                     @Param("sendStatus") int sendStatus);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("jobGroup") int jobGroup,
                      @Param("channelType") String channelType,
                      @Param("sendStatus") int sendStatus);

    int save(XxlJobAlarmRecord record);
}
