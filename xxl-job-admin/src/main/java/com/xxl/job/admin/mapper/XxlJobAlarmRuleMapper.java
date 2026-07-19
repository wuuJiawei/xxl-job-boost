package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobAlarmRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobAlarmRuleMapper {

    List<XxlJobAlarmRule> pageList(@Param("offset") int offset,
                                   @Param("pagesize") int pagesize,
                                   @Param("jobGroupIds") List<Integer> jobGroupIds,
                                   @Param("jobGroup") int jobGroup,
                                   @Param("jobId") int jobId,
                                   @Param("alarmEvent") String alarmEvent,
                                   @Param("enabled") int enabled);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("jobGroupIds") List<Integer> jobGroupIds,
                      @Param("jobGroup") int jobGroup,
                      @Param("jobId") int jobId,
                      @Param("alarmEvent") String alarmEvent,
                      @Param("enabled") int enabled);

    int save(XxlJobAlarmRule rule);

    int update(XxlJobAlarmRule rule);

    int remove(@Param("id") int id);

    XxlJobAlarmRule load(@Param("id") int id);

    List<XxlJobAlarmRule> findExecutorDefaults(@Param("jobGroup") int jobGroup);

    List<XxlJobAlarmRule> findEnabledExecutorDefaults(@Param("jobGroup") int jobGroup,
                                                       @Param("alarmEvent") String alarmEvent);

    int removeExecutorDefaults(@Param("jobGroup") int jobGroup);

    int removeByJobGroup(@Param("jobGroup") int jobGroup);
}
