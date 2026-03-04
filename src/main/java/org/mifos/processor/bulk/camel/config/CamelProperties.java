package org.mifos.processor.bulk.camel.config;

public final class CamelProperties {

    private CamelProperties() {}

    public static final String AUTH_TYPE = "authType";
    public static final String IS_BATCH_READY = "isBatchReady"; // camel property to check if batch is ready for
                                                                // sampling

    public static final String SERVER_FILE_NAME = "serverFileName";

    public static final String LOCAL_FILE_PATH = "localFilePath";

    public static final String LOCAL_FILE_PATH_LIST = "localFilePaths";

    public static final String SUB_BATCH_FILE_ARRAY = "subBatchFileArray";

    public static final String SUB_BATCH_COUNT = "subBatchCount";

    public static final String SUB_BATCH_CREATED = "subBatchCreated";
    public static final String SUB_BATCH_DETAILS = "subBatchDetails";

    public static final String SERVER_SUB_BATCH_FILE_NAME_ARRAY = "serverSubBatchFileName";

    public static final String TRANSACTION_LIST = "transactionList";

    public static final String TRANSACTION_LIST_LENGTH = "transactionListLength";

    public static final String TRANSACTION_LIST_ELEMENT = "transactionListElement";

    public static final String GSMA_CHANNEL_REQUEST = "gsmaChannelRequest";

    public static final String OVERRIDE_HEADER = "overrideHeader";

    public static final String TENANT_NAME = "tenantName";

    public static final String FILE_1 = "file1";

    public static final String FILE_2 = "file2";

    public static final String OPS_APP_ACCESS_TOKEN = "opsAppAccessToken";

    public static final String BATCH_STATUS_FAILED = "batchStatusFailed";

    public static final String CALLBACK_RESPONSE_CODE = "responseCode";

    public static final String BATCH_REQUEST_TYPE = "batchRequestType";

    public static final String RESULT_TRANSACTION_LIST = "resultTransactionList";

    public static final String ZEEBE_VARIABLE = "zeebeVariable";

    public static final String EXTERNAL_ENDPOINT_FAILED = "extEndpointFailed";

    public static final String EXTERNAL_ENDPOINT = "extEndpoint";

    public static final String PAYLOAD_LIST = "payloadList";

    public static final String IS_PAYMENT_MODE_VALID = "isPaymentModeValid";

    public static final String PAYMENT_MODE_TYPE = "paymentModeType";

    public static final String PAYLOAD = "payload";

    public static final String BATCH_ID_HEADER = "X-BatchID";
    public static final String HOST = "externalApiCallHost";
    public static final String ENDPOINT = "externalApiCallEndpoint";
    public static final String CACHED_TRANSACTION_ID = "cachedTransactionId";
    public static final String PAYEE_IDENTITY = "payeeIdentity";
    public static final String PAYMENT_MODALITY = "paymentModality";
    public static final String PAYEE_PARTY_ID = "payeePartyId";
    public static final String PAYEE_PARTY_ID_TYPE = "payeePartyIdType";
    public static final String HEADER_REGISTERING_INSTITUTE_ID = "X-Registering-Institution-ID";
    public static final String HEADER_PROGRAM_ID = "X-Program-ID";
    public static final String REGISTERING_INSTITUTE_ID = "registeringInstituteId";
    public static final String PROGRAM_ID = "programId";
    public static final String IS_UPDATED = "isUpdated";
    public static final String HEADER_PLATFORM_TENANT_ID = "Platform-TenantId";
    public static final String HEADER_CLIENT_CORRELATION_ID = "X-CorrelationID";
    public static final String CLIENT_CORRELATION_ID = "clientCorrelationId";
    public static final String SUB_BATCH_ENTITY = "subBatchEntity";
    public static final String EVENT_TYPE = "eventType";
    public static final String DUPLICATE_TRANSACTION_LIST = "duplicateTransactionList";
    public static final String ORIGINAL_TRANSACTION_LIST = "originalTransactionList";
    public static final String CALLBACK = "X-CallbackURL";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String PAYEE_DFSP_ID = "X-PayeeDFSP-ID";
}
