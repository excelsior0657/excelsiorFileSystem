package com.ss.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.utils.RequestUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Aspect
@Slf4j
@Component
public class ApiLoggerAspect {
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    public ApiLoggerAspect(HttpServletRequest request, ObjectMapper objectMapper) {
        this.request = request;
        this.objectMapper = objectMapper;
    }


    @Pointcut("execution(* com.ss.controller..*Controller.*(..))")
    public void apiLog(){

    }

    @Around("apiLog()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        LogModel logModel = new LogModel();
        Long start = System.currentTimeMillis();
        Object result = point.proceed();
        // 解析请求参数
        parseRequest(point, logModel);
        // 设置返回值
        logModel.setResponse(result);
        Long end = System.currentTimeMillis();
        logModel.setTimestamp(end)
                .setCost(end-start);
        log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logModel));
        return result;
    }

    @AfterThrowing(value = "apiLog()", throwing = "e")
    public void afterThrowing(JoinPoint point, Throwable e) throws JsonProcessingException {
        Long start = System.currentTimeMillis();
        StackTraceElement[] stackTrace = e.getStackTrace();
        String stackTracing = Arrays.toString(stackTrace).replace("[","").replace("]","");
        StackTraceElement stackTraceElement = e.getStackTrace()[0];
        ExceptionInfo exceptionInfo = new ExceptionInfo();
        exceptionInfo.setMessage(e.getMessage())
                .setFilename(stackTraceElement.getFileName())
                .setClassName(stackTraceElement.getClassName())
                .setMethodName(stackTraceElement.getMethodName())
                .setLineNumber(stackTraceElement.getLineNumber())
                .setDetails(stackTracing);
        LogModel logModel = new LogModel();
        logModel.setException(exceptionInfo);
        // 解析请求参数
        parseRequest((ProceedingJoinPoint) point, logModel);
        Long end = System.currentTimeMillis();
        logModel.setTimestamp(end)
                .setCost(end-start);
        log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logModel));
    }


    private void parseRequest(ProceedingJoinPoint point, LogModel logModel){
        String ip = RequestUtil.getIpAddress(request);
        String method = request.getMethod();
        StringBuffer path = request.getRequestURL();

        Object[] args = point.getArgs();
        CodeSignature signature = (CodeSignature)point.getSignature();
        String[] paramsName = signature.getParameterNames();
        Map<String, Object> params = new HashMap<>(paramsName.length);
        for(int i=0;i<paramsName.length;i++){
            params.put(paramsName[i], args[i].toString());
        }
        logModel.setIp(ip)
                .setMethod(method)
                .setPath(path.toString())
                .setParams(params);
    }

    @Data
    @Accessors(chain = true)
    public static class LogModel{
        private String ip;
        private String method;
        private String path;
        private Map<String, Object> params;
        private Object response;
        private ExceptionInfo exception;
        private Long timestamp;
        private Long cost;
    }

    @Data
    @Accessors(chain = true)
    public static class ExceptionInfo{
        private String message;
        private String filename;
        private String className;
        private String methodName;
        private Integer lineNumber;
        private Object details;

    }

}
