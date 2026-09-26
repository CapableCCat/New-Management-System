package com.tsguosc.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.tsguosc.common.result.Result;
import com.tsguosc.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：任何异常都收敛成统一返回体 {@code {code, message, data}}。
 *
 * <p>约定：HTTP 状态码一律 200，真实语义由 {@code code} 承载（前端只判断 code）。
 * <p>兜底分支只回通用文案，堆栈只进日志，避免把内部细节暴露到响应里。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按异常自带的 code 返回 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return Result.fail(ResultCode.PARAM_ERROR, fieldMessage(e.getBindingResult().getFieldError()));
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        return Result.fail(ResultCode.PARAM_ERROR, fieldMessage(e.getBindingResult().getFieldError()));
    }

    /** 方法参数上的约束校验失败（@Validated + @RequestParam 等） */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.warn("参数校验失败：{}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /** 缺少必填参数 / 参数类型不匹配 / 请求体解析失败 */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("请求参数有误：{}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR);
    }

    /** 请求方式不被支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方式不支持：{}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR, "请求方式不被支持：" + e.getMethod());
    }

    /**
     * 上传文件超过 spring.servlet.multipart 限制。
     *
     * <p>不拦的话会冒成 500 系统异常，用户看到的是"系统繁忙"，无法判断是自己图片太大。
     * （上传口有头像与公告配图两处，文案取通用的「图片」。）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("上传文件超限：{}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR, "图片大小不能超过 2MB");
    }

    /** 访问了不存在的接口/资源 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("请求的资源不存在：{}", e.getResourcePath());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /** Sa-Token：未登录（T4 起登录链路生效后启用） */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("未登录访问：{}", e.getMessage());
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    /** Sa-Token：无权限 / 角色不足（T4 起登录链路生效后启用） */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<Void> handleNoPermissionException(Exception e) {
        log.warn("无权限访问：{}", e.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);
    }

    /** 兜底：未知异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }

    private String fieldMessage(FieldError fieldError) {
        if (fieldError == null) {
            return ResultCode.PARAM_ERROR.getMessage();
        }
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}
