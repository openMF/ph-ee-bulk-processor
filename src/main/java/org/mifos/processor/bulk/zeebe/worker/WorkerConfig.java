package org.mifos.processor.bulk.zeebe.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkerConfig {

    @Value("${config.partylookup.enable}")
    public boolean isPartyLookUpWorkerEnabled;

    @Value("${config.approval.enable}")
    public boolean isApprovalWorkerEnabled;

    @Value("${config.ordering.enable}")
    public boolean isOrderingWorkerEnabled;

    @Value("${config.splitting.enable}")
    public boolean isSplittingWorkerEnabled;

    @Value("${config.formatting.enable}")
    public boolean isFormattingWorkerEnabled;

    @Value("${config.mergeback.enable}")
    public boolean isMergeBackWorkerEnabled;

    @Value("${config.completion-threshold-check.enable}")
    public boolean isCompletionThresholdCheckEnabled;

    @Value("${config.deduplication.enabled}")
    public boolean isTransactionDeduplicationEnabled;

}
