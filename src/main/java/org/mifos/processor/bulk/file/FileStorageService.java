package org.mifos.processor.bulk.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public interface FileStorageService {

    public String save(MultipartFile file);

    String save(InputStream file, String filename);

    String save(String data, String filename);

}
