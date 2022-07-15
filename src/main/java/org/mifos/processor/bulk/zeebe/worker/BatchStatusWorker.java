package org.mifos.processor.bulk.zeebe.worker;

public class BatchStatusWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.BATCH_STATUS, (client, job) -> {
            client.newCompleteCommand(job.getKey()).send();
        });
    }

}
