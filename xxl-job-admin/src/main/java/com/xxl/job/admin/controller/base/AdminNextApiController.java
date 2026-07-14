package com.xxl.job.admin.controller.base;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.core.alarm.AlarmChannelType;
import com.xxl.job.admin.core.alarm.AlarmEventType;
import com.xxl.job.admin.mapper.XxlJobAlarmChannelMapper;
import com.xxl.job.admin.mapper.XxlJobAlarmRecordMapper;
import com.xxl.job.admin.mapper.XxlJobAlarmRuleMapper;
import com.xxl.job.admin.mapper.XxlJobAuditLogMapper;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobLogGlueMapper;
import com.xxl.job.admin.mapper.XxlJobLogMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobAlarmChannel;
import com.xxl.job.admin.model.XxlJobAlarmRecord;
import com.xxl.job.admin.model.XxlJobAlarmRule;
import com.xxl.job.admin.model.XxlJobAuditLog;
import com.xxl.job.admin.model.GovernanceOverview;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.JobFailureAggregate;
import com.xxl.job.admin.model.JobSlowAggregate;
import com.xxl.job.admin.model.XxlJobLogGlue;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.scheduler.misfire.MisfireStrategyEnum;
import com.xxl.job.admin.scheduler.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.scheduler.type.ScheduleTypeEnum;
import com.xxl.job.admin.service.AuditLogService;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.admin.util.I18nUtil;
import com.xxl.job.admin.util.JobGroupPermissionUtil;
import com.xxl.job.core.constant.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin-next")
public class AdminNextApiController {
    private static final Logger logger = LoggerFactory.getLogger(AdminNextApiController.class);

    @Resource
    private XxlJobService xxlJobService;
    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;
    @Resource
    private XxlJobLogMapper xxlJobLogMapper;
    @Resource
    private XxlJobUserMapper xxlJobUserMapper;
    @Resource
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;
    @Resource
    private XxlJobAlarmChannelMapper xxlJobAlarmChannelMapper;
    @Resource
    private XxlJobAlarmRecordMapper xxlJobAlarmRecordMapper;
    @Resource
    private XxlJobAlarmRuleMapper xxlJobAlarmRuleMapper;
    @Resource
    private XxlJobAuditLogMapper xxlJobAuditLogMapper;
    @Resource
    private AuditLogService auditLogService;

    @GetMapping("/session")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> session(HttpServletRequest request) {
        LoginInfo loginInfo = XxlSsoHelper.loginCheckWithAttr(request).getData();

        Map<String, Object> data = new HashMap<>();
        data.put("userId", loginInfo.getUserId());
        data.put("userName", loginInfo.getUserName());
        data.put("permissionSet", loginInfo.getPermissionList());
        data.put("isAdmin", XxlSsoHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess());
        return Response.ofSuccess(data);
    }

    @GetMapping("/dashboard/summary")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> dashboardSummary() {
        return Response.ofSuccess(xxlJobService.dashboardInfo());
    }

    @GetMapping("/governance/overview")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<GovernanceOverview> governanceOverview(HttpServletRequest request) {
        List<XxlJobGroup> permittedGroups = JobGroupPermissionUtil.filterJobGroupByPermission(request, xxlJobGroupMapper.findAll());
        List<Integer> permittedGroupIds = permittedGroups.stream().map(XxlJobGroup::getId).toList();

        GovernanceOverview data = new GovernanceOverview();
        data.setTotalJobs(xxlJobInfoMapper.findAllCount());
        data.setOwnedJobs(xxlJobInfoMapper.countWithAuthor());
        data.setTaggedJobs(xxlJobInfoMapper.countWithJobTag());
        data.setAuditEvents(xxlJobAuditLogMapper.countAll());

        if (permittedGroupIds.isEmpty()) {
            data.setFailureTopList(new ArrayList<>());
            data.setSlowTopList(new ArrayList<>());
        } else {
            data.setFailureTopList(xxlJobLogMapper.pageFailureAggregates(
                    0,
                    5,
                    permittedGroupIds,
                    -1,
                    0,
                    "",
                    "",
                    null,
                    null
            ));
            data.setSlowTopList(xxlJobLogMapper.pageSlowAggregates(
                    0,
                    5,
                    permittedGroupIds,
                    -1,
                    0,
                    "",
                    "",
                    30,
                    null,
                    null
            ));
        }

        data.setRecentAuditList(xxlJobAuditLogMapper.findRecent(8));
        return Response.ofSuccess(data);
    }

