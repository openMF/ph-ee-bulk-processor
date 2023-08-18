package org.mifos.processor.bulk.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path root = Paths.get("");

    @Override
    public String save(MultipartFile file) {
        String filename = UUID.randomUUID() + "_" + Objects.requireNonNull(file.getOriginalFilename());
        try {
            Files.copy(file.getInputStream(), this.root.resolve(filename));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return filename;
    }
}
