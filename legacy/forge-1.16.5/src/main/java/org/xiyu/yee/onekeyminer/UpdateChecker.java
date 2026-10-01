package org.xiyu.yee.onekeyminer;

import com.google.gson.*;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
                String mcVersion = "1.16.5"; // 1.16.5固定版本
                String loader = detectLoader(); // 例如 forge

                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonArray root = new JsonParser().parse(response.toString()).getAsJsonArray();

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
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity sp = (ServerPlayerEntity) event.getEntity();
            fetchLatestVersion();

            if (latestVersion != null) {
                String current = ModList.get()
                        .getModContainerById(MODID)
                        .map(c -> c.getModInfo().getVersion().toString())
                        .orElse("unknown");

                if (!current.equals(latestVersion)) {
                    sp.sendMessage(new TranslationTextComponent("message.onekeyminer.update_available", current, latestVersion), sp.getUUID());
                }
            }
        }
    }
}
