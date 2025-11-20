package org.mifos.processor.bulk.config;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class PreflightCorsFilter implements Filter {

    private final List<String> allowedOrigins;

    @Autowired
    public PreflightCorsFilter(CorsProperties corsProperties) {
        this.allowedOrigins = corsProperties.getAllowedOrigins();
    }

    @Override
    public void doFilter(javax.servlet.ServletRequest servletRequest,
                         javax.servlet.ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String origin = request.getHeader("Origin");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) && origin != null && allowedOrigins.contains(origin)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers",
                    "content-type,platform-tenantid,purpose,type,x-callback-url,x-correlationid,x-program-id,x-registering-institution-id,x-signature");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "86400");
            return; // short-circuit OPTIONS preflight
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {
    }

}
