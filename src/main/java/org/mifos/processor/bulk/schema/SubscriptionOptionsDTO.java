package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionOptionsDTO {

    @JsonProperty("TTL")
    private int tTL;

    @JsonProperty("FIFO")
    private boolean fIFO;

    public static Builder builder = new Builder();

    public static class Builder {
        private Integer tTL;

        private Boolean fIFO;

        public Builder tTL(int tTL) {
            this.tTL = tTL;
            return this;
        }

        public Builder fIFO(boolean fIFO) {
            this.fIFO = fIFO;
            return this;
        }

        public SubscriptionOptionsDTO build() {
            if (this.tTL == null) {
                this.tTL = 3600;
            }
            if (this.fIFO == null) {
                this.fIFO = false;
            }
            return new SubscriptionOptionsDTO(this.tTL, this.fIFO);
        }
    }

}
