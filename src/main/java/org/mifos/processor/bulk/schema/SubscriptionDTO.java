package org.mifos.processor.bulk.schema;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public static SubscriptionDTOBuilder subscriptionDTOBuilder = new SubscriptionDTOBuilder();

    public static class SubscriptionDTOBuilder {

        private String id;
        private String roomClass;
        private String roomCode;
        private String srcServiceCode;
        private String srcOperationId;
        private String dstServiceCode;
        private String dstOperationId;
        private String delivery;
        private SubscriptionOptionsDTO options;

        public SubscriptionDTOBuilder roomClass(String roomClass) {
            this.roomClass = roomClass;
            return this;
        }

        public SubscriptionDTOBuilder roomCode(String roomCode) {
            this.roomCode = roomCode;
            return this;
        }

        public SubscriptionDTOBuilder srcServiceCode(String srcServiceCode) {
            this.srcServiceCode = srcServiceCode;
            return this;
        }

        public SubscriptionDTOBuilder srcOperationId(String srcOperationId) {
            this.srcOperationId = srcOperationId;
            return this;
        }

        public SubscriptionDTOBuilder dstServiceCode(String dstServiceCode) {
            this.dstServiceCode = dstServiceCode;
            return this;
        }

        public SubscriptionDTOBuilder dstOperationId(String dstOperationId) {
            this.dstOperationId = dstOperationId;
            return this;
        }

        public SubscriptionDTOBuilder delivery(String delivery) {
            this.delivery = delivery;
            return this;
        }

        public SubscriptionDTOBuilder options(SubscriptionOptionsDTO options) {
            this.options = options;
            return this;
        }

        private void check() {
            if (this.id == null) {
                this.id = UUID.randomUUID().toString();
            }
            if (this.options == null) {
                this.options = SubscriptionOptionsDTO.subscriptionOptionsDTOBuilder.build();
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
            return new SubscriptionDTO(this.id, this.roomClass, this.roomCode, this.srcServiceCode, this.srcOperationId,
                    this.dstServiceCode, this.dstOperationId, this.delivery, this.options);
        }
    }
}
