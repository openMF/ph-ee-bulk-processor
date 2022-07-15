package org.mifos.processor.bulk.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public interface FileTransferService {

    String uploadFile(MultipartFile file, String bucketName);

    String uploadFile(File file, String bucketName);

    byte[] downloadFile(String fileName, String bucketName);

    void deleteFile(String fileName, String bucketName);

}
