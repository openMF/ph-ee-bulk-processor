package org.mifos.processor.bulk.zeebe;

public class ZeebeVariables {

    private ZeebeVariables() {}

    public static final String ACCOUNT = "account";
    public static final String AUTH_RETRIES_LEFT = "authRetriesLeft";
    public static final String CHANNEL_REQUEST = "channelRequest";
    public static final String ERROR_INFORMATION = "errorInformation";
    public static final String IS_AUTHORISATION_REQUIRED = "isAuthorisationRequired";
    public static final String IS_RTP_REQUEST = "isRtpRequest";
    public static final String OPERATOR_MANUAL_OVERRIDE = "operatorManualOverride"; // TODO validate in request?
    public static final String ORIGIN_DATE = "originDate";
    public static final String PARTY_ID = "partyId";
    public static final String PARTY_ID_TYPE = "partyIdType";
    public static final String TENANT_ID = "tenantId";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String GSMA_CHANNEL_REQUEST = "gsmaChannelRequest";
    public static final String PARTY_LOOKUP_FSPID = "partyLookupFspId";
    public static final String INITIATOR_FSPID = "initiatorFspId";
    public static final String TRANSACTION_TYPE = "transactionType";
    public static final String BATCH_ID = "batchId";
    public static final String SUB_BATCH_ID = "subBatchId";
    public static final String IS_SAMPLE_READY = "isSampleReady";
    public static final String SAMPLED_TX_IDS = "sampledTransactionIds";

    public static final String PARTY_LOOKUP_FAILED = "partyLookupFailed";
    public static final String APPROVAL_FAILED = "approvalFailed";
    public static final String ORDERING_FAILED = "orderingFailed";
    public static final String SPLITTING_FAILED = "splittingFailed";
    public static final String FORMATTING_FAILED = "formattingFailed";
    public static final String INIT_SUB_BATCH_FAILED = "initSubBatchFailed";
    public static final String MERGE_FAILED = "mergeFailed";

    public static final String FILE_NAME = "filename";

    public static final String REQUEST_ID = "requestId";

    public static final String SUB_BATCHES = "subBatches";
    public static final String PURPOSE = "purpose";

    public static final String INIT_SUCCESS_SUB_BATCHES = "initSuccessSubBatches";

    public static final String INIT_FAILURE_SUB_BATCHES = "initFailureSubBatches";

    public static final String PARTY_LOOKUP_ENABLED = "partyLookupEnabled";

    public static final String APPROVAL_ENABLED = "approvalEnabled";

    public static final String ORDERING_ENABLED = "orderingEnabled";

    public static final String SPLITTING_ENABLED = "splittingEnabled";

    public static final String FORMATTING_ENABLED = "formattingEnabled";

    public static final String COMPLETION_THRESHOLD_CHECK_ENABLED = "completionThresholdCheckEnabled";

    public static final String MERGE_ENABLED = "mergeEnabled";

    public static final String ORDERED_BY = "orderedBy";

    public static final String FORMATTING_STANDARD = "formattingStandard";

    public static final String REMAINING_SUB_BATCH = "remainingSubBatch";

    public static final String TRANSACTION_REQUEST = "transactionRequest";

    public static final String TOTAL_AMOUNT = "totalAmount";

    public static final String ONGOING_AMOUNT = "ongoingAmount";

    public static final String FAILED_AMOUNT = "failedAmount";

    public static final String COMPLETED_AMOUNT = "completedAmount";

    public static final String MERGE_FILE_LIST = "mergeFiles";

    public static final String MERGE_ITERATION = "mergeIteration";

    public static final String MERGE_COMPLETED = "mergeCompleted";

    public static final String RESULT_FILE = "resultFile";

    public static final String MAX_STATUS_RETRY = "maxStatusRetry";

    public static final String RETRY = "retry";

    public static final String CALLBACK_RETRY = "callbackRetryCount";

    public static final String COMPLETION_THRESHOLD = "completionThreshold";

    public static final String COMPLETION_RATE = "completionRate";

    public static final String ERROR_CODE = "errorCode";

    public static final String ERROR_DESCRIPTION = "errorDescription";

    public static final String THRESHOLD_DELAY = "thresholdDelay";

    public static final String PAYMENT_MODE = "paymentMode";

    public static final String CALLBACK_SUCCESS = "callbackSuccessful";

    public static final String CALLBACK_URL = "callbackUrl";

    public static final String MAX_CALLBACK_RETRY = "maxCallbackRetry";

    public static final String BULK_NOTIF_SUCCESS = "isNotificationsSuccessEnabled";

    public static final String BULK_NOTIF_FAILURE = "isNotificationsFailureEnabled";

    public static final String PHASES = "phases";

    public static final String PHASE_COUNT = "phaseCount";

    public static final String INITIATOR_FSP_ID = "initiatorFspId";
    public static final String ACCOUNT_LOOKUP_RETRY_COUNT = "accountLookupRetryCount";
    public static final String ACCOUNT_LOOKUP_FAILED = "accountLookupFailed";
    public static final String ORIGIN_CHANNEL_REQUEST = "originChannelRequest";
    public static final String CALLBACK = "X-CallbackURL";

    public static final String DEBULKINGDFSPID = "debulkingDfspid";

    public static final String FILE_VALIDITY = "isFileValid";

    public static final String NOTE = "note";
    public static final String PARTY_LOOKUP_FSP_ID = "partyLookupFspId";

}
