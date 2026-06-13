package com.xxl.job.admin.service;

import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.sso.core.model.LoginInfo;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Map;

/**
 * core job action for xxl-job
 * 
 * @author xuxueli 2016-5-28 15:30:33
 */
public interface XxlJobService {

	/**
	 * page list
	 */
	public Response<PageModel<XxlJobInfo>> pageList(int offset, int pagesize, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author, String jobTag);

	/**
	 * add job
	 */
	public Response<String> add(XxlJobInfo jobInfo, LoginInfo loginInfo, HttpServletRequest request);

	/**
	 * update job
	 */
	public Response<String> update(XxlJobInfo jobInfo, LoginInfo loginInfo, HttpServletRequest request);

	/**
	 * remove job
	 */
	public Response<String> remove(int id, LoginInfo loginInfo, HttpServletRequest request);

	/**
	 * start job
	 */
	public Response<String> start(int id, LoginInfo loginInfo, HttpServletRequest request);

	/**
	 * stop job
	 */
	public Response<String> stop(int id, LoginInfo loginInfo, HttpServletRequest request);

	/**
	 * trigger
	 */
	public Response<String> trigger(LoginInfo loginInfo, int jobId, String executorParam, String addressList, HttpServletRequest request);

	/**
	 * dashboard info
	 */
	public Map<String,Object> dashboardInfo();

	/**
	 * chart info
	 */
	public Response<Map<String,Object>> chartInfo(Date startDate, Date endDate);

}
