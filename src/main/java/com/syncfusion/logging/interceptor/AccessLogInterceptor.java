package com.syncfusion.logging.interceptor;


import com.syncfusion.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static java.util.Objects.isNull;

@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger(Constants.ACCESS_LOG);
    private final DateFormat dateFormat = new SimpleDateFormat(Constants.LOG_DATE_FORMAT);
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTime.set(System.currentTimeMillis());
        setRequestId(request);
        setResponseRequestId(response);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        String requestHost = httpServletRequest.getHeader(Constants.X_REQUEST_HOST);
        String requestRealIP = httpServletRequest.getHeader(Constants.X_REAL_IP);
        if (isNull(requestHost))
            requestHost = httpServletRequest.getRemoteAddr();
        Date date = new Date();
        String method = httpServletRequest.getMethod();
        String uri = httpServletRequest.getRequestURI();
        String protocol = httpServletRequest.getProtocol();

        String referer = httpServletRequest.getHeader(Constants.REFERER);
        if (isNull(referer))
            referer = Constants.HYPHEN;

        String userAgent = httpServletRequest.getHeader(Constants.USER_AGENT);
        if (userAgent == null)
            userAgent = Constants.HYPHEN;

        String callingHost = httpServletRequest.getHeader(Constants.HOST);
        if (isNull(callingHost))
            userAgent = Constants.HYPHEN;
        String message = (requestRealIP != null ? requestRealIP : requestHost) + " " +
                "- " +
                "[" + dateFormat.format(date) + "] " +
                "\"" + method + " " + uri + " " + protocol + "\" " +
                response.getStatus() + " " +
                "- " +
                "\"" + referer + "\" " +
                "\"" + userAgent + "\" " +
                "\"" + callingHost + "\" " +
                (System.currentTimeMillis() - startTime.get());
        ACCESS_LOG.info(message);
        startTime.remove();
    }

    private void setRequestId(HttpServletRequest httpServletRequest) {
        String requestId = httpServletRequest.getHeader(Constants.X_REQUEST_ID);
        if (isNull(requestId))
            requestId = UUID.randomUUID().toString();
        requestId = requestId.replaceAll(Constants.SINGLE_WHITESPACE, Constants.HYPHEN);
        MDC.put(Constants.REQUEST_ID, requestId);
    }

    private void setResponseRequestId(HttpServletResponse httpServletResponse) {
        httpServletResponse.addHeader(Constants.X_REQUEST_ID, MDC.get(Constants.REQUEST_ID));
    }
}
