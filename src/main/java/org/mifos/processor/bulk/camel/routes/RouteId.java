package org.mifos.processor.bulk.camel.routes;

public enum RouteId {

    PARTY_LOOKUP("direct:partyLookup"), APPROVAL("direct:approval"), ORDERING("direct:ordering"), SPLITTING("direct:splitting"), FORMATTING(
            "direct:formatting"), BATCH_STATUS("direct:batchStatus"), SEND_CALLBACK(
                    "direct:sendCallback"), MERGE_BACK("direct:mergeSubBatch"), INIT_SUB_BATCH("direct:init-sub-batches");

    private final String value;

    private RouteId(String s) {
        value = s;
    }

    public String getValue() {
        return value;
    }
}
