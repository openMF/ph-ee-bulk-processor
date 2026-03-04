package org.mifos.processor.bulk.connectors.api;

import java.util.Map;
import org.mifos.connector.common.identityaccountmapper.dto.AccountMapperRequestDTO;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Url;

@Component
public interface AccountLookupApi {

    @POST
    Call<Object> batchAccountLookup(@Url String fullUrl, @Body AccountMapperRequestDTO requestBody, @HeaderMap Map<String, String> headers);
}
