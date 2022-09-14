package org.mifos.processor.bulk.format.helper;

import org.mifos.processor.bulk.schema.GSMATransaction;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.stereotype.Component;

@Component
public class GSMAMapper extends BaseMapper<Transaction, GSMATransaction> {
    @Override
    public GSMATransaction convert(Transaction object) {
        GSMATransaction gsmaTransaction = new GSMATransaction();
        gsmaTransaction.setId(object.getId());
        gsmaTransaction.setRequestId("test");
        gsmaTransaction.setPaymentMode(object.getPaymentMode());
        gsmaTransaction.setAccountNumber(object.getAccountNumber());
        gsmaTransaction.setAmount(object.getAmount());
        gsmaTransaction.setNote(object.getNote());
        return gsmaTransaction;
    }
}
