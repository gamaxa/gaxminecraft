package com.gamaxa.data;

import com.gamaxa.Data;
import com.gamaxa.GAXBukkit;
import com.google.common.collect.ImmutableMap;
import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.Block;
import com.wavesplatform.wavesj.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class Tracker implements Runnable {

    private final GAXBukkit plugin;
    private final Map<String, Transaction> seller = new ConcurrentHashMap<>();
    private final Map<UUID, Transaction> buyer = new ConcurrentHashMap<>();
    private final Node node = new Node(Data.getNodeUrl(false));
    private int height = this.node.getHeight();

    public Tracker(GAXBukkit plugin, List<Transaction> transactions) throws URISyntaxException, IOException {
        this.plugin = plugin;
        for (Transaction transaction : transactions) {
            this.seller.put(transaction.getSellerAddress(), transaction);
            this.buyer.put(transaction.getBuyer(), transaction);
        }
    }

    @Override
    public void run() {
        try {
            int actualHeight = this.node.getHeight();
            if (this.height >= actualHeight) {
                this.height = actualHeight;
                return;
            }
            do {
                Block block;
                try {
                    block = this.node.getBlock(this.height);
                } catch (Exception e) {
                    return; // for some reason the server returns the new height before the block is ready
                }
                for (Map<String, Object> transaction : block.transactions) {
                    if (transaction.containsKey("type") &&
                            transaction.get("type").equals(4) &&
                            Data.getAssetId(false).equals(transaction.get("assetId")) &&
                            this.seller.containsKey(transaction.get("recipient"))) {
                        Transaction t = this.seller.get(transaction.get("recipient"));
                        byte[] decoded;
                        try {
                            decoded = Base58.decode((String) transaction.get("attachment"));
                        } catch (Exception e) {
                            // eh
                            continue;
                        }
                        BigDecimal amount = BigDecimal.valueOf((long) (int) transaction.get("amount"), 8);
                        if (t.getAmount().subtract(amount).doubleValue() <= 0.00000001) {
                            String str = new String(decoded, StandardCharsets.UTF_8);
                            if (str.startsWith(t.getId())) {
                                // we're good!
                                Player buyer = Bukkit.getPlayer(t.getBuyer());
                                Player seller = Bukkit.getPlayer(t.getSeller());
                                if (buyer != null) {
                                    this.plugin.getData().sendMessage(buyer, "lang.finished.buyer", ImmutableMap.of(
                                            "amount", t.getAmount() + ""
                                    ));
                                }
                                if (seller != null) {
                                    this.plugin.getData().sendMessage(seller, "lang.finished.seller", ImmutableMap.of(
                                            "amount", t.getAmount() + ""
                                    ));
                                }
                                this.givePlayerItem(t.getBuyer(), t.getItemStack());
                                this.removeTransaction(t);
                                this.plugin.getStorage().removeTransaction(t, e -> {
                                    if (e != null) {
                                        this.plugin.getLogger().log(Level.WARNING, "Failed to delete transaction file", e);
                                    }
                                });
                            }
                        }
                    }
                }
            } while (++this.height <= actualHeight);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void givePlayerItem(UUID uuid, ItemStack itemStack) {
        this.plugin.getStorage().addPendingItem(uuid, itemStack, e -> {
            if (e != null) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to give player item", e);
                return;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                this.plugin.getData().sendMessage(player, "lang.claim.available");
            }
        });
    }

    public Collection<Transaction> getTransactions() {
        return this.buyer.values();
    }

    public boolean isSelling(String seller) {
        return this.seller.containsKey(seller);
    }

    public void removeTransaction(Transaction transaction) {
        this.seller.remove(transaction.getSellerAddress());
        this.buyer.remove(transaction.getBuyer());
    }

    public void addTransaction(Transaction transaction) {
        if (this.isSelling(transaction.getSellerAddress()) || this.isBuying(transaction.getBuyer())) {
            throw new IllegalArgumentException("Seller already has an existing transaction.");
        }
        this.buyer.put(transaction.getBuyer(), transaction);
        this.seller.put(transaction.getSellerAddress(), transaction);
    }

    public boolean isBuying(UUID buyer) {
        return this.buyer.containsKey(buyer);
    }

    public boolean confirm(UUID buyer, BiConsumer<Transaction, Throwable> consumer) {
        Transaction transaction = this.buyer.get(buyer);
        if (transaction != null) {
            transaction.setConfirmed();
            this.plugin.getStorage().saveTransaction(transaction, e -> consumer.accept(transaction, e));
            return true;
        }
        return false;
    }
}
