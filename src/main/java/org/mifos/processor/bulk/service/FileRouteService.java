package org.mifos.processor.bulk.service;

import static org.mifos.processor.bulk.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.mifos.processor.bulk.file.FileTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FileRouteService {

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;
    @Value("${application.bucket-name}")
    private String bucketName;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void downloadFile(Exchange exchange) throws IOException {
        String filename = exchange.getProperty(SERVER_FILE_NAME, String.class);

        byte[] csvFile = fileTransferService.downloadFile(filename, bucketName);
        File file = new File(filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(csvFile);
        }
        exchange.setProperty(LOCAL_FILE_PATH, file.getAbsolutePath());
        logger.info("File downloaded");
    }

    public void uploadFile(Exchange exchange) {
        String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
        String serverFileName = fileTransferService.uploadFile(new File(filepath), bucketName);
        exchange.setProperty(SERVER_FILE_NAME, serverFileName);
        logger.info("Uploaded file: {}", serverFileName);
    }
}