    @GetMapping("/jobgroups")
    @ResponseBody
    @XxlSso
    public Response<Object> jobGroups(HttpServletRequest request) {
        List<XxlJobGroup> groups = JobGroupPermissionUtil.filterJobGroupByPermission(
                request,
                xxlJobGroupMapper.findAll()
        );
        return Response.ofSuccess(groups);
    }

    @GetMapping("/jobgroups/{jobGroupId}/jobs")
    @ResponseBody
    @XxlSso
    public Response<Object> jobsByGroup(HttpServletRequest request, @PathVariable int jobGroupId) {
        JobGroupPermissionUtil.validJobGroupPermission(request, jobGroupId);
        return Response.ofSuccess(xxlJobInfoMapper.getJobsByGroup(jobGroupId));
    }

    @GetMapping("/job-metadata")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> jobMetadata() {
        Map<String, Object> data = new HashMap<>();
        data.put("scheduleTypes", toOptions(ScheduleTypeEnum.values(), ScheduleTypeEnum::name, ScheduleTypeEnum::getTitle));
        data.put("routeStrategies", toOptions(ExecutorRouteStrategyEnum.values(), ExecutorRouteStrategyEnum::name, ExecutorRouteStrategyEnum::getTitle));
        data.put("misfireStrategies", toOptions(MisfireStrategyEnum.values(), MisfireStrategyEnum::name, MisfireStrategyEnum::getTitle));
        data.put("blockStrategies", toOptions(ExecutorBlockStrategyEnum.values(), ExecutorBlockStrategyEnum::name, ExecutorBlockStrategyEnum::getTitle));
        data.put("glueTypes", toOptions(GlueTypeEnum.values(), GlueTypeEnum::name, GlueTypeEnum::getDesc));
        data.put("alarmChannelTypes", toOptions(AlarmChannelType.values(), AlarmChannelType::name, AlarmChannelType::name));
        data.put("alarmEventTypes", toOptions(AlarmEventType.values(), AlarmEventType::name, AlarmEventType::getTitle));
        data.put("alarmChannels", xxlJobAlarmChannelMapper.findAllEnabled());
        return Response.ofSuccess(data);
    }

    @GetMapping("/jobs/{jobId}")
    @ResponseBody
    @XxlSso
    public Response<Object> jobDetail(HttpServletRequest request, @PathVariable int jobId) {
        XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(jobId);
        if (jobInfo == null) {
            return Response.ofFail("任务不存在");
        }
        JobGroupPermissionUtil.validJobGroupPermission(request, jobInfo.getJobGroup());
        return Response.ofSuccess(jobInfo);
    }

