package org.mifos.processor.bulk;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.schema.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Deprecated
public class HealthCheck extends RouteBuilder {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CsvMapper csvMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${application.bucket-name}")
    private String bucketName;

    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int workerMaxJobs;

    @Override
    public void configure() {
        from("rest:GET:/")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant(""));

        from("rest:GET:/channel/bulk/transfer/{fileName}")
                .id("transfer-details")
                .log(LoggingLevel.INFO, "## CHANNEL -> inbound bulk transfer request with ${header.fileName}")
                .process(exchange -> {
                    String fileName = exchange.getIn().getHeader("fileName", String.class);
                    String batchId = UUID.randomUUID().toString();

                    // TODO: How to get sender information? Hard coded in Channel connector?
                    byte[] csvFile = fileTransferService.downloadFile(fileName, bucketName);

                    CsvSchema schema = CsvSchema.emptySchema().withHeader();
                    MappingIterator<Transaction> readValues = csvMapper.readerWithSchemaFor(Transaction.class).with(schema).readValues(csvFile);

                    /*while (readValues.hasNext()) {
                        Transaction current = readValues.next();
                        current.setBatchId(batchId);
                        System.out.println(objectMapper.writeValueAsString(current));
                        if (current.getPayment_mode().equals("gsma"))
                            kafkaTemplate.send(gsmaTopicName, objectMapper.writeValueAsString(current));
                        else if (current.getPayment_mode().equals("sclb"))
                            kafkaTemplate.send(slcbTopicName, objectMapper.writeValueAsString(current));
                    }*/
                })
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant(""));
    }
}
