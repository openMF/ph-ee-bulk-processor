package org.mifos.processor.bulk.API;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;

public class ApiOriginFilter extends GenericFilterBean {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String tenant = req.getHeader("" +
                HEADER_PLATFORM_TENANT_ID);
        logger.debug("Tenant Name is : {}", tenant);
        logger.info("Client IP Address: {}", req.getRemoteHost());
    }

}
