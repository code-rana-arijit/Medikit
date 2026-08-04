package com.medikit.common.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class UserContext {

    private UserContext() {
    }

    public static String currentUserId() {
        return header("X-User-Id");
    }

    public static String currentUserRole() {
        return header("X-User-Role");
    }

    public static String currentUserEmail() {
        return header("X-User-Email");
    }

    private static String header(String name) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest().getHeader(name);
    }
}
