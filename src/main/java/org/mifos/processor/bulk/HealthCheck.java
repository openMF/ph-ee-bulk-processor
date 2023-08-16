package org.mifos.processor.bulk;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.schema.TransactionOlder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
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

    @Value(value = "${kafka.topic.gsma.name}")
    private String gsmaTopicName;

    @Value(value = "${kafka.topic.slcb.name}")
    private String slcbTopicName;

    @Override
    public void configure() {
        from("rest:GET:/").setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200)).setBody(constant(""));

        from("rest:GET:/channel/bulk/transfer/{fileName}").id("transfer-details")
                .log(LoggingLevel.INFO, "## CHANNEL -> inbound bulk transfer request with ${header.fileName}").process(exchange -> {
                    String fileName = exchange.getIn().getHeader("fileName", String.class);
                    String batchId = UUID.randomUUID().toString();
                    exchange.setProperty(BATCH_ID, batchId);

                    // TODO: How to get sender information? Hard coded in Channel connector?
                    byte[] csvFile = fileTransferService.downloadFile(fileName, bucketName);

                    CsvSchema schema = CsvSchema.emptySchema().withHeader();
                    MappingIterator<TransactionOlder> readValues = csvMapper.readerWithSchemaFor(TransactionOlder.class).with(schema)
                            .readValues(csvFile);

                    while (readValues.hasNext()) {
                        TransactionOlder current = readValues.next();
                        current.setBatchId(batchId);
                        logger.info("Writing string in kafka {}", objectMapper.writeValueAsString(current));
                        if (current.getPaymentMode().equals("gsma") || current.getPaymentMode().equals("afrimoney")) {
                            kafkaTemplate.send(gsmaTopicName, objectMapper.writeValueAsString(current));
                        } else if (current.getPaymentMode().equals("sclb")) {
                            kafkaTemplate.send(slcbTopicName, objectMapper.writeValueAsString(current));
                        }
                    }
                }).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200)).setBody(exchange -> exchange.getProperty(BATCH_ID));
    }
}
