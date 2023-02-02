package searchengine.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**Аспект для замера времени выполнения методов*/
@Aspect
@Component
public class LogExecutionTimeAspect {
    private static final Logger log = LoggerFactory.getLogger(LogExecutionTimeAspect.class);

    @Around("execution(* searchengine.services.indexService.*.*(..))")
    public Object logExecutionTimeAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();

        log.info("Время выполнения метода {} заняло {} мс.", joinPoint.getSignature(), System.currentTimeMillis()-startTime );
        return result;
    }
}
