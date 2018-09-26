package com.gamaxa.data;

import com.gamaxa.Data;
import com.gamaxa.GAXBukkit;
import com.google.common.collect.ImmutableMap;
import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
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
public class TradeTracker implements Runnable {

    private final GAXBukkit plugin;
    private final Map<String, Transaction<ItemStack>> seller = new ConcurrentHashMap<>();
    private final Map<UUID, Transaction<ItemStack>> buyer = new ConcurrentHashMap<>();
    private final Node node = new Node(Data.getNodeUrl(false));

    public TradeTracker(GAXBukkit plugin, List<Transaction<ItemStack>> transactions) throws URISyntaxException, IOException {
        this.plugin = plugin;
        for (Transaction<ItemStack> transaction : transactions) {
            this.seller.put(transaction.getSellerAddress(), transaction);
            this.buyer.put(transaction.getBuyer(), transaction);
        }
    }

    @Override
    public void run() {
        for (Map.Entry<String, Transaction<ItemStack>> entry : seller.entrySet()) {
            try {
                List<Map<String, Object>> transactions = this.node.getTransactionList(entry.getKey(), 10);
                for (Map<String, Object> transaction : transactions) {
                    if (!transaction.containsKey("id")) {
                        continue;
                    }
                    if (transaction.containsKey("type") &&
                            transaction.get("type").equals(4) &&
                            Data.getAssetId(false).equals(transaction.get("assetId"))) {
                        byte[] decoded;
                        try {
                            decoded = Base58.decode((String) transaction.get("attachment"));
                        } catch (Exception e) {
                            // eh
                            continue;
                        }
                        String str = new String(decoded, StandardCharsets.UTF_8);
                        if (str.indexOf(" ") > 0) {
                            str = str.trim().split(" ")[0].trim(); // try our best in case they added an extra character or something
                        }
                        Transaction<ItemStack> t = entry.getValue();
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
                            this.givePlayerItem(t.getBuyer(), t.getItem());
                            this.removeTransaction(t);
                            this.plugin.getStorage().removeTransaction(t, e -> {
                                if (e != null) {
                                    this.plugin.getLogger().log(Level.WARNING, "Failed to delete transaction file", e);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to check transactions for address " + entry.getKey(), e);
            }
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

    public Collection<Transaction<ItemStack>> getTransactions() {
        return this.buyer.values();
    }

    public boolean isSelling(String seller) {
        return this.seller.containsKey(seller);
    }

    public void removeTransaction(Transaction<ItemStack> transaction) {
        this.seller.remove(transaction.getSellerAddress());
        this.buyer.remove(transaction.getBuyer());
    }

    public void addTransaction(Transaction<ItemStack> transaction) {
        if (this.isSelling(transaction.getSellerAddress()) || this.isBuying(transaction.getBuyer())) {
            throw new IllegalArgumentException("Seller already has an existing transaction.");
        }
        this.buyer.put(transaction.getBuyer(), transaction);
        this.seller.put(transaction.getSellerAddress(), transaction);
    }

    public boolean isBuying(UUID buyer) {
        return this.buyer.containsKey(buyer) && !this.buyer.get(buyer).isConfirmed();
    }

    public boolean confirm(UUID buyer, BiConsumer<Transaction<ItemStack>, Throwable> consumer) {
        Transaction<ItemStack> transaction = this.buyer.get(buyer);
        if (transaction != null) {
            transaction.setConfirmed();
            this.plugin.getStorage().saveTransaction(transaction, e -> consumer.accept(transaction, e));
            return true;
        }
        return false;
    }
}
