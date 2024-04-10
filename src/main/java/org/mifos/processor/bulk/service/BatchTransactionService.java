package org.mifos.processor.bulk.service;

import org.mifos.processor.bulk.exception.ConflictingDataException;
import org.mifos.processor.bulk.repository.BatchesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BatchTransactionService {

    private static final Logger log = LoggerFactory.getLogger(BatchTransactionService.class);
    @Autowired
    BatchesRepository batchesRepository;

    public void validateClientCorrelationID(String clientCorrelationId) {
        if (batchesRepository.existsByCorrelationId(clientCorrelationId)) {
            throw new ConflictingDataException("clientCorrelationId", clientCorrelationId);
        }

    }
}
