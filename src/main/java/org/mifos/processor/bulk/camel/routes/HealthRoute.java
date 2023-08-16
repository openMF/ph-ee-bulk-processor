package org.mifos.processor.bulk.camel.routes;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class HealthRoute extends BaseRouteBuilder {

    @Override
    public void configure() throws Exception {

        // todo remove once camel APIs are migrated to spring
        from("rest:GET:/actuator/health/liveness")
                .id("rest:GET:/actuator/health/liveness")
                .setBody(exchange -> new JSONObject(){{ put("status", "UP"); }}.toString());
    }
}
