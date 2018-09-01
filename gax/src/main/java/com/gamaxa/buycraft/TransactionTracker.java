package com.gamaxa.buycraft;

import com.gamaxa.Data;
import com.gamaxa.GAXBukkit;
import com.gamaxa.data.Transaction;
import com.google.common.collect.ImmutableMap;
import com.wavesplatform.wavesj.Base58;
import com.wavesplatform.wavesj.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class TransactionTracker implements Runnable {
    private final GAXBukkit plugin;
    private final Map<String, Transaction<BuycraftGiftcard>> cards = new HashMap<>();
    private final Node node = new Node(Data.getNodeUrl(false));
    private final GiftcardIntegration giftcards;
    private final SecureRandom secureRandom = new SecureRandom();

    private String lastTransaction = "";

    public TransactionTracker(GAXBukkit plugin, List<Transaction<BuycraftGiftcard>> transactions) throws URISyntaxException {
        this.plugin = plugin;
        for (Transaction<BuycraftGiftcard> transaction : transactions) {
            this.cards.put(transaction.getId(), transaction);
        }
        this.giftcards = new GiftcardIntegration(this.plugin);
    }

    public void getGiftCard(UUID player, BiConsumer<BuycraftGiftcard, Throwable> consumer) {
        this.plugin.getStorage().getGiftCard(player, (gf, e) -> {
            if (e != null) {
                consumer.accept(null, e);
                return;
            }
            if (gf != null) {
                consumer.accept(gf, null);
                return;
            }
            this.giftcards.createGiftCard(0, (newGf, e2) -> {
                if (e2 != null) {
                    consumer.accept(null, e2);
                    return;
                }
                this.plugin.getStorage().setGiftCard(player, newGf, e3 -> consumer.accept(newGf, e3));
            });
        });
    }

    public void createTransaction(UUID player, BiConsumer<Transaction<BuycraftGiftcard>, Throwable> consumer) {
        this.getGiftCard(player, (gf, e) -> {
            if (e != null) {
                consumer.accept(null, e);
                return;
            }
            int rand = this.secureRandom.nextInt(100000);
            Transaction<BuycraftGiftcard> trans = new Transaction<>(String.format("%04d", rand), new UUID(0, 0), player, gf, BigDecimal.ZERO, this.plugin.getConfig().getString("buycraft.address"));
            this.cards.put(trans.getId(), trans);
            this.plugin.getStorage().saveTransaction(trans, ex -> consumer.accept(trans, ex));
        });
    }

    @Override
    public void run() {
        try {
            List<Map<String, Object>> transactionList = node.getTransactionList(this.plugin.getConfig().getString("buycraft.address"), 50);
            for (Map<String, Object> transaction : transactionList) {
                if (this.lastTransaction.equals(transaction.get("id"))) {
                    return;
                }

                if (!transaction.containsKey("id")) {
                    continue;
                }
                if (this.plugin.getStorage().isPaymentProcessed((String) transaction.get("id"))) {
                    break;
                }
                if (transaction.containsKey("type") &&
                        transaction.get("type").equals(4) &&
                        Data.getAssetId(false).equals(transaction.get("assetId")) &&
                        this.plugin.getConfig().getString("buycraft.address").equals(transaction.get("recipient"))) {

                    byte[] decoded;
                    try {
                        decoded = Base58.decode((String) transaction.get("attachment"));
                    } catch (Exception e) {
                        // eh
                        this.plugin.getStorage().addProcessedPayment((String) transaction.get("id"), e2 -> {
                            if (e2 != null) {
                                this.plugin.getLogger().log(Level.WARNING, "Failed to add payment!", e2);
                            }
                        });
                        continue;
                    }
                    String str = new String(decoded, StandardCharsets.UTF_8);
                    if (str.indexOf(" ") > 0) {
                        str = str.trim().split(" ")[0].trim(); // try our best in case they added an extra character or something
                    }

                    Transaction<BuycraftGiftcard> t = this.cards.get(str);
                    if (t == null) {
                        this.plugin.getStorage().addProcessedPayment((String) transaction.get("id"), e -> {
                            if (e != null) {
                                this.plugin.getLogger().log(Level.WARNING, "Failed to add payment!", e);
                            }
                        });
                        continue;
                    }
                    BigDecimal amount = BigDecimal.valueOf((long) (int) transaction.get("amount"), 8);
                    amount = amount.multiply(BigDecimal.valueOf(this.plugin.getConfig().getDouble("buycraft.ratio")));
                    this.giftcards.creditGiftCard(t.getItem().id, amount);

                    this.plugin.getStorage().addProcessedPayment((String) transaction.get("id"), e -> {
                        if (e != null) {
                            this.plugin.getLogger().log(Level.WARNING, "Failed to add payment!", e);
                        }
                    });

                    BigDecimal finalAmount = amount;
                    this.plugin.getStorage().removeTransaction(t, e -> {
                        if (e != null) {
                            this.plugin.getLogger().log(Level.WARNING, "Failed to remove transaction", e);
                        }
                        Player player = Bukkit.getPlayer(t.getBuyer());
                        if (player != null) {
                            this.plugin.getData().sendMessage(player, "lang.giftcard.received", ImmutableMap.of(
                                    "amount", finalAmount.toPlainString(),
                                    "giftcard", t.getItem().code
                            ));
                        }
                    });
                }

                this.lastTransaction = (String) transaction.get("id");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
