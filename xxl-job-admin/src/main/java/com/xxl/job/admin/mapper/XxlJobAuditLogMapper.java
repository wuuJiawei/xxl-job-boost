package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobAuditLogMapper {

    int save(XxlJobAuditLog auditLog);

    List<XxlJobAuditLog> pageList(@Param("offset") int offset,
                                  @Param("pagesize") int pagesize,
                                  @Param("operator") String operator,
                                  @Param("actionType") String actionType,
                                  @Param("resourceType") String resourceType,
                                  @Param("jobGroup") int jobGroup);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("operator") String operator,
                      @Param("actionType") String actionType,
                      @Param("resourceType") String resourceType,
                      @Param("jobGroup") int jobGroup);

    List<XxlJobAuditLog> findRecent(@Param("pagesize") int pagesize);

    int countAll();

    XxlJobAuditLog findLatestByActionAndResource(@Param("actionType") String actionType,
                                                 @Param("resourceType") String resourceType,
                                                 @Param("resourceId") String resourceId,
                                                 @Param("operatorUserId") String operatorUserId);
}
