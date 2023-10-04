package com.ss.errors;

import com.ss.interfaces.IResponse;

/**
 * @author Excelsior
 */

public enum EnumMetaException implements IResponse {
    NOT_ENOUGH_CHUNK_SERVER(1001, "分片服务数量不足"),
    META_FILE_NOT_FOUND(1002, "文件元数据不存在"),
    CHUNK_FILE_NOT_UPLOADED(1003, "分片未上传"),
    NO_CHUNK_META_AVAILABLE(1004, "分片不可用");

    private final int code;
    private final String message;

    EnumMetaException(int code, String message) {
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
