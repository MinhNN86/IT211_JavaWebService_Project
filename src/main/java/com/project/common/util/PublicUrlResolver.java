package com.project.common.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class PublicUrlResolver {
    public String resolve(String url) {
        if (url == null || url.isBlank() || !url.startsWith("/")) {
            return url;
        }
        if (RequestContextHolder.getRequestAttributes() == null) {
            return url;
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath().path(url).toUriString();
    }
}
