package com.ape.marathon.common;

/**
 * 统一响应结果封装
 * 所有接口均返回该结构：{ code, msg, data }
 * code=200 表示操作成功；其他为失败
 */
public class Result<T> {

    /** 状态码：200 成功，其他 失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功：携带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /** 成功：无数据（如删除、修改） */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /** 失败：自定义错误码与信息 */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    /** 失败：默认 500 */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}