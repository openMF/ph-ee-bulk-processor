package org.mifos.processor.bulk.camel.routes;

import org.mifos.processor.bulk.file.FileTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;

import static org.mifos.processor.bulk.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;

@Component
public class FileDownloadingRoute extends BaseRouteBuilder{

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Value("${application.bucket-name}")
    private String bucketName;

    @Override
    public void configure() throws Exception {
        from("direct:download-file")
                .id("download-file")
                .log("Started download-file route")
                .process(exchange -> {
                    String filename = exchange.getProperty(SERVER_FILE_NAME, String.class);

                    byte[] csvFile = fileTransferService.downloadFile(filename, bucketName);
                    File file = new File(filename);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(csvFile);
                    }
                    exchange.setProperty(LOCAL_FILE_PATH, file.getAbsolutePath());
                });
    }
}
