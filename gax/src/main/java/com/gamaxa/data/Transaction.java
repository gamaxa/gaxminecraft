package com.gamaxa.data;

import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author PaulBGD
 */
public class Transaction<I> {
    private final String id;
    private final UUID seller;
    private final UUID buyer;
    private final I itemStack;
    private final BigDecimal amount;
    private final String sellerAddress;
    private final long timestamp;
    private boolean confirmed;

    public Transaction(String id, UUID seller, UUID buyer, I itemStack, BigDecimal amount, String sellerAddress) {
        this(id, seller, buyer, itemStack, amount, sellerAddress, System.currentTimeMillis(), false);
    }

    public Transaction(String id, UUID seller, UUID buyer, I itemStack, BigDecimal amount, String sellerAddress, long timestamp, boolean confirmed) {
        this.id = id;
        this.seller = seller;
        this.buyer = buyer;
        this.itemStack = itemStack;
        this.amount = amount;
        this.sellerAddress = sellerAddress;
        this.timestamp = timestamp;
        this.confirmed = confirmed;
    }

    public String getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public UUID getBuyer() {
        return buyer;
    }

    public I getItem() {
        return itemStack;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getSellerAddress() {
        return sellerAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed() {
        this.confirmed = true;
    }
}
