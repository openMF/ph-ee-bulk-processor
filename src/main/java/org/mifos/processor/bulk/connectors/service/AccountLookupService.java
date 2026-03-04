package org.mifos.processor.bulk.connectors.service;

import java.io.IOException;
import java.util.Map;
import org.mifos.connector.common.identityaccountmapper.dto.AccountMapperRequestDTO;
import org.mifos.processor.bulk.connectors.api.AccountLookupApi;
import org.mifos.processor.bulk.utility.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Retrofit;

@Service
public class AccountLookupService {

    @Autowired
    RetrofitService retrofitService;

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    public void accountLookupCall(String baseUrl, String fullUrl, AccountMapperRequestDTO requestBody, Map<String, Object> headers)
            throws IOException {
        Retrofit retrofit = retrofitService.createRetrofit(baseUrl);

        AccountLookupApi accountLookupApi = retrofit.create(AccountLookupApi.class);

        Call<Object> call = accountLookupApi.batchAccountLookup(fullUrl, requestBody, Headers.convertHeaders(headers));

        try {
            retrofit2.Response<Object> response = call.execute();
            if (response.isSuccessful()) {
                Object apiResponse = response.body();
                logger.debug("API response is :: {}", apiResponse);
            } else {
                logger.error("Error occurred. HTTP status code: {}", response.code());
            }
        } catch (IOException e) {
            logger.error("Error making Retrofit API call", e);
            throw e;
        }
    }
}
