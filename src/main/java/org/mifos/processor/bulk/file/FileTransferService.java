package org.mifos.processor.bulk.file;

import java.io.File;
import java.io.InputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileTransferService {

    byte[] downloadFile(String fileName, String bucketName);

    String uploadFile(MultipartFile file, String bucketName);

    String uploadFile(File file, String bucketName);

    InputStream streamFile(String fileName, String bucketName);

    void deleteFile(String fileName, String bucketName);

}
