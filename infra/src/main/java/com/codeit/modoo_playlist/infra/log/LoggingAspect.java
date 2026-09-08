package com.codeit.modoo_playlist.infra.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // 서비스 계층 포인트컷
    @Pointcut("execution(* com.codeit.blog.service..*.*(..))")
    public void serviceLayerPointcut() {}

    // 컨트롤러 계층 포인트컷
    @Pointcut("execution(* com.codeit.blog.controller..*.*(..))")
    public void controllerLayerPointcut() {}

    // 메서드 진입 로그
    @Before("serviceLayerPointcut() || controllerLayerPointcut()")
    public void logBefore(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.debug("==> {}.{}({})", className, methodName, Arrays.toString(joinPoint.getArgs()));
    }

    // 정상 반환 로그
    @AfterReturning(pointcut = "serviceLayerPointcut() || controllerLayerPointcut()", returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.debug("<== {}.{}({}), return: {}", className, methodName,
                Arrays.toString(joinPoint.getArgs()), result);
    }

    // 예외 발생 로그
    @AfterThrowing(pointcut = "serviceLayerPointcut() || controllerLayerPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Exception e) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.error("<== {}.{}() 예외: {}", className, methodName, e.getMessage(), e);
    }

    // 서비스 실행시간 측정 (1초 초과 시 warn)
    @Around("serviceLayerPointcut()")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("{}#{} 실행 시간: {}ms", className, methodName, elapsed);
            if (elapsed > 1000) {
                log.warn("{}#{} 실행 시간이 {}ms로 느립니다. 성능 최적화 필요",
                        className, methodName, elapsed);
            }
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("{}#{} 실행 실패 - {}ms, 예외: {}", className, methodName, elapsed, t.getMessage());
            throw t;
        }
    }

    // 중요 비즈니스 이벤트 포인트컷 (로그인 / 회원가입)
    @Pointcut("execution(* com.codeit.blog.service.AuthService.login(..))"
            + " || execution(* com.codeit.blog.service.UserService.create(..))")
    public void businessEventsPointcut() {}

    @Before("businessEventsPointcut()")
    public void logBusinessEvents(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("[BIZ] ==> {}.{}({})", className, methodName, Arrays.toString(joinPoint.getArgs()));
    }
}
