package org.mifos.processor.bulk.zeebe.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkerConfig {

    @Value("${config.partylookup}")
    public boolean isPartyLookUpWorkerEnabled;

    @Value("${config.approval}")
    public boolean isApprovalWorkerEnabled;

    @Value("${config.ordering}")
    public boolean isOrderingWorkerEnabled;

    @Value("${config.splitting}")
    public boolean isSplittingWorkerEnabled;

    @Value("${config.formatting}")
    public boolean isFormattingWorkerEnabled;

    @Value("${config.mergeback}")
    public boolean isMergeBackWorkerEnabled;


}
