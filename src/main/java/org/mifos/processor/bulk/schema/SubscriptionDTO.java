package org.mifos.processor.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionDTO {

    private String id;

    private String roomClass;

    private String roomCode;

    private String srcServiceCode;

    private String srcOperationId;

    private String dstServiceCode;

    private String dstOperationId;

    private String delivery;

    private SubscriptionOptionsDTO options;

    public static Builder builder = new Builder();

    public static class Builder {
        private String id, roomClass, roomCode, srcServiceCode, srcOperationId,
                dstServiceCode, dstOperationId, delivery;
        private SubscriptionOptionsDTO options;

        public Builder roomClass(String roomClass) {
            this.roomClass = roomClass;
            return this;
        }

        public Builder roomCode(String roomCode) {
            this.roomCode = roomCode;
            return this;
        }

        public Builder srcServiceCode(String srcServiceCode) {
            this.srcServiceCode = srcServiceCode;
            return this;
        }

        public Builder srcOperationId(String srcOperationId) {
            this.srcOperationId = srcOperationId;
            return this;
        }

        public Builder dstServiceCode(String dstServiceCode) {
            this.dstServiceCode = dstServiceCode;
            return this;
        }

        public Builder dstOperationId(String dstOperationId) {
            this.dstOperationId = dstOperationId;
            return this;
        }

        public Builder delivery(String delivery) {
            this.delivery = delivery;
            return this;
        }

        public Builder options(SubscriptionOptionsDTO options) {
            this.options = options;
            return this;
        }

        private void check() {
            if (this.id == null) {
                this.id = UUID.randomUUID().toString();
            }
            if (this.options == null) {
                this.options = SubscriptionOptionsDTO.builder.build();
            }
            if (this.delivery == null) {
                this.delivery = "PUSH";
            }
            if (roomClass == null) {
                throw new RuntimeException("roomClass field cant be null");
            }
            if (roomCode == null) {
                throw new RuntimeException("roomCode field cant be null");
            }
            if (srcServiceCode == null) {
                throw new RuntimeException("srcServiceCode field cant be null");
            }
            if (srcOperationId == null) {
                throw new RuntimeException("srcOperationId field cant be null");
            }
            if (dstServiceCode == null) {
                throw new RuntimeException("dstServiceCode field cant be null");
            }
            if (dstOperationId == null) {
                throw new RuntimeException("dstOperationId field cant be null");
            }
        }

        public SubscriptionDTO build() {
            check();
            return new SubscriptionDTO(this.id,
                    this.roomClass, this.roomCode, this.srcServiceCode, this.srcOperationId,
                    this.dstServiceCode, this.dstOperationId, this.delivery, this.options);
        }
    }
}
