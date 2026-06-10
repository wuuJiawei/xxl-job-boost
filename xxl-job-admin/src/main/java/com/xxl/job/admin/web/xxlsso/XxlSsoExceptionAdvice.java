package com.xxl.job.admin.web.xxlsso;

import com.xxl.job.admin.controller.base.AdminNextApiController;
import com.xxl.sso.core.exception.XxlSsoException;
import com.xxl.tool.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminNextApiController.class)
public class XxlSsoExceptionAdvice {

    @ExceptionHandler(XxlSsoException.class)
    public ResponseEntity<Response<String>> handleXxlSsoException(XxlSsoException exception) {
        Response<String> body = Response.of(exception.getErrorCode(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
