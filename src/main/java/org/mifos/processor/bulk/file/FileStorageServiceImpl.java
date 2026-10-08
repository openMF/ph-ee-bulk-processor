package org.mifos.processor.bulk.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private final Path root = Paths.get("");

    @Override
    public String save(MultipartFile file) {
        String filename = UUID.randomUUID() + "_" + Objects.requireNonNull(file.getOriginalFilename());
        logger.info("FileStorageServiceImpl.save() - filename: {}", filename);
        logger.info("FileStorageServiceImpl.save() - file.getSize(): {}", file.getSize());
        logger.info("FileStorageServiceImpl.save() - file.isEmpty(): {}", file.isEmpty());
        try {
            InputStream inputStream = file.getInputStream();
            logger.info("FileStorageServiceImpl.save() - inputStream available: {}", inputStream.available());
            long bytesCopied = Files.copy(inputStream, this.root.resolve(filename));
            logger.info("FileStorageServiceImpl.save() - bytesCopied: {}", bytesCopied);
        } catch (Exception e) {
            logger.error("FileStorageServiceImpl.save() - Exception: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
        return filename;
    }

    @Override
    public String save(InputStream inputStream, String filename) {
        String uniqueFileName = getUniqueFileName(filename);
        try {
            Files.copy(inputStream, this.root.resolve(uniqueFileName));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
        return uniqueFileName;
    }

    @Override
    public String save(String data, String filename) {
        String uniqueFileName = getUniqueFileName(filename);
        try {
            Files.writeString(this.root.resolve(uniqueFileName), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return uniqueFileName;
    }

    private String getUniqueFileName(String filename) {
        return UUID.randomUUID() + "_" + Objects.requireNonNull(filename);
    }
}
