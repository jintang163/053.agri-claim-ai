package com.agri.claim.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "访问被拒绝，权限不足"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    SYSTEM_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    LOGIN_ERROR(1001, "登录失败，用户名或密码错误"),
    LOGIN_EXPIRED(1002, "登录已过期，请重新登录"),
    TOKEN_INVALID(1003, "Token无效"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    USER_DISABLED(1005, "账号已被禁用"),
    USER_NOT_EXIST(1006, "用户不存在"),
    PASSWORD_ERROR(1007, "密码错误"),
    CAPTCHA_ERROR(1008, "验证码错误"),

    FILE_NOT_FOUND(2001, "文件不存在"),
    FILE_UPLOAD_ERROR(2002, "文件上传失败"),
    FILE_DOWNLOAD_ERROR(2003, "文件下载失败"),
    FILE_TOO_LARGE(2004, "文件大小超出限制"),
    FILE_FORMAT_ERROR(2005, "文件格式不支持"),

    IMAGE_PROCESS_ERROR(3001, "影像处理失败"),
    IMAGE_UPLOAD_ERROR(3002, "影像上传失败"),
    IMAGE_NOT_FOUND(3003, "影像不存在"),
    IMAGE_FORMAT_ERROR(3004, "影像格式不支持"),
    IMAGE_PREPROCESS_ERROR(3005, "影像预处理失败"),
    IMAGE_CORRECTION_ERROR(3006, "影像校正失败"),

    AI_PROCESS_ERROR(4001, "AI处理失败"),
    MODEL_INFER_ERROR(4002, "模型推理失败"),
    SEGMENT_ERROR(4003, "农田分割失败"),
    CHANGE_DETECT_ERROR(4004, "变化检测失败"),
    NDVI_CALCULATE_ERROR(4005, "NDVI计算失败"),

    ASSESS_ERROR(5001, "定损失败"),
    COMPENSATE_CALC_ERROR(5002, "赔付金额计算失败"),
    REPORT_GENERATE_ERROR(5003, "定损报告生成失败"),
    POLICY_NOT_FOUND(5004, "保单信息不存在"),
    CROP_NOT_FOUND(5005, "作物信息不存在"),

    DATA_NOT_FOUND(6001, "数据不存在"),
    DATA_EXIST(6002, "数据已存在"),
    DATA_DELETE_ERROR(6003, "数据删除失败"),
    DATA_INSERT_ERROR(6004, "数据新增失败"),
    DATA_UPDATE_ERROR(6005, "数据更新失败"),
    DATA_EXPORT_ERROR(6006, "数据导出失败"),
    DATA_IMPORT_ERROR(6007, "数据导入失败");

    private final Integer code;
    private final String msg;
}
