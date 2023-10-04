package com.ss.utils.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.ss.dto.FileMeta;
import com.ss.utils.FilenameGenerator;
import org.springframework.util.DigestUtils;

import java.security.DigestException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DefaultFilenameGenerator implements FilenameGenerator {
    @Override
    public String generate(FileMeta fileMeta, Object... args) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
        String time = now.format(formatter);
        return "f%s%s".formatted(time, md5(fileMeta, args[0].toString()));
    }

    private String md5(FileMeta fileMeta, String address){
        return DigestUtil.md5Hex(
                "%s_%s_%s_%s".formatted(
                        address,
                        fileMeta.getFilesize(),
                        fileMeta.getBucketName(),
                        fileMeta.getExtension()
                )
        );
    }
}
