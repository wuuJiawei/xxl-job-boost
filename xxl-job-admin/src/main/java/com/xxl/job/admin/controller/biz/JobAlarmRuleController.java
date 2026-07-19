package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.core.alarm.AlarmChannelService;
import com.xxl.job.admin.core.alarm.AlarmEventType;
import com.xxl.job.admin.mapper.XxlJobAlarmRuleMapper;
import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.model.XxlJobAlarmRule;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.service.AuditLogService;
import com.xxl.job.admin.util.JobGroupPermissionUtil;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.CollectionTool;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/alarmrule")
public class JobAlarmRuleController {

    @Resource
    private XxlJobAlarmRuleMapper xxlJobAlarmRuleMapper;
    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource
    private AlarmChannelService alarmChannelService;
    @Resource
    private AuditLogService auditLogService;

    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso
    public Response<PageModel<XxlJobAlarmRule>> pageList(HttpServletRequest request,
                                                         @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
                                                         @RequestParam(value = "pagesize", required = false, defaultValue = "10") int pagesize,
                                                         @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup,
                                                         @RequestParam(value = "jobId", required = false, defaultValue = "0") int jobId,
                                                         @RequestParam(value = "alarmEvent", required = false, defaultValue = "") String alarmEvent,
                                                         @RequestParam(value = "enabled", required = false, defaultValue = "-1") int enabled) {

        if (jobGroup > 0) {
            JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup);
        }
        List<Integer> permittedGroupIds = JobGroupPermissionUtil.filterJobGroupByPermission(request, xxlJobGroupMapper.findAll())
                .stream().map(XxlJobGroup::getId).toList();
        if (permittedGroupIds.isEmpty()) {
            PageModel<XxlJobAlarmRule> empty = new PageModel<>();
            empty.setData(List.of());
            empty.setTotal(0);
            return Response.ofSuccess(empty);
        }
        List<XxlJobAlarmRule> list = xxlJobAlarmRuleMapper.pageList(offset, pagesize, permittedGroupIds, jobGroup, jobId, alarmEvent, enabled);
        int count = xxlJobAlarmRuleMapper.pageListCount(offset, pagesize, permittedGroupIds, jobGroup, jobId, alarmEvent, enabled);

