package org.mifos.processor.bulk.format;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.mifos.processor.bulk.schema.BatchRequestDTO;
import org.mifos.processor.bulk.schema.Party;
import org.mifos.processor.bulk.schema.PartyType;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.stereotype.Component;

@Component
public class RestRequestConvertor implements EntityMapper<BatchRequestDTO, Transaction> {

    @Override
    public BatchRequestDTO convertTo(Transaction object) {
        Party creditParty = new Party();
        creditParty.setKey(PartyType.fromValue(object.getPayeeIdentifierType()).getValue());
        creditParty.setValue(object.getPayeeIdentifier());

        Party debitParty = new Party();
        debitParty.setKey(PartyType.fromValue(object.getPayerIdentifierType()).getValue());
        debitParty.setValue(object.getPayerIdentifier());

        // creating the DTO
        BatchRequestDTO batchRequestDTO = new BatchRequestDTO();
        batchRequestDTO.setAmount(object.getAmount());
        batchRequestDTO.setCurrency(object.getCurrency());
        batchRequestDTO.setCreditParty(List.of(creditParty));
        batchRequestDTO.setDebitParty(List.of(debitParty));
        batchRequestDTO.setDescriptionText(object.getNote());
        batchRequestDTO.setSubType(object.getPaymentMode());

        return batchRequestDTO;
    }

    @Override
    public Transaction convertFrom(BatchRequestDTO object) {

        // creating the transaction
        Transaction transaction = new Transaction();
        transaction.setCurrency(object.getCurrency());
        transaction.setAmount(object.getAmount());
        if (object.getDebitParty() != null && object.getDebitParty().size() > 0) {
            transaction.setPayerIdentifierType(object.getDebitParty().get(0).getKey());
            transaction.setPayerIdentifier(object.getDebitParty().get(0).getValue());
        }
        if (object.getCreditParty() != null && object.getCreditParty().size() > 0) {
            transaction.setPayeeIdentifierType(object.getCreditParty().get(0).getKey());
            transaction.setPayeeIdentifier(object.getCreditParty().get(0).getValue());
        }
        transaction.setNote(object.getDescriptionText());
        transaction.setPaymentMode(object.getSubType());

        return transaction;
    }

    @Override
    public List<BatchRequestDTO> convertListTo(List<Transaction> objects) {
        List<BatchRequestDTO> batchRequestDTOList = new ArrayList<>();
        objects.forEach(e -> batchRequestDTOList.add(convertTo(e)));
        return batchRequestDTOList;
    }

    @Override
    public List<Transaction> convertListFrom(List<BatchRequestDTO> objects) {
        List<Transaction> transactionList = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            Transaction transaction = convertFrom(objects.get(i));
            transaction.setId(i);
            transaction.setRequestId(UUID.randomUUID().toString());
            transactionList.add(transaction);
        }
        return transactionList;
    }

}
