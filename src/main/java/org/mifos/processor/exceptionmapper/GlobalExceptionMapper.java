package org.mifos.processor.exceptionmapper;

import io.camunda.zeebe.client.api.command.ClientStatusException;
import org.mifos.processor.bulk.exception.ConflictingDataException;
import org.mifos.processor.bulk.schema.ConflictResponseDTO;
import org.mifos.processor.bulk.schema.ExceptionMapperDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionMapper {

    @ExceptionHandler(ClientStatusException.class)
    public ResponseEntity<ExceptionMapperDTO> handleClientStatusException(ClientStatusException ex) {
        ExceptionMapperDTO dto = new ExceptionMapperDTO("01", "Process definition not found");
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).contentType(MediaType.APPLICATION_JSON).body(dto);
    }

    @ExceptionHandler(ConflictingDataException.class)
    public ResponseEntity<ConflictResponseDTO> handleConflictingDataException(ConflictingDataException ex) {
        ConflictResponseDTO dto = new ConflictResponseDTO("01", "Conflict occurred due to existing data", ex.getConflictingFieldName(),
                ex.getConflictingFieldValue());
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(dto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionMapperDTO> handleException(Exception ex) {
        ExceptionMapperDTO dto = new ExceptionMapperDTO("01", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(dto);
    }
}
