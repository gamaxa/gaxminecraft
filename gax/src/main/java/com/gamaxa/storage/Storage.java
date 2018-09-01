package com.gamaxa.storage;

import com.gamaxa.GAXBukkit;
import com.gamaxa.buycraft.BuycraftGiftcard;
import com.gamaxa.data.Transaction;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author PaulBGD
 */
public abstract class Storage {
    protected final GAXBukkit plugin;

    public Storage(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    public abstract void getWavesAddress(UUID uuid, BiConsumer<String, Throwable> consumer);

    public abstract void setWavesAddress(UUID uuid, String address, Consumer<Throwable> consumer);

    public abstract void getPendingItems(UUID uuid, BiConsumer<List<ItemStack>, Throwable> consumer);

    public abstract void addPendingItem(UUID uuid, ItemStack itemStack, Consumer<Throwable> consumer);

    public abstract void clearPendingItems(UUID uuid);

    public abstract void saveTransaction(Transaction<?> transaction, Consumer<Throwable> consumer);

    public abstract void removeTransaction(Transaction<?> transaction, Consumer<Throwable> consumer);

    public abstract void setGiftCard(UUID uuid, BuycraftGiftcard card, Consumer<Throwable> consumer);

    public abstract void addProcessedPayment(String paymentId, Consumer<Throwable> consumer);

    public abstract void isPaymentProcessed(String paymentId, BiConsumer<Boolean, Throwable> consumer);

    public abstract boolean isPaymentProcessed(String paymentId);

    public abstract void getGiftCard(UUID uuid, BiConsumer<BuycraftGiftcard, Throwable> consumer);

    /**
     * Note: runs on current thread, meant to be used on startup
     */
    public abstract List<Transaction<?>> getAndClearTransactions();
}
