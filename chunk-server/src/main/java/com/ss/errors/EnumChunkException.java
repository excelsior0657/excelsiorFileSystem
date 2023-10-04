package com.ss.errors;

import com.ss.interfaces.IResponse;

public enum EnumChunkException implements IResponse {
    FAILED_TO_CREATE_CHUNK_FILE(2001, "分片文件创建失败"),
    DISK_SPACE_NOT_ENOUGH_MEMORY(2002, "磁盘空间不足"),
    FAILED_TO_READ_CHUNK_FILE(2003, "分片文件读取失败");

    private final Integer code;
    private final String message;

    EnumChunkException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
