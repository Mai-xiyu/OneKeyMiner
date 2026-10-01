package org.xiyu.yee.onekeyminer;

import com.google.gson.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

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

    private static final boolean ALLOW_MAJOR_MATCH = false;

    private static void fetchLatestVersion() {
        if (checked) return;
        checked = true;

        CompletableFuture.runAsync(() -> {
            try {
                String mcVersion = "1.12.2";
                String loader = "forge";

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

                    JsonArray gameVersions = file.getAsJsonArray("game_versions");
                    JsonArray loaders = file.getAsJsonArray("loaders");

                    boolean supportsGame = false;
                    for (JsonElement v : gameVersions) {
                        String supported = v.getAsString();
                        if (mcVersion.equals(supported)) {
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

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP sp = (EntityPlayerMP) event.player;
            fetchLatestVersion();

            if (latestVersion != null) {
                String current = Onekeyminer.VERSION;

                if (!current.equals(latestVersion)) {
                    sp.sendMessage(new TextComponentTranslation("message.onekeyminer.update_available", current, latestVersion));
                }
            }
        }
    }
}
