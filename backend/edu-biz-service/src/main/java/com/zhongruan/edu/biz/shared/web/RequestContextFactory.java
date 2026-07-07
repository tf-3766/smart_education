package com.zhongruan.edu.biz.shared.web;

import com.zhongruan.edu.biz.shared.security.AuthenticatedUser;
import com.zhongruan.edu.common.context.RequestContext;
import com.zhongruan.edu.common.context.RequestSource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RequestContextFactory {
    public RequestContext current(HttpServletRequest request) {
        RequestSource source = parseSource(request.getHeader("X-Request-Source"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return new RequestContext(RequestTrace.from(request), user.userId(), user.activeRole(), source);
        }
        return RequestContext.anonymous(RequestTrace.from(request), source);
    }

    private RequestSource parseSource(String source) {
        if (source == null || source.isBlank()) {
            return RequestSource.DIRECT;
        }
        try {
            return RequestSource.valueOf(source);
        } catch (IllegalArgumentException ignored) {
            return RequestSource.DIRECT;
        }
    }
}

