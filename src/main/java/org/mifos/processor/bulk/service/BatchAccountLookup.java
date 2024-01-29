package org.mifos.processor.bulk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.mifos.connector.common.identityaccountmapper.dto.AccountMapperRequestDTO;
import org.mifos.connector.common.identityaccountmapper.dto.BeneficiaryDTO;
import org.mifos.processor.bulk.api.definition.AccountLookupApi;
import org.mifos.processor.bulk.schema.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REGISTERING_INSTITUTION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;


public class BatchAccountLookup {
    @Autowired
    public ObjectMapper objectMapper;
    @Value("${identity_account_mapper.hostname}")
    private String identityURL;
    @Value("${identity_account_mapper.batch_account_lookup}")
    private String batchAccountLookup;

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    public void batchAccountLookupFunction(Exchange exchange) throws IOException {
        List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
        HashMap<String, List<Transaction>> stringListHashMap = new HashMap<>();
        List<BeneficiaryDTO> beneficiaryDTOList = new ArrayList<>();
        transactionList.forEach(transaction -> {
            beneficiaryDTOList.add(new BeneficiaryDTO(transaction.getPayeeIdentifier(), "", "", ""));
        });
        String requestId = exchange.getProperty(REQUEST_ID, String.class);
        String callbackUrl = exchange.getProperty(CALLBACK, String.class);
        String registeringInstitutionId = exchange.getProperty(HEADER_REGISTERING_INSTITUTE_ID, String.class);
        AccountMapperRequestDTO accountMapperRequestDTO = new AccountMapperRequestDTO(requestId, registeringInstitutionId,
                beneficiaryDTOList);
        String requestBody = objectMapper.writeValueAsString(accountMapperRequestDTO);

        exchange.getIn().setHeader(CALLBACK, callbackUrl);
        exchange.getIn().setHeader(REGISTERING_INSTITUTION_ID, registeringInstitutionId);
        exchange.getIn().setHeader("Content-type", "application/json");
        exchange.getIn().setBody(requestBody);
        logger.info("Account mapperDTO is : {}", accountMapperRequestDTO.toString());
        logger.info("identityURL is : {}", identityURL);
        logger.info("batchAccountLookup is : {}", batchAccountLookup);

        Map<String, Object> headers = new HashMap<>();
        headers = exchange.getIn().getHeaders();

        String fullUrl = identityURL + batchAccountLookup;
        makeApiCallUsingRetrofit(fullUrl, accountMapperRequestDTO, headers);
    }

    private void makeApiCallUsingRetrofit(String fullUrl, AccountMapperRequestDTO requestBody, Map<String, Object> headers) throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(identityURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AccountLookupApi accountLookupApi = retrofit.create(AccountLookupApi.class);

        Call<Object> call = accountLookupApi.batchAccountLookup(fullUrl, requestBody, convertHeaders(headers));

        try {
            retrofit2.Response<Object> response = call.execute();
            if (response.isSuccessful()) {
                Object apiResponse = response.body();
                logger.info("Retrofit API response is :: {}", apiResponse);
            } else {
                logger.error("Error occurred. HTTP status code: {}", response.code());
            }
        } catch (IOException e) {
            logger.error("Error making Retrofit API call", e);
            throw e;
        }
    }

    private Map<String, String> convertHeaders(Map<String, Object> headers) {
        Map<String, String> stringHeaders = new HashMap<>();
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            stringHeaders.put(entry.getKey(), entry.getValue().toString());
        }
        return stringHeaders;
    }
}
