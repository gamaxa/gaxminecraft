package com.gamaxa.buycraft;

import com.gamaxa.GAXBukkit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author PaulBGD
 */
public class GiftcardIntegration {
    private final Gson gson = new Gson();
    private final GAXBukkit plugin;

    public GiftcardIntegration(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    public Map<Double, Double> getGiftCards() {
        List<Map<?, ?>> cards = this.plugin.getConfig().getMapList("buycraft.giftcards");
        Map<Double, Double> map = new HashMap<>(cards.size());
        for (Map<?, ?> card : cards) {
            map.put((Double) card.get("amount"), (Double) card.get("gaxamount"));
        }
        return map;
    }

    public void createGiftCard(double amount, BiConsumer<BuycraftGiftcard, Throwable> consumer) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> createGiftCard(amount, consumer));
            return;
        }
        JsonObject main = new JsonObject();
        main.addProperty("amount", amount);

        CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build()).build();
        HttpPost post = new HttpPost("https://plugin.buycraft.net/gift-cards");
        post.addHeader("X-Buycraft-Secret", this.plugin.getConfig().getString("buycraft.key"));
        StringEntity entity = new StringEntity(this.gson.toJson(main), ContentType.APPLICATION_JSON);
        post.setEntity(entity);

        try (CloseableHttpResponse res = client.execute(post)) {
            HttpEntity resEntity = res.getEntity();
            BuycraftResponse br = this.gson.fromJson(new InputStreamReader(resEntity.getContent()), BuycraftResponse.class);
            Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(new BuycraftGiftcard(br.data.id, br.data.code), null));
        } catch (IOException e) {
            Bukkit.getScheduler().runTask(this.plugin, () -> consumer.accept(null, e));
        }
    }

    public void creditGiftCard(int card, BigDecimal amount) throws IOException {
        JsonObject main = new JsonObject();
        main.addProperty("amount", amount.toPlainString());

        CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build()).build();
        HttpPut put = new HttpPut("https://plugin.buycraft.net/gift-cards/" + card);

        put.addHeader("X-Buycraft-Secret", this.plugin.getConfig().getString("buycraft.key"));
        StringEntity entity = new StringEntity(this.gson.toJson(main), ContentType.APPLICATION_JSON);
        put.setEntity(entity);

        try (CloseableHttpResponse res = client.execute(put)) {
            HttpEntity resEntity = res.getEntity();
            BuycraftErrorResponse br = this.gson.fromJson(new InputStreamReader(resEntity.getContent()), BuycraftErrorResponse.class);
            if (br.error_message != null) {
                throw new IOException(br.error_message + " Code " + br.error_code);
            }
        }
    }
}
