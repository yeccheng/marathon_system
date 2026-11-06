package com.ape.marathon.ai.common;

/**
 * 对齐主工程 Result&lt;T&gt;：{ code, msg, data }
 */
public class ApiResult<T> {

    private Integer code;
    private String msg;
    private T data;

    public boolean ok() {
        return code != null && code == 200;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
