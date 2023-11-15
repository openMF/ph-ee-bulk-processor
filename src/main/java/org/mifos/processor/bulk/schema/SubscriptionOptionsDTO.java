package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionOptionsDTO {

    @JsonProperty("TTL")
    private int ttl;

    @JsonProperty("FIFO")
    private boolean fifo;

    public static SubscriptionOptionsDTOBuilder subscriptionOptionsDTOBuilder = new SubscriptionOptionsDTOBuilder();

    public static class SubscriptionOptionsDTOBuilder {

        private Integer ttl;

        private Boolean fifo;

        public SubscriptionOptionsDTOBuilder tTL(int tTL) {
            this.ttl = tTL;
            return this;
        }

        public SubscriptionOptionsDTOBuilder fIFO(boolean fIFO) {
            this.fifo = fIFO;
            return this;
        }

        public SubscriptionOptionsDTO build() {
            if (this.ttl == null) {
                this.ttl = 3600;
            }
            if (this.fifo == null) {
                this.fifo = false;
            }
            return new SubscriptionOptionsDTO(this.ttl, this.fifo);
        }
    }

}
