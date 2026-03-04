package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionOlder extends Transaction {

    @JsonProperty("batchId")
    private String batchId;

}
