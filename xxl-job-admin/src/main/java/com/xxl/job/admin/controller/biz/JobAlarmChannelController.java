package com.xxl.job.admin.controller.biz;

import com.xxl.job.admin.constant.Consts;
import com.xxl.job.admin.core.alarm.AlarmChannelType;
import com.xxl.job.admin.mapper.XxlJobAlarmChannelMapper;
import com.xxl.job.admin.model.XxlJobAlarmChannel;
import com.xxl.job.admin.service.AuditLogService;
import com.xxl.sso.core.annotation.XxlSso;
import com.xxl.sso.core.helper.XxlSsoHelper;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.core.CollectionTool;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.json.GsonTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/alarmchannel")
public class JobAlarmChannelController {

    @Resource
    private XxlJobAlarmChannelMapper xxlJobAlarmChannelMapper;
    @Resource
    private AuditLogService auditLogService;

    @RequestMapping("/pageList")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<PageModel<XxlJobAlarmChannel>> pageList(
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "pagesize", required = false, defaultValue = "10") int pagesize,
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "enabled", required = false, defaultValue = "-1") int enabled) {

        List<XxlJobAlarmChannel> list = xxlJobAlarmChannelMapper.pageList(offset, pagesize, name, type, enabled);
        int count = xxlJobAlarmChannelMapper.pageListCount(offset, pagesize, name, type, enabled);

        PageModel<XxlJobAlarmChannel> pageModel = new PageModel<>();
        pageModel.setData(list);
        pageModel.setTotal(count);
        return Response.ofSuccess(pageModel);
    }

    @RequestMapping("/insert")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> insert(HttpServletRequest request, XxlJobAlarmChannel channel) {
        Response<String> valid = validate(channel);
        if (!valid.isSuccess()) {
            return valid;
        }

        Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
        channel.setUpdateTime(new Date());
        int ret = xxlJobAlarmChannelMapper.save(channel);
        if (ret > 0) {
            auditLogService.record(loginInfoResponse.getData(), request, "alarmchannel-insert", "alarm-channel", String.valueOf(channel.getId()), channel.getName(), null, channel);
        }
        return ret > 0 ? Response.ofSuccess() : Response.ofFail();
    }

    @RequestMapping("/update")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> update(HttpServletRequest request, XxlJobAlarmChannel channel) {
        XxlJobAlarmChannel existing = xxlJobAlarmChannelMapper.load(channel.getId());
        if (channel.getId() < 1 || existing == null) {
            return Response.ofFail("告警渠道不存在");
        }

        Response<String> valid = validate(channel);
        if (!valid.isSuccess()) {
            return valid;
        }

        Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
        channel.setUpdateTime(new Date());
        int ret = xxlJobAlarmChannelMapper.update(channel);
        if (ret > 0) {
            auditLogService.record(loginInfoResponse.getData(), request, "alarmchannel-update", "alarm-channel", String.valueOf(channel.getId()), channel.getName(), null, channel);
        }
        return ret > 0 ? Response.ofSuccess() : Response.ofFail();
    }

    @RequestMapping("/delete")
    @ResponseBody
    @XxlSso(role = Consts.ADMIN_ROLE)
    public Response<String> delete(HttpServletRequest request, @RequestParam("ids[]") List<Integer> ids) {
        if (CollectionTool.isEmpty(ids) || ids.size() != 1) {
            return Response.ofFail("请选择一条告警渠道数据");
        }

        Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
        XxlJobAlarmChannel existing = xxlJobAlarmChannelMapper.load(ids.get(0));
        int ret = xxlJobAlarmChannelMapper.remove(ids.get(0));
        if (ret > 0) {
            auditLogService.record(loginInfoResponse.getData(), request, "alarmchannel-delete", "alarm-channel", String.valueOf(ids.get(0)), existing != null ? existing.getName() : null, null, existing);
        }
        return ret > 0 ? Response.ofSuccess() : Response.ofFail();
    }

    private Response<String> validate(XxlJobAlarmChannel channel) {
        if (StringTool.isBlank(channel.getName())) {
            return Response.ofFail("请输入渠道名称");
        }
        if (channel.getName().length() > 64) {
            return Response.ofFail("渠道名称长度不能超过64");
        }

        AlarmChannelType type = AlarmChannelType.match(channel.getType());
        if (type == null) {
            return Response.ofFail("告警渠道类型非法");
        }
        channel.setType(type.name());

        if (channel.getEnabled() != 0) {
            channel.setEnabled(1);
        }

        if (type == AlarmChannelType.EMAIL) {
            if (StringTool.isBlank(channel.getRecipients())) {
                return Response.ofFail("邮件渠道必须配置接收人");
            }
        } else if (StringTool.isBlank(channel.getEndpoint())) {
            return Response.ofFail("Webhook渠道必须配置地址");
        }

        if (StringTool.isNotBlank(channel.getHeadersJson())) {
            try {
                GsonTool.fromJsonMap(channel.getHeadersJson(), String.class, Object.class);
            } catch (Exception e) {
                return Response.ofFail("自定义请求头JSON格式非法");
            }
        }

        return Response.ofSuccess();
    }
}