        PageModel<XxlJobAlarmRule> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(count);
        return Response.ofSuccess(pageModel);
    }

    @RequestMapping("/insert")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> insert(HttpServletRequest request, XxlJobAlarmRule rule) {
        LoginInfo loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, rule.getJobGroup());
        Response<String> valid = validate(rule);
        if (!valid.isSuccess()) {
            return valid;
        }
        rule.setUpdateTime(new Date());
        int ret = xxlJobAlarmRuleMapper.save(rule);
        if (ret > 0) {
            auditLogService.record(loginInfo, request, "alarmrule-insert", "alarm-rule",
                    String.valueOf(rule.getId()), rule.getName(), rule.getJobGroup(), rule);
        }
        return ret > 0 ? Response.ofSuccess() : Response.ofFail();
    }

    @RequestMapping("/update")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> update(HttpServletRequest request, XxlJobAlarmRule rule) {
        XxlJobAlarmRule existing = xxlJobAlarmRuleMapper.load(rule.getId());
        if (rule.getId() < 1 || existing == null) {
            return Response.ofFail("告警规则不存在");
        }

        LoginInfo loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, existing.getJobGroup());
        if (rule.getJobGroup() != existing.getJobGroup()) {
            JobGroupPermissionUtil.validJobGroupPermission(request, rule.getJobGroup());
        }

        Response<String> valid = validate(rule);
        if (!valid.isSuccess()) {
            return valid;
        }

        rule.setUpdateTime(new Date());
        int ret = xxlJobAlarmRuleMapper.update(rule);
        if (ret > 0) {
            auditLogService.record(loginInfo, request, "alarmrule-update", "alarm-rule",
                    String.valueOf(rule.getId()), rule.getName(), rule.getJobGroup(), rule);
        }
        return ret > 0 ? Response.ofSuccess() : Response.ofFail();
    }

    @RequestMapping("/delete")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> delete(HttpServletRequest request, @RequestParam("ids[]") List<Integer> ids) {
        if (CollectionTool.isEmpty(ids) || ids.size() != 1) {
            return Response.ofFail("请选择一条告警规则数据");
        }
        XxlJobAlarmRule existing = xxlJobAlarmRuleMapper.load(ids.get(0));
        if (existing == null) {
            return Response.ofFail("告警规则不存在");
        }

        LoginInfo loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, existing.getJobGroup());
        int ret = xxlJobAlarmRuleMapper.remove(ids.get(0));
        if (ret > 0) {
            auditLogService.record(loginInfo, request, "alarmrule-delete", "alarm-rule",
                    String.valueOf(ids.get(0)), existing.getName(), existing.getJobGroup(), existing);
        }
        return ret > 0 ? Response.ofSuccess() : Response.ofFail();
    }

    @RequestMapping("/executorPolicy")
    @ResponseBody
    @XxlSso
    public Response<List<XxlJobAlarmRule>> executorPolicy(HttpServletRequest request,
                                                          @RequestParam("jobGroup") int jobGroup) {
        JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup);
        if (xxlJobGroupMapper.load(jobGroup) == null) {
            return Response.ofFail("执行器不存在");
        }
        return Response.ofSuccess(xxlJobAlarmRuleMapper.findExecutorDefaults(jobGroup));
    }

    @RequestMapping("/updateExecutorPolicy")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    @Transactional
    public Response<String> updateExecutorPolicy(HttpServletRequest request,
                                                 @RequestParam("jobGroup") int jobGroup,
                                                 @RequestParam(value = "executorFailChannelIds", required = false, defaultValue = "") String executorFailChannelIds,
                                                 @RequestParam(value = "executorTimeoutChannelIds", required = false, defaultValue = "") String executorTimeoutChannelIds,
                                                 @RequestParam(value = "triggerFailChannelIds", required = false, defaultValue = "") String triggerFailChannelIds) {
        LoginInfo loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup);
        XxlJobGroup group = xxlJobGroupMapper.load(jobGroup);
        if (group == null) {
            return Response.ofFail("执行器不存在");
        }

        Map<AlarmEventType, String> policy = new LinkedHashMap<>();
        policy.put(AlarmEventType.EXECUTOR_FAIL, executorFailChannelIds);
        policy.put(AlarmEventType.EXECUTOR_TIMEOUT, executorTimeoutChannelIds);
        policy.put(AlarmEventType.TRIGGER_FAIL, triggerFailChannelIds);

        Map<AlarmEventType, String> normalizedPolicy = new LinkedHashMap<>();
        try {
            for (Map.Entry<AlarmEventType, String> entry : policy.entrySet()) {
                normalizedPolicy.put(entry.getKey(), alarmChannelService.normalizeChannelIdsToString(entry.getValue()));
            }
        } catch (IllegalArgumentException e) {
            return Response.ofFail(e.getMessage());
        }

        xxlJobAlarmRuleMapper.removeExecutorDefaults(jobGroup);
        Date now = new Date();
        for (Map.Entry<AlarmEventType, String> entry : normalizedPolicy.entrySet()) {
            if (StringTool.isBlank(entry.getValue())) {
                continue;
            }
            XxlJobAlarmRule rule = new XxlJobAlarmRule();
            rule.setName(entry.getKey().getTitle() + "默认告警");
            rule.setJobGroup(jobGroup);
            rule.setJobId(0);
            rule.setAlarmEvent(entry.getKey().name());
            rule.setChannelIds(entry.getValue());
            rule.setEnabled(1);
            rule.setRemark("执行器默认告警策略");
            rule.setUpdateTime(now);
            if (xxlJobAlarmRuleMapper.save(rule) < 1) {
                throw new IllegalStateException("执行器默认告警策略保存失败");
            }
        }

        auditLogService.record(loginInfo, request, "alarm-policy-update", "job-group",
                String.valueOf(jobGroup), group.getTitle(), jobGroup, normalizedPolicy);
        return Response.ofSuccess();
    }

    private Response<String> validate(XxlJobAlarmRule rule) {
        if (StringTool.isBlank(rule.getName())) {
            return Response.ofFail("请输入规则名称");
        }
        if (rule.getName().length() > 64) {
            return Response.ofFail("规则名称长度不能超过64");
        }
        if (rule.getJobGroup() < 1) {
            return Response.ofFail("请选择执行器");
        }
        XxlJobGroup group = xxlJobGroupMapper.load(rule.getJobGroup());
        if (group == null) {
            return Response.ofFail("执行器不存在");
        }
        if (rule.getJobId() != null && rule.getJobId() > 0) {
            return Response.ofFail("任务级告警请在任务编辑页配置");
        }
        rule.setJobId(0);

        AlarmEventType eventType = AlarmEventType.match(rule.getAlarmEvent());
        if (eventType == null) {
            return Response.ofFail("告警事件类型非法");
        }
        rule.setAlarmEvent(eventType.name());

        try {
            rule.setChannelIds(alarmChannelService.normalizeChannelIdsToString(rule.getChannelIds()));
        } catch (IllegalArgumentException e) {
            return Response.ofFail(e.getMessage());
        }
        if (StringTool.isBlank(rule.getChannelIds())) {
            return Response.ofFail("请至少绑定一个告警渠道");
        }

        if (rule.getEnabled() != 0) {
            rule.setEnabled(1);
        }
        return Response.ofSuccess();
    }
}
