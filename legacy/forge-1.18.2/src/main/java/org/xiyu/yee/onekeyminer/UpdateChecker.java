package org.xiyu.yee.onekeyminer;

import com.google.gson.*;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.xiyu.yee.onekeyminer.Onekeyminer.MODID;

@EventBusSubscriber(modid = MODID)
public class UpdateChecker {

    private static final String PROJECT_ID = "LZu4H9cp";
    private static final String API_URL = "https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version";

    private static String latestVersion = null;
    private static boolean checked = false;

    /** 是否宽松匹配大版本，例如 1.21 覆盖 1.21.5 */
    private static final boolean ALLOW_MAJOR_MATCH = false;

    private static void fetchLatestVersion() {
        if (checked) return;
        checked = true;

        CompletableFuture.runAsync(() -> {
            try {
                String mcVersion = SharedConstants.getCurrentVersion().getId(); // 例如 1.21.5
                String loader = detectLoader(); // 例如 forge

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                JsonArray root = JsonParser.parseString(res.body()).getAsJsonArray();

                String bestVersion = null;
                String publishedDate = null;

                for (JsonElement elem : root) {
                    JsonObject file = elem.getAsJsonObject();

                    // 取支持的版本和加载器
                    JsonArray gameVersions = file.getAsJsonArray("game_versions");
                    JsonArray loaders = file.getAsJsonArray("loaders");

                    boolean supportsGame = false;
                    for (JsonElement v : gameVersions) {
                        String supported = v.getAsString();
                        if (mcVersion.equals(supported)) {
                            supportsGame = true;
                            break;
                        }
                        if (ALLOW_MAJOR_MATCH && mcVersion.startsWith(supported + ".")) {
                            supportsGame = true;
                            break;
                        }
                    }

                    boolean supportsLoader = false;
                    for (JsonElement l : loaders) {
                        if (loader.equalsIgnoreCase(l.getAsString())) {
                            supportsLoader = true;
                            break;
                        }
                    }

                    if (!(supportsGame && supportsLoader)) continue;

                    String versionNumber = file.get("version_number").getAsString();
                    String date = file.get("date_published").getAsString();

                    // 选最新的
                    if (publishedDate == null || date.compareTo(publishedDate) > 0) {
                        bestVersion = versionNumber;
                        publishedDate = date;
                    }
                }

                latestVersion = bestVersion;

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static String detectLoader() {
    // 目前是 forge 固定值，如果以后支持其他加载器，可以再做判断
    return "forge";
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            fetchLatestVersion();

            if (latestVersion != null) {
                String current = ModList.get()
                        .getModContainerById(MODID)
                        .map(c -> c.getModInfo().getVersion().toString())
                        .orElse("unknown");

                if (!current.equals(latestVersion)) {
                    sp.sendMessage(new TranslatableComponent("message.onekeyminer.update_available", current, latestVersion), sp.getUUID());
                }
            }
        }
    }
}
