package org.mifos.processor.bulk.api;

import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;

public class ApiOriginFilter extends GenericFilterBean {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String tenant = req.getHeader("" + HEADER_PLATFORM_TENANT_ID);
        logger.debug("Tenant Name is : {}", tenant);
        logger.info("Client IP Address: {}", req.getRemoteHost());
    }

}
