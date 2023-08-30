package org.mifos.processor.bulk.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;

@Service
public interface FileTransferService {

    byte[] downloadFile(String fileName, String bucketName);

    String uploadFile(MultipartFile file, String bucketName);

    String uploadFile(File file, String bucketName);

    InputStream streamFile(String fileName, String bucketName);

    void deleteFile(String fileName, String bucketName);

}
