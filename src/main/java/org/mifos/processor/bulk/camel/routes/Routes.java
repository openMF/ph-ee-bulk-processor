package org.mifos.processor.bulk.camel.routes;


import com.google.gson.Gson;
import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.builder.RouteBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import static org.mifos.processor.bulk.camel.config.CamelProperties.IS_BATCH_READY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class Routes extends RouteBuilder {

    @Autowired
    ZeebeClient zeebeClient;

    @Value("${operations-app-config.host}")
    String operationsAppHost;

    @Value("${config.minimum-successful-tx-ratio}")
    double minimumSuccessfulTxRatio;

    @Override
    public void configure() throws Exception {
        routeCheckTransactions();
        routeSampleTransactions();
    }

    private void routeCheckTransactions() {
        String id = "check-transactions";
        from("direct:" + id)
                .id(id)
                .log("Fetching transaction details")
                //set request params
                .to(operationsAppHost+ "/api/v1/batch/transactions")
                .process(exchange -> {
                    // get response body
                    JSONObject transfers = new JSONObject(exchange.getIn().getBody(String.class));

                    int totalTransactions = transfers.length();
                    int successfulTransactions = 0;
                    for (Iterator<String> it = transfers.keys(); it.hasNext(); ) {
                        String transactionId = it.next();
                        String transactionStatus = transfers.getString(transactionId);
                        if (transactionStatus.equals("COMPLETED")) {
                            successfulTransactions++;
                        }
                    }

                    HashMap<String, Object> newVariables = new HashMap<>();
                    // check successful transactions >= x%
                    if (((double)successfulTransactions / totalTransactions) >= minimumSuccessfulTxRatio) {
                        newVariables.put(IS_SAMPLE_READY, true);
                    } else {
                        newVariables.put(IS_SAMPLE_READY, false);
                    }

                    zeebeClient.newSetVariablesCommand(Long.parseLong(exchange.getProperty(BATCH_ID).toString()))
                            .variables(newVariables)
                            .send()
                            .join();
                });
    }

    private void routeSampleTransactions() {
        String id = "sample-transactions";
        from("direct:" + id)
                .id(id)
                .log("Fetching transaction details")
                .process(exchange -> {
                    exchange.getIn().setHeader("batchId", exchange.getProperty(BATCH_ID));
                })
                .toD(operationsAppHost + "/api/v1/batch/transactions")
                .process(exchange -> {
                    // get response body

                    // check if batch is ready for sampling
                    if (exchange.getProperty(IS_BATCH_READY, String.class).equals("false")) {
                        return;
                    }
                    // sample transactions
                    JSONObject transfers = new JSONObject(exchange.getIn().getBody(String.class));
                    final ArrayList<String> successfulTransactionIds = new ArrayList<>();
                    final ArrayList<String> sampledTransactionIds = new ArrayList<>();
                    for (Iterator<String> it = transfers.keys(); it.hasNext(); ) {
                        String transactionId = it.next();
                        String transactionStatus = transfers.getString(transactionId);
                        if (transactionStatus.equals("COMPLETED")) {
                            successfulTransactionIds.add(transactionId);
                        }
                    }
                    Collections.shuffle(successfulTransactionIds);
                    int sampleSize = (int) (successfulTransactionIds.size() * 0.9);
                    for (int i = 0; i < sampleSize; i++) {
                        sampledTransactionIds.add(successfulTransactionIds.get(i));
                    }
                    HashMap<String, Object> newVariables = new HashMap<>();
                    newVariables.put(SAMPLED_TX_IDS, new Gson().toJson(sampledTransactionIds));

                    // store the sampled transaction ids in zeebe variable
                    zeebeClient.newSetVariablesCommand(Long.parseLong(exchange.getProperty(BATCH_ID).toString()))
                            .variables(newVariables)
                            .send()
                            .join();

                });
    }
}
