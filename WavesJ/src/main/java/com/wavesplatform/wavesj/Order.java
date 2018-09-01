package com.wavesplatform.wavesj;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {
    public enum Type {
        BUY, SELL;

        @JsonValue
        public String toJson() {
            return toString().toLowerCase();
        }

        @JsonCreator
        static Type fromString(String json) {
            return json == null ? null : Type.valueOf(json.toUpperCase());
        }
    }

    public enum Status {
        ACCEPTED, FILLED, PARTIALLY_FILLED, CANCELED, NOT_FOUND;

        @JsonCreator
        static Status fromString(String json) {
            if (json == null) return null;
            json = json.intern();
            if (json == "Accepted" || json == "OrderAccepted") {
                return ACCEPTED;
            } else if (json == "Filled") {
                return FILLED;
            } else if (json == "PartiallyFilled") {
                return PARTIALLY_FILLED;
            } else if (json ==  "Cancelled") {
                return CANCELED;
            } else if (json ==  "NotFound") {
                return NOT_FOUND;
            } else {
                throw new IllegalArgumentException("Bad status value: " + json);
            }
        }

        public boolean isActive() {
            return this == ACCEPTED || this == PARTIALLY_FILLED;
        }
    }

    @JsonAlias({"orderType"})
    public final Type type;
    public final String id;
    public final long amount;
    public final long price;
    public final long filled;
    public final long timestamp;
    public final Status status;
    public final AssetPair assetPair;

    @JsonCreator
    private Order(
            @JsonProperty("id") String id,
            @JsonProperty("type") Type type,
            @JsonProperty("assetPair") AssetPair assetPair,
            @JsonProperty("amount") long amount,
            @JsonProperty("price") long price,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("filled") long filled,
            @JsonProperty("status") Status status) {
        this.id = id;
        this.type = type;
        this.assetPair = assetPair;
        this.amount = amount;
        this.price = price;
        this.timestamp = timestamp;
        this.status = status;
        this.filled = filled;
    }

    public boolean isActive() {
        return status.isActive();
    }
}
