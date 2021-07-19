package org.mifos.processor.bulk.zeebe;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.zeebe.client.ZeebeClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.schema.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
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

    @PostConstruct
    public void setupWorkers() {

        zeebeClient.newWorker()
                .jobType("bulk-processor")
                .handler((client, job) -> {
                    logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

                    Map<String, Object> variables = job.getVariablesAsMap();
                    String batchId = (String) variables.get("batchId");
                    String fileName = (String) variables.get("fileName");

                    // TODO: How to get sender information? Hard coded in Channel connector?
                    byte[] csvFile = fileTransferService.downloadFile(fileName, bucketName);

                    CsvSchema schema = CsvSchema.emptySchema().withHeader();
                    MappingIterator<Transaction> readValues = csvMapper.readerWithSchemaFor(Transaction.class).with(schema).readValues(csvFile);

                    while (readValues.hasNext()) {
                        Transaction current = readValues.next();
//                        System.out.println(objectMapper.writeValueAsString(current));
                        if (current.getPayment_mode().equals("gsma"))
                            kafkaTemplate.send(gsmaTopicName, objectMapper.writeValueAsString(current));
                        else if (current.getPayment_mode().equals("sclb"))
                            kafkaTemplate.send(slcbTopicName, objectMapper.writeValueAsString(current));
                    }

                    client.newCompleteCommand(job.getKey())
                            .send();
                })
                .name("bulk-processor")
                .maxJobsActive(workerMaxJobs)
                .open();

    }
}