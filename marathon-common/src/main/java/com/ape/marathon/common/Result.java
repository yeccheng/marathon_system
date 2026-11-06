package com.ape.marathon.common;

/**
 * 统一响应结果封装：{ code, msg, data }
 */
public class Result<T> {

    private Integer code;
    private String msg;
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(200, "操作成功", data);
    }

    public static <T> Result<T> success() {
        return new Result<T>(200, "操作成功", null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<T>(code, msg, null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<T>(500, msg, null);
    }

    public boolean ok() {
        return code != null && code == 200;
    }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
