package com.project.aspect;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class PerformanceLogAspect {

    @Value("${app.performance.warn-threshold-ms:500}")
    private long warnThresholdMs;

    @Pointcut("""
            @within(org.springframework.stereotype.Service) &&
            execution(public * com.project.modules..service..*(..)) &&
            !within(com.project.modules.audit.service..*)
            """)
    public void serviceLayerMethods() {
    }

    @Around("serviceLayerMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long durationMs = Duration.ofNanos(System.nanoTime() - startTime).toMillis();

            if (durationMs >= warnThresholdMs) {
                log.warn("[PERF] {}.{}() executed in {} ms (threshold: {} ms)", className, methodName, durationMs,
                        warnThresholdMs);
            } else {
                log.debug("[PERF] {}.{}() executed in {} ms", className, methodName, durationMs);
            }
            return result;
        } catch (Throwable ex) {
            long durationMs = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
            log.error("[PERF] {}.{}() failed after {} ms - {}", className, methodName, durationMs,
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
