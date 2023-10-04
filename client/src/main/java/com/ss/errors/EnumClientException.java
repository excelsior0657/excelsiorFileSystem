package com.ss.errors;

import com.ss.interfaces.IResponse;

public enum EnumClientException implements IResponse {
    FAILED_TO_GET_META_FILE(3001, "获取 meta file 失败"),
    FAILED_TO_UPLOAD_CHUNK_FILE(3002, "上传分片失败"),
    SCHEMA_DOES_NOT_SUPPORT(3003, "通信协议不支持"),
    FAILED_TO_UPDATE_CHUNK_FILE_COMPLETE_STATUS(3004, "分片文件状态修改失败"),
    CHUNK_FILE_INCOMPLETE(3005, "分片文件不完整"),
    FAILED_TO_DOWNLOAD_FILE(3006, "文件下载失败")
    ;

    private final int code;
    private final String message;

    EnumClientException(int code, String message) {
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
