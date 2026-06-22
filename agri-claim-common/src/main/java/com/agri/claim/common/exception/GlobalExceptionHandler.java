package com.agri.claim.common.exception;

import com.agri.claim.common.result.R;
import com.agri.claim.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常 | uri: {} | code: {} | msg: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage()).traceId(TraceContext.traceId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("参数校验异常 | uri: {} | msg: {}", request.getRequestURI(), msg);
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg).traceId(TraceContext.traceId());
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        FieldError fieldError = e.getFieldError();
        String msg = fieldError != null ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "参数绑定异常";
        log.error("参数绑定异常 | uri: {} | msg: {}", request.getRequestURI(), msg);
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg).traceId(TraceContext.traceId());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e,
                                                       HttpServletRequest request) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.error("约束违反异常 | uri: {} | msg: {}", request.getRequestURI(), msg);
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg).traceId(TraceContext.traceId());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e,
                                                                  HttpServletRequest request) {
        String msg = "缺少必需参数: " + e.getParameterName();
        log.error("缺少必需参数 | uri: {} | msg: {}", request.getRequestURI(), msg);
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg).traceId(TraceContext.traceId());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                              HttpServletRequest request) {
        String msg = "参数类型错误: " + e.getName() + " 应为 " +
                (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "指定类型");
        log.error("参数类型错误 | uri: {} | msg: {}", request.getRequestURI(), msg);
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg).traceId(TraceContext.traceId());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
                                                          HttpServletRequest request) {
        log.error("请求体不可读 | uri: {} | msg: {}", request.getRequestURI(), e.getMessage());
        return R.fail(ResultCode.BAD_REQUEST, "请求体格式错误").traceId(TraceContext.traceId());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e,
                                                                 HttpServletRequest request) {
        log.error("请求方法不支持 | uri: {} | method: {}", request.getRequestURI(), e.getMethod());
        return R.fail(ResultCode.METHOD_NOT_ALLOWED).traceId(TraceContext.traceId());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 | uri: {}", request.getRequestURI(), e);
        return R.fail(ResultCode.SYSTEM_ERROR).traceId(TraceContext.traceId());
    }
}
