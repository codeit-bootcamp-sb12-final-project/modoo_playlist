package com.codeit.modoo_playlist.infra.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * MDC(Mapped Diagnostic Context) 인터셉터
 * 모든 요청마다 requestId / method / uri 를 MDC에 주입 → 로그에서 요청 추적 가능
 */
@Slf4j
public class MDCLoggingInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID     = "requestId";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String REQUEST_URI    = "requestUri";
    public static final String REQUEST_ID_HEADER = "Blog-Request-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // IP + UUID 앞 8자리로 requestId 생성
        String requestId = getClientIp(request) + "-" + UUID.randomUUID().toString().substring(0, 8);

        MDC.put(REQUEST_ID, requestId);
        MDC.put(REQUEST_METHOD, request.getMethod());
        MDC.put(REQUEST_URI, request.getRequestURI());
        response.setHeader(REQUEST_ID_HEADER, requestId);

        log.debug("Request started");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        log.debug("Request finished");
        MDC.clear(); // 반드시 정리
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }

        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isLoopbackAddress()) return "127.0.0.1";
            if (addr instanceof Inet6Address && ip.startsWith("::ffff:")) return ip.substring(7);
            return addr.getHostAddress();
        } catch (UnknownHostException e) {
            return ip;
        }
    }
}
