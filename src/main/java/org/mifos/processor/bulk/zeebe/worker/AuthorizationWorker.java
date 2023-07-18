package org.mifos.processor.bulk.zeebe.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.schema.AuthorizationRequest;
import org.mifos.processor.bulk.schema.AuthorizationResponse;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class AuthorizationWorker extends BaseWorker{

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Value("${application.bucket-name}")
    private String bucketName;

    private static final String AUTHORIZATION_SUCCESSFUL = "authorizationSuccessful";
    private static final String AUTHORIZATION_FAIL_REASON = "authorizationFailReason";

    @Override
    public void setup() {
        newWorker(Worker.AUTHORIZATION, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            if (!workerConfig.isAuthorizationWorkerEnabled) {
                variables.put(AUTHORIZATION_SUCCESSFUL, true);
                client.newCompleteCommand(job.getKey()).variables(variables).send();
                return;
            }

            String batchId = (String) variables.get(BATCH_ID);
            String fileName = (String) variables.get(FILE_NAME);
            String clientCorrelationId = (String) variables.get("clientCorrelationId");

            List<Transaction> transactionList = fetchTransactionList(fileName);
            String amount = calculateTotalAmountToBeTransferred(transactionList);
            String currency = getCurrencyFromFirstTransaction(transactionList.get(0));
            String payerIdentifier = getPayerIdentifierFromFirstTransaction(transactionList.get(0));

            AuthorizationRequest requestPayload = new AuthorizationRequest(batchId, payerIdentifier, currency, amount);
            AuthorizationResponse authorizationResponse = invokeBatchAuthorizationApi(batchId,
                                                            requestPayload, clientCorrelationId);

            String expectedAuthorizationStatus = "Y";
            boolean isAuthorizationSuccessful = Objects.nonNull(authorizationResponse) &&
                                                expectedAuthorizationStatus.equals(authorizationResponse.getStatus());
            variables.put(AUTHORIZATION_SUCCESSFUL, isAuthorizationSuccessful);

            if(!isAuthorizationSuccessful){
                variables.put(AUTHORIZATION_FAIL_REASON, authorizationResponse.getReason());
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

    private List<Transaction> fetchTransactionList(String fileName){
        byte[] fileInBytes = fileTransferService.downloadFile(fileName, bucketName);
        String csvData = new String(fileInBytes);
        return parseCSVDataToList(csvData);
    }

    private List<Transaction> parseCSVDataToList(String csvData) {
        List<Transaction> transactionList = new ArrayList<>();
        String[] lines = csvData.split("\n");

        for(int i=1; i<lines.length; i++){
            String transactionString = lines[i];
            String[] transactionFields = transactionString.split(",");

            Transaction transaction = new Transaction();
            transaction.setId(Integer.parseInt(transactionFields[0]));
            transaction.setRequestId(transactionFields[1]);
            transaction.setPaymentMode(transactionFields[2]);
            transaction.setPayerIdentifierType(transactionFields[3]);
            transaction.setPayerIdentifier(transactionFields[4]);
            transaction.setPayeeIdentifierType(transactionFields[5]);
            transaction.setPayeeIdentifier(transactionFields[6]);
            transaction.setAmount(transactionFields[7]);
            transaction.setCurrency(transactionFields[8]);
            transaction.setNote(transactionFields[9]);
            transactionList.add(transaction);
        }
        return transactionList;
    }

    private String getCurrencyFromFirstTransaction(Transaction transaction){
        return transaction.getCurrency();
    }

    private String getPayerIdentifierFromFirstTransaction(Transaction transaction){
        return transaction.getPayerIdentifier();
    }

    private String calculateTotalAmountToBeTransferred(List<Transaction> transactionList) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for(Transaction transaction : transactionList){
            totalAmount = totalAmount.add(new BigDecimal(transaction.getAmount()));
        }
        return totalAmount.toPlainString();
    }

    private AuthorizationResponse invokeBatchAuthorizationApi(String batchId, AuthorizationRequest requestPayload,
                                                              String clientCorrelationId) {
        RestTemplate restTemplate = new RestTemplate();
        AuthorizationResponse authResponse = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Client-Correlation-ID", clientCorrelationId);

        HttpEntity<AuthorizationRequest> requestEntity = new HttpEntity<>(requestPayload, headers);
        String endpoint = "/batches/" + batchId;

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            authResponse = objectMapper.readValue(responseEntity.getBody(), AuthorizationResponse.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return authResponse;
    }
}
