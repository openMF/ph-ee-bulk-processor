package org.mifos.processor.bulk.utility;

import org.mifos.processor.bulk.schema.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransactionParser {

    private static final Logger logger = LoggerFactory.getLogger(TransactionParser.class);

    private TransactionParser() {
        throw new IllegalStateException("Utility class");
    }

    public static Transaction parseLineToTransaction(String line) {
        try {
            String[] parts = line.split(",", -1);
            Transaction transaction = new Transaction();

            if (parts.length > 0 && !parts[0].isEmpty()) {
                transaction.setId(Integer.parseInt(parts[0]));
            }
            if (parts.length > 1) {
                transaction.setRequestId(parts[1]);
            }
            if (parts.length > 2) {
                transaction.setPaymentMode(parts[2]);
            }
            if (parts.length > 4) {
                transaction.setPayerIdentifierType(parts[3]);
            }
            if (parts.length > 5) {
                transaction.setPayerIdentifier(parts[4]);
            }
            if (parts.length > 6) {
                transaction.setPayeeIdentifierType(parts[5]);
            }
            if (parts.length > 7) {
                transaction.setPayeeIdentifier(parts[6]);
            }
            if (parts.length > 8) {
                transaction.setAmount(parts[7]);
            }
            if (parts.length > 9) {
                transaction.setCurrency(parts[8]);
            }
            if (parts.length > 10) {
                transaction.setNote(parts[9]);
            }
            if (parts.length > 11) {
                transaction.setProgramShortCode(parts[10]);
            }
            if (parts.length > 12) {
                transaction.setCycle(parts[11]);
            }
            if (parts.length > 13) {
                transaction.setPayeeDfspId(parts[12]);
            }

            return transaction;
        } catch (Exception e) {
            logger.error("Error parsing line to Transaction object: {}", line, e);
            return null;
        }
    }
}
