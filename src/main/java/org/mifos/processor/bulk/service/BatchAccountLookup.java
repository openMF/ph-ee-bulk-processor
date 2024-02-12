package org.mifos.processor.bulk.service;

import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REGISTERING_INSTITUTION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.mifos.connector.common.identityaccountmapper.dto.AccountMapperRequestDTO;
import org.mifos.connector.common.identityaccountmapper.dto.BeneficiaryDTO;
import org.mifos.processor.bulk.connectors.service.AccountLookupService;
import org.mifos.processor.bulk.schema.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class BatchAccountLookup {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccountLookupService accountLookupService;
    @Value("${identity_account_mapper.hostname}")
    private String identityEndpoint;
    @Value("${identity_account_mapper.batch_account_lookup}")
    private String batchAccountLookup;

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    public void doBatchAccountLookup(Exchange exchange) throws IOException {
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

        Map<String, Object> headers = new HashMap<>();
        headers = exchange.getIn().getHeaders();

        String fullUrl = identityEndpoint + batchAccountLookup;
        accountLookupService.accountLookupCall(identityEndpoint, fullUrl, accountMapperRequestDTO, headers);
    }
}