    @GetMapping("/jobs/{jobId}/code")
    @ResponseBody
    @XxlSso
    public Response<Object> jobCodeDetail(HttpServletRequest request, @PathVariable int jobId) {
        XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(jobId);
        if (jobInfo == null) {
            return Response.ofFail(I18nUtil.getString("jobinfo_glue_jobid_invalid"));
        }

        JobGroupPermissionUtil.validJobGroupPermission(request, jobInfo.getJobGroup());

        GlueTypeEnum glueType = GlueTypeEnum.match(jobInfo.getGlueType());
        if (GlueTypeEnum.BEAN == glueType) {
            return Response.ofFail(I18nUtil.getString("jobinfo_glue_gluetype_invalid"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("jobId", jobInfo.getId());
        data.put("jobDesc", jobInfo.getJobDesc());
        data.put("glueType", jobInfo.getGlueType());
        data.put("glueTypeLabel", glueType.getDesc());
        data.put("currentVersion", toGlueVersion(
                jobInfo.getId(),
                jobInfo.getGlueType(),
                jobInfo.getGlueRemark(),
                jobInfo.getGlueSource(),
                jobInfo.getGlueUpdatetime()
        ));
        data.put("historyVersions", xxlJobLogGlueMapper.findByJobId(jobInfo.getId()).stream()
                .map(this::toGlueVersion)
                .toList());
        return Response.ofSuccess(data);
    }

    @PostMapping("/jobs/{jobId}/code")
    @ResponseBody
    @XxlSso
    public Response<String> saveJobCode(HttpServletRequest request,
                                        @PathVariable int jobId,
                                        @RequestParam("glueSource") String glueSource,
                                        @RequestParam("glueRemark") String glueRemark) {

        if (StringTool.isBlank(glueSource)) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_source"));
        }
        if (glueRemark == null) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark"));
        }
        if (glueRemark.length() < 4 || glueRemark.length() > 100) {
            return Response.ofFail(I18nUtil.getString("jobinfo_glue_remark_limit"));
        }

        XxlJobInfo existsJobInfo = xxlJobInfoMapper.loadById(jobId);
        if (existsJobInfo == null) {
            return Response.ofFail(I18nUtil.getString("jobinfo_glue_jobid_invalid"));
        }

        GlueTypeEnum glueType = GlueTypeEnum.match(existsJobInfo.getGlueType());
        if (GlueTypeEnum.BEAN == glueType) {
            return Response.ofFail(I18nUtil.getString("jobinfo_glue_gluetype_invalid"));
        }

        LoginInfo loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, existsJobInfo.getJobGroup());

        existsJobInfo.setGlueSource(glueSource);
        existsJobInfo.setGlueRemark(glueRemark);
        existsJobInfo.setGlueUpdatetime(new Date());
        existsJobInfo.setUpdateTime(new Date());
        xxlJobInfoMapper.update(existsJobInfo);

        XxlJobLogGlue xxlJobLogGlue = new XxlJobLogGlue();
        xxlJobLogGlue.setJobId(existsJobInfo.getId());
        xxlJobLogGlue.setGlueType(existsJobInfo.getGlueType());
        xxlJobLogGlue.setGlueSource(glueSource);
        xxlJobLogGlue.setGlueRemark(glueRemark);
        xxlJobLogGlue.setAddTime(new Date());
        xxlJobLogGlue.setUpdateTime(new Date());
        xxlJobLogGlueMapper.save(xxlJobLogGlue);
        xxlJobLogGlueMapper.removeOld(existsJobInfo.getId(), 30);

        logger.info(">>>>>>>>>>> xxl-job operation log: operator = {}, type = {}, content = {}",
                loginInfo.getUserName(), "jobcode-update", GsonTool.toJson(xxlJobLogGlue));
        auditLogService.record(loginInfo, request, "jobcode-update", "job-code", String.valueOf(existsJobInfo.getId()), existsJobInfo.getJobDesc(), existsJobInfo.getJobGroup(), xxlJobLogGlue);
        return Response.ofSuccess();
    }

