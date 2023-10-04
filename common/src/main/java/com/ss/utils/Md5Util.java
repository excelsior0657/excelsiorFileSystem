package com.ss.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Excelsior
 */
public class Md5Util {
    public static String getMd5(byte[] content) throws NoSuchAlgorithmException {
        try {
            MessageDigest digest = MessageDigest.getInstance("Md5");
            digest.update(content);
            byte[] bytes = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Md5 计算失败");
        }

    }

    public static String getMd5(MultipartFile file) {
        int size = (int) file.getSize();
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, byteCount);
            }
            byte[] bytes = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return builder.toString();

        } catch (Exception e) {
            throw new RuntimeException("Md5 计算失败");
        }
    }
}