package org.mifos.processor.bulk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;

@Configuration
public class SecurityServerConfig {

    @Value("${security-server.host}")
    public String host;

    @Value("${security-server.baseuri}")
    public String baseUri;

    @Value("${security-server.country}")
    public String country;

    @Value("${security-server.organisation}")
    public String organisation;

    @Value("${security-server.endpoints.subs}")
    public String subscribingEndpoint;

    public String subscribingUrl;

    @PostConstruct
    public void setup() {
        subscribingUrl = host + baseUri + subscribingEndpoint;
        subscribingUrl = subscribingUrl.replace("{country}", country)
                .replace("{orgs}", organisation);
    }
}