    @GetMapping("/logs/{logId}")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> logDetail(HttpServletRequest request, @PathVariable long logId) {
        XxlJobLog jobLog = xxlJobLogMapper.load(logId);
        if (jobLog == null) {
            return Response.ofFail("日志不存在");
        }

        JobGroupPermissionUtil.validJobGroupPermission(request, jobLog.getJobGroup());

        XxlJobInfo jobInfo = xxlJobInfoMapper.loadById(jobLog.getJobId());
        Map<String, Object> data = new HashMap<>();
        data.put("id", jobLog.getId());
        data.put("jobId", jobLog.getJobId());
        data.put("jobGroup", jobLog.getJobGroup());
        data.put("jobDesc", jobInfo != null ? jobInfo.getJobDesc() : "");
        data.put("triggerCode", jobLog.getTriggerCode());
        data.put("handleCode", jobLog.getHandleCode());
        data.put("triggerTime", jobLog.getTriggerTime());
        data.put("handleTime", jobLog.getHandleTime());
        data.put("executorAddress", jobLog.getExecutorAddress());
        return Response.ofSuccess(data);
    }

    @GetMapping("/user-metadata")
    @ResponseBody
    @XxlSso(role = com.xxl.job.admin.constant.Consts.ADMIN_ROLE)
    public Response<Map<String, Object>> userMetadata() {
        Map<String, Object> data = new HashMap<>();
        data.put("groups", xxlJobGroupMapper.findAll());

        List<Map<String, Object>> roleOptions = new java.util.ArrayList<>();
        roleOptions.add(option(0, "普通用户"));
        roleOptions.add(option(1, "管理员"));
        data.put("roles", roleOptions);
        return Response.ofSuccess(data);
    }

    @GetMapping("/users/{userId}")
    @ResponseBody
    @XxlSso(role = com.xxl.job.admin.constant.Consts.ADMIN_ROLE)
    public Response<Object> userDetail(@PathVariable int userId) {
        XxlJobUser user = xxlJobUserMapper.loadById(userId);
        if (user == null) {
            return Response.ofFail("用户不存在");
        }
        user.setPassword(null);
        return Response.ofSuccess(user);
    }

    @GetMapping("/alarm-channels/{channelId}")
    @ResponseBody
    @XxlSso(role = com.xxl.job.admin.constant.Consts.ADMIN_ROLE)
    public Response<Object> alarmChannelDetail(@PathVariable int channelId) {
        XxlJobAlarmChannel channel = xxlJobAlarmChannelMapper.load(channelId);
        if (channel == null) {
            return Response.ofFail("告警渠道不存在");
        }
        return Response.ofSuccess(channel);
    }

    @GetMapping("/alarm-rules/{ruleId}")
    @ResponseBody
    @XxlSso
    public Response<Object> alarmRuleDetail(HttpServletRequest request, @PathVariable int ruleId) {
        XxlJobAlarmRule rule = xxlJobAlarmRuleMapper.load(ruleId);
        if (rule == null) {
            return Response.ofFail("告警规则不存在");
        }
        JobGroupPermissionUtil.validJobGroupPermission(request, rule.getJobGroup());
        return Response.ofSuccess(rule);
    }

