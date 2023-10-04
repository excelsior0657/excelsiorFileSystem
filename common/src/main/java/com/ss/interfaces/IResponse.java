package com.ss.interfaces;

public interface IResponse {
    /**
     * 获取状态码
     * @return code
     */
    int getCode();

    /**
     * 获取消息
     * @return message
     */
    String getMessage();
}
