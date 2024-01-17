package org.mifos.processor.exceptionmapper;

import io.camunda.zeebe.client.api.command.ClientStatusException;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionMapperDTO> handleException(Exception ex) {
        ExceptionMapperDTO dto = new ExceptionMapperDTO("01", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(dto);
    }
}