    @GetMapping("/alarm-rules")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> alarmRules(HttpServletRequest request,
                                                    @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
                                                    @RequestParam(value = "pagesize", required = false, defaultValue = "10") int pagesize,
                                                    @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup,
                                                    @RequestParam(value = "jobId", required = false, defaultValue = "0") int jobId,
                                                    @RequestParam(value = "alarmEvent", required = false, defaultValue = "") String alarmEvent,
                                                    @RequestParam(value = "enabled", required = false, defaultValue = "-1") int enabled) {
        if (jobGroup > 0) {
            JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup);
        }
        List<XxlJobAlarmRule> list = xxlJobAlarmRuleMapper.pageList(offset, pagesize, jobGroup, jobId, alarmEvent, enabled);
        int count = xxlJobAlarmRuleMapper.pageListCount(offset, pagesize, jobGroup, jobId, alarmEvent, enabled);
        Map<String, Object> data = new HashMap<>();
        data.put("data", list);
        data.put("total", count);
        return Response.ofSuccess(data);
    }

    @GetMapping("/alarm-records")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> alarmRecords(HttpServletRequest request,
                                                      @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
                                                      @RequestParam(value = "pagesize", required = false, defaultValue = "10") int pagesize,
                                                      @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup,
                                                      @RequestParam(value = "channelType", required = false, defaultValue = "") String channelType,
                                                      @RequestParam(value = "sendStatus", required = false, defaultValue = "-1") int sendStatus) {
        if (jobGroup > 0) {
            JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup);
        }
        List<XxlJobAlarmRecord> list = xxlJobAlarmRecordMapper.pageList(offset, pagesize, jobGroup, channelType, sendStatus);
        int count = xxlJobAlarmRecordMapper.pageListCount(offset, pagesize, jobGroup, channelType, sendStatus);
        Map<String, Object> data = new HashMap<>();
        data.put("data", list);
        data.put("total", count);
        return Response.ofSuccess(data);
    }

    @GetMapping("/failure-aggregates")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> failureAggregates(HttpServletRequest request,
                                                           @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
                                                           @RequestParam(value = "pagesize", required = false, defaultValue = "10") int pagesize,
                                                           @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup,
                                                           @RequestParam(value = "jobId", required = false, defaultValue = "0") int jobId,
                                                           @RequestParam(value = "author", required = false, defaultValue = "") String author,
                                                           @RequestParam(value = "jobTag", required = false, defaultValue = "") String jobTag,
                                                           @RequestParam(value = "filterTime", required = false, defaultValue = "") String filterTime) {
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (StringTool.isNotBlank(filterTime)) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = com.xxl.tool.core.DateTool.parseDateTime(temp[0]);
                triggerTimeEnd = com.xxl.tool.core.DateTool.parseDateTime(temp[1]);
            }
        }

        List<XxlJobGroup> permittedGroups = JobGroupPermissionUtil.filterJobGroupByPermission(request, xxlJobGroupMapper.findAll());
        if (jobGroup > 0) {
            JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup);
        }
        List<Integer> permittedGroupIds = permittedGroups.stream().map(XxlJobGroup::getId).toList();
        if (permittedGroupIds.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("data", new ArrayList<>());
            empty.put("total", 0);
            return Response.ofSuccess(empty);
        }

        List<JobFailureAggregate> list = xxlJobLogMapper.pageFailureAggregates(
                offset,
                pagesize,
                permittedGroupIds,
                jobGroup,
                jobId,
                author,
                jobTag,
                triggerTimeStart,
                triggerTimeEnd
        );
        int count = xxlJobLogMapper.pageFailureAggregatesCount(
                permittedGroupIds,
                jobGroup,
                jobId,
                author,
                jobTag,
                triggerTimeStart,
                triggerTimeEnd
        );

        Map<String, Object> data = new HashMap<>();
        data.put("data", list);
        data.put("total", count);
        return Response.ofSuccess(data);
    }

    @GetMapping("/slow-tasks")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> slowTasks(HttpServletRequest request,
                                                   @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
                                                   @RequestParam(value = "pagesize", required = false, defaultValue = "10") int pagesize,
                                                   @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup,
                                                   @RequestParam(value = "jobId", required = false, defaultValue = "0") int jobId,
                                                   @RequestParam(value = "author", required = false, defaultValue = "") String author,
                                                   @RequestParam(value = "jobTag", required = false, defaultValue = "") String jobTag,
                                                   @RequestParam(value = "minDurationSeconds", required = false, defaultValue = "30") int minDurationSeconds,
                                                   @RequestParam(value = "filterTime", required = false, defaultValue = "") String filterTime) {
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (StringTool.isNotBlank(filterTime)) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = com.xxl.tool.core.DateTool.parseDateTime(temp[0]);
                triggerTimeEnd = com.xxl.tool.core.DateTool.parseDateTime(temp[1]);
            }
        }

        List<XxlJobGroup> permittedGroups = JobGroupPermissionUtil.filterJobGroupByPermission(request, xxlJobGroupMapper.findAll());
        if (jobGroup > 0) {
            JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup);
        }
        List<Integer> permittedGroupIds = permittedGroups.stream().map(XxlJobGroup::getId).toList();
        if (permittedGroupIds.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("data", new ArrayList<>());
            empty.put("total", 0);
            return Response.ofSuccess(empty);
        }

        int slowThreshold = Math.max(minDurationSeconds, 1);
        List<JobSlowAggregate> list = xxlJobLogMapper.pageSlowAggregates(
                offset,
                pagesize,
                permittedGroupIds,
                jobGroup,
                jobId,
                author,
                jobTag,
                slowThreshold,
                triggerTimeStart,
                triggerTimeEnd
        );
        int count = xxlJobLogMapper.pageSlowAggregatesCount(
                permittedGroupIds,
                jobGroup,
                jobId,
                author,
                jobTag,
                slowThreshold,
                triggerTimeStart,
                triggerTimeEnd
        );

        Map<String, Object> data = new HashMap<>();
        data.put("data", list);
        data.put("total", count);
        return Response.ofSuccess(data);
    }

    @GetMapping("/audit-logs")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<Map<String, Object>> auditLogs(@RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
                                                   @RequestParam(value = "pagesize", required = false, defaultValue = "10") int pagesize,
                                                   @RequestParam(value = "operator", required = false, defaultValue = "") String operator,
                                                   @RequestParam(value = "actionType", required = false, defaultValue = "") String actionType,
                                                   @RequestParam(value = "resourceType", required = false, defaultValue = "") String resourceType,
                                                   @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup) {
        List<XxlJobAuditLog> list = xxlJobAuditLogMapper.pageList(offset, pagesize, operator, actionType, resourceType, jobGroup);
        int count = xxlJobAuditLogMapper.pageListCount(offset, pagesize, operator, actionType, resourceType, jobGroup);
        Map<String, Object> data = new HashMap<>();
        data.put("data", list);
        data.put("total", count);
        return Response.ofSuccess(data);
    }

    @GetMapping("/help")
    @ResponseBody
    @XxlSso
    public Response<Map<String, Object>> helpInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("productName", "XXL-JOB Boost");
        data.put("githubUrl", "https://github.com/xuxueli/xxl-job");
        data.put("documentUrl", "https://www.xuxueli.com/xxl-job/");
        data.put("boostGithubUrl", "https://github.com/wuuJiawei/xxl-job-boost");
        return Response.ofSuccess(data);
    }

    private Map<String, Object> option(int value, String label) {
        Map<String, Object> option = new HashMap<>();
        option.put("value", value);
        option.put("label", label);
        return option;
    }

    private Map<String, Object> toGlueVersion(XxlJobLogGlue glue) {
        return toGlueVersion(glue.getId(), glue.getGlueType(), glue.getGlueRemark(), glue.getGlueSource(), glue.getAddTime());
    }

    private Map<String, Object> toGlueVersion(int id, String glueType, String glueRemark, String glueSource, Date updatedAt) {
        Map<String, Object> data = new HashMap<>();
        GlueTypeEnum match = GlueTypeEnum.match(glueType);
        data.put("id", id);
        data.put("glueType", glueType);
        data.put("glueTypeLabel", match != null ? match.getDesc() : glueType);
        data.put("glueRemark", glueRemark);
        data.put("glueSource", glueSource);
        data.put("updatedAt", updatedAt);
        return data;
    }

    private <T> List<Map<String, String>> toOptions(T[] values, java.util.function.Function<T, String> valueGetter, java.util.function.Function<T, String> labelGetter) {
        return java.util.Arrays.stream(values)
                .map(item -> {
                    Map<String, String> option = new HashMap<>();
                    option.put("value", valueGetter.apply(item));
                    option.put("label", labelGetter.apply(item));
                    return option;
                })
                .toList();
    }
}
