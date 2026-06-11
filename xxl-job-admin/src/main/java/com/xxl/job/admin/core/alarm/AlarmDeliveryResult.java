package com.xxl.job.admin.core.alarm;

public class AlarmDeliveryResult {

    private boolean success;
    private Integer responseCode;
    private String responseBody;
    private String errorMsg;

    public static AlarmDeliveryResult success(Integer responseCode, String responseBody) {
        AlarmDeliveryResult result = new AlarmDeliveryResult();
        result.success = true;
        result.responseCode = responseCode;
        result.responseBody = responseBody;
        return result;
    }

    public static AlarmDeliveryResult fail(Integer responseCode, String responseBody, String errorMsg) {
        AlarmDeliveryResult result = new AlarmDeliveryResult();
        result.success = false;
        result.responseCode = responseCode;
        result.responseBody = responseBody;
        result.errorMsg = errorMsg;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
