package org.mifos.processor.bulk.camel.config;

public class CamelProperties {

    private CamelProperties() {}

    public static final String AUTH_TYPE = "authType";
    public static final String IS_BATCH_READY = "isBatchReady"; // camel property to check if batch is ready for sampling

    public static final String SERVER_FILE_NAME = "serverFileName";

    public static final String LOCAL_FILE_PATH = "localFilePath";

    public static final String LOCAL_FILE_PATH_LIST = "localFilePaths";

    public static final String SUB_BATCH_FILE_ARRAY = "subBatchFileArray";

    public static final String SUB_BATCH_COUNT = "subBatchCount";

    public static final String SUB_BATCH_CREATED = "subBatchCreated";

    public static final String SERVER_SUB_BATCH_FILE_NAME_ARRAY = "serverSubBatchFileName";

    public static final String TRANSACTION_LIST = "transactionList";

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

}
