package com.gamaxa.storage;

import com.gamaxa.GAXBukkit;
import com.gamaxa.buycraft.BuycraftGiftcard;
import com.gamaxa.data.Transaction;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class FlatStorage extends Storage {

    private final File folder;
    private final File transFolder;
    private final File cardFolder;
    private final File paymentFolder;

    public FlatStorage(GAXBukkit plugin) {
        super(plugin);

        this.folder = createFolder("data");
        this.transFolder = createFolder("transactions");
        this.cardFolder = createFolder("cards");
        this.paymentFolder = createFolder("payments");
    }

    private File createFolder(String name) {
        File folder = new File(this.plugin.getDataFolder(), name);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalArgumentException("Failed to create folder " + folder);
        }
        return folder;
    }

    private File getFolder(UUID uuid) {
        File userFolder = new File(this.folder, uuid.toString());
        if (!userFolder.exists() && !userFolder.mkdirs()) {
            throw new IllegalArgumentException("Failed to create folder " + userFolder);
        }
        return userFolder;
    }

    @Override
    public void getWavesAddress(UUID uuid, BiConsumer<String, Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> getWavesAddress(uuid, (addr, e) -> {
                Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(addr, e));
            }));
            return;
        }
        try {
            File waves = new File(getFolder(uuid), "waves");
            if (waves.exists()) {
                consumer.accept(Files.readFirstLine(waves, StandardCharsets.UTF_8), null);
            } else {
                consumer.accept(null, null);
            }
        } catch (Throwable e) {
            consumer.accept(null, e);
        }
    }

    @Override
    public void setWavesAddress(UUID uuid, String address, Consumer<Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> setWavesAddress(uuid, address, (e) -> {
                Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(e));
            }));
            return;
        }
        try {
            File waves = new File(getFolder(uuid), "waves");
            FileOutputStream outputStream = new FileOutputStream(waves);
            outputStream.write(address.getBytes(StandardCharsets.UTF_8));
            outputStream.close();
            consumer.accept(null);
        } catch (Throwable e) {
            consumer.accept(e);
        }
    }

    @Override
    public void getPendingItems(UUID uuid, BiConsumer<List<ItemStack>, Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> getPendingItems(uuid, (list, e) -> {
                Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(list, e));
            }));
            return;
        }
        try {
            File itemsFile = new File(getFolder(uuid), "items");
            if (!itemsFile.exists()) {
                consumer.accept(ImmutableList.of(), null);
                return;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
            List<?> items1 = config.getList("items");
            consumer.accept((List<ItemStack>) items1, null);
        } catch (Throwable e) {
            consumer.accept(null, e);
        }
    }

    @Override
    public void addPendingItem(UUID uuid, ItemStack itemStack, Consumer<Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> addPendingItem(uuid, itemStack, e -> {
                Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(e));
            }));
            return;
        }
        try {
            File itemsFile = new File(getFolder(uuid), "items");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);

            List<ItemStack> items = (List<ItemStack>) config.getList("items", new ArrayList<>());
            items.add(itemStack);
            config.set("items", items);
            config.save(itemsFile);

            consumer.accept(null);
        } catch (Throwable e) {
            consumer.accept(e);
        }
    }

    @Override
    public void clearPendingItems(UUID uuid) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> clearPendingItems(uuid));
            return;
        }
        File itemsFile = new File(getFolder(uuid), "items");
        if (!itemsFile.delete() && !itemsFile.delete() && !itemsFile.delete()) {
            // ruh roh
        }
    }

    private String generateHash(Transaction<?> transaction) throws NoSuchAlgorithmException {
        byte[] data = new byte[4 * 8]; // 4 longs, 8 bytes each
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putLong(transaction.getSeller().getLeastSignificantBits());
        buffer.putLong(transaction.getSeller().getMostSignificantBits());
        buffer.putLong(transaction.getBuyer().getLeastSignificantBits());
        buffer.putLong(transaction.getBuyer().getMostSignificantBits());

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return new BigInteger(1, md.digest(data)).toString(16);
    }

    @Override
    public void saveTransaction(Transaction<?> transaction, Consumer<Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> saveTransaction(transaction, e -> {
                Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(e));
            }));
            return;
        }
        File transFile;
        try {
            transFile = new File(this.transFolder, this.generateHash(transaction));
        } catch (NoSuchAlgorithmException e) {
            consumer.accept(e);
            return;
        }
        try (BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(new FileOutputStream(transFile))) {
            outputStream.writeObject(transaction.getId());
            // seller
            outputStream.writeLong(transaction.getSeller().getLeastSignificantBits());
            outputStream.writeLong(transaction.getSeller().getMostSignificantBits());
            // buyer
            outputStream.writeLong(transaction.getBuyer().getLeastSignificantBits());
            outputStream.writeLong(transaction.getBuyer().getMostSignificantBits());
            // itemstack
            outputStream.writeObject(transaction.getItem());
            // amount
            outputStream.writeLong(transaction.getAmount().longValue());
            outputStream.writeInt(transaction.getAmount().scale());
            // address
            outputStream.writeObject(transaction.getSellerAddress());
            // timestamp
            outputStream.writeLong(transaction.getTimestamp());
            // confirmed
            outputStream.writeBoolean(transaction.isConfirmed());

            consumer.accept(null);
        } catch (Throwable e) {
            consumer.accept(e);
        }
    }

    @Override
    public void removeTransaction(Transaction<?> transaction, Consumer<Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> removeTransaction(transaction, e -> {
                Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(e));
            }));
            return;
        }
        try {
            File transFile = new File(this.transFolder, this.generateHash(transaction));
            transFile.delete();
            consumer.accept(null);
        } catch (Throwable e) {
            consumer.accept(e);
        }
    }

    @Override
    public void setGiftCard(UUID uuid, BuycraftGiftcard card, Consumer<Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> setGiftCard(uuid, card, consumer));
            return;
        }
        File cardFile = new File(this.cardFolder, uuid.toString());
        try (FileOutputStream outputStream = new FileOutputStream(cardFile)) {
            outputStream.write((card.id + ":" + card.code).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(e));
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(null));
    }

    @Override
    public void getGiftCard(UUID uuid, BiConsumer<BuycraftGiftcard, Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> getGiftCard(uuid, consumer));
            return;
        }
        File cardFile = new File(this.cardFolder, uuid.toString());
        String[] card = null;
        if (cardFile.exists()) {
            try {
                card = Files.toString(cardFile, StandardCharsets.UTF_8).split(":");
            } catch (IOException e) {
                Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(null, e));
                return;
            }
        }
        BuycraftGiftcard finalCard = card == null ? null : new BuycraftGiftcard(Integer.parseInt(card[0]), card[1]);
        Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(finalCard, null));
    }

    @Override
    public void addProcessedPayment(String paymentId, Consumer<Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> addProcessedPayment(paymentId, consumer));
            return;
        }
        File cardFile = new File(this.paymentFolder, paymentId);
        try {
            if (!cardFile.exists()) {
                cardFile.createNewFile();
            }
        } catch (IOException e) {
            Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(e));
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(null));
    }

    @Override
    public void isPaymentProcessed(String paymentId, BiConsumer<Boolean, Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> isPaymentProcessed(paymentId, consumer));
            return;
        }
        boolean exists = this.isPaymentProcessed(paymentId);
        Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(exists, null));
    }

    @Override
    public boolean isPaymentProcessed(String paymentId) {
        return new File(this.paymentFolder, paymentId).exists();
    }

    @Override
    public List<Transaction<?>> getAndClearTransactions() {
        List<Transaction<?>> transactions = new ArrayList<>();
        for (File file : this.transFolder.listFiles()) {
            try {
                if (file.isDirectory()) {
                    this.plugin.getLogger().log(Level.WARNING, "Found folder " + file + " that was unexpected.");
                    continue;
                }
                BukkitObjectInputStream inputStream = new BukkitObjectInputStream(new FileInputStream(file));
                String id = (String) inputStream.readObject();
                long sellerLeast = inputStream.readLong();
                long sellerMost = inputStream.readLong();
                long buyerLeast = inputStream.readLong();
                long buyerMost = inputStream.readLong();
                Object itemStack = inputStream.readObject();
                long amount = inputStream.readLong();
                int scale = inputStream.readInt();
                String address = (String) inputStream.readObject();
                long timestamp = inputStream.readLong();
                boolean confirmed = inputStream.readBoolean();
                transactions.add(new Transaction<>(id, new UUID(sellerMost, sellerLeast), new UUID(buyerMost, buyerLeast), itemStack, BigDecimal.valueOf(amount, scale), address, timestamp, confirmed));
//                file.delete();
            } catch (Throwable e) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to load transaction!", e);
            }
        }
        return transactions;
    }
}
