package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.XxlJobSystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XxlJobSystemConfigMapper {

    List<XxlJobSystemConfig> findByPrefix(@Param("prefix") String prefix);

    int upsert(XxlJobSystemConfig config);
}
