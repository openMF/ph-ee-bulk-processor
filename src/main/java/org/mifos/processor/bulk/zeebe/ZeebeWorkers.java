package org.mifos.processor.bulk.zeebe;

import static org.mifos.processor.bulk.camel.config.CamelProperties.IS_BATCH_READY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.IS_SAMPLE_READY;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.camunda.zeebe.client.ZeebeClient;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.schema.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Deprecated
public class ZeebeWorkers {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private CsvMapper csvMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Value("${application.bucket-name}")
    private String bucketName;

    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int workerMaxJobs;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    // @PostConstruct
    public void setupWorkers() {
        workerBulkProcessor();
        workerCheckTransactions();
        workerSampleTransactions();
    }

    private void workerBulkProcessor() {
        zeebeClient.newWorker().jobType("bulk-processor").handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

            Map<String, Object> variables = job.getVariablesAsMap();
            String batchId = (String) variables.get(BATCH_ID);
            String fileName = (String) variables.get("fileName");

            // TODO: How to get sender information? Hard coded in Channel connector?
            byte[] csvFile = fileTransferService.downloadFile(fileName, bucketName);

            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Transaction> readValues = csvMapper.readerWithSchemaFor(Transaction.class).with(schema).readValues(csvFile);

            /*
             * while (readValues.hasNext()) { Transaction current = readValues.next(); current.setBatchId(batchId); if
             * (current.getPayment_mode().equals("gsma")) kafkaTemplate.send(gsmaTopicName,
             * objectMapper.writeValueAsString(current)); else if (current.getPayment_mode().equals("sclb"))
             * kafkaTemplate.send(slcbTopicName, objectMapper.writeValueAsString(current)); }
             */

            client.newCompleteCommand(job.getKey()).send();
        }).name("bulk-processor").maxJobsActive(workerMaxJobs).open();
    }

    private void workerCheckTransactions() {
        String jobType = "check-transactions";
        zeebeClient.newWorker().jobType(jobType).handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

            Map<String, Object> variables = job.getVariablesAsMap();
            String batchId = (String) variables.get(BATCH_ID);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(BATCH_ID, batchId);
            producerTemplate.send("direct:check-transactions", exchange);

            client.newCompleteCommand(job.getKey()).send();
        }).name(jobType).maxJobsActive(workerMaxJobs).open();
    }

    private void workerSampleTransactions() {
        String jobType = "sample-transactions";
        zeebeClient.newWorker().jobType(jobType).handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

            Map<String, Object> variables = job.getVariablesAsMap();
            String batchId = (String) variables.get(BATCH_ID);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(BATCH_ID, batchId);
            exchange.setProperty(IS_BATCH_READY, variables.get(IS_SAMPLE_READY));
            producerTemplate.send("direct:sample-transactions", exchange);

            client.newCompleteCommand(job.getKey()).send();
        }).name(jobType).maxJobsActive(workerMaxJobs).open();
    }
}
