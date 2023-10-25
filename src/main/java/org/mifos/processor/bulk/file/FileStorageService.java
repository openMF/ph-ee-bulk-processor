package org.mifos.processor.bulk.file;

import java.io.InputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileStorageService {

    String save(MultipartFile file);

    String save(InputStream file, String filename);

    String save(String data, String filename);

}
