package com.ss.utils;

import com.ss.dto.FileMeta;

public interface FilenameGenerator {
    String generate(FileMeta fileMeta, Object... args);
}
