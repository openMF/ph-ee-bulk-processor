package org.mifos.processor.bulk.connectors;

import lombok.extern.slf4j.Slf4j;
import org.mifos.processor.bulk.file.FileTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class FileRoute {

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Value("${application.bucket-name}")
    private String bucketName;

    public String downloadFile(String filename) throws FileNotFoundException {
        byte[] csvFile = fileTransferService.downloadFile(filename, bucketName);
        File file = new File(filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(csvFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to download the file", e);
        }
        log.info("File downloaded");
        return file.getAbsolutePath();
    }

    public String uploadFile(String filepath) {
        String serverFileName = fileTransferService.uploadFile(new File(filepath), bucketName);
        log.info("Uploaded file: {}", serverFileName);
        return serverFileName;
    }
}