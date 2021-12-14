package org.mifos.processor.bulk.camel.routes;


import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.json.JsonArray;
import org.springframework.stereotype.Component;

@Component
public class Routes extends RouteBuilder {


    @Override
    public void configure() throws Exception {
        routeCheckTransactions();
    }

    private void routeCheckTransactions(){
        String id = "check-transactions";
        from("direct:"+id)
                .id(id)
                .log("Fetching transaction details")
                //set request params
                .to("/api/v1/batch/transactions")
                .process(exchange -> {
                    // get response body
                    // check successful transactions >= x%
                    // sample y% of successful transactions
                    // store the sampled request IDs in zeebe workflow variable
                });
    }
}
