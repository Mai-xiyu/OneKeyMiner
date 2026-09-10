package org.xiyu.onekeyminer.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration for Fabric.
 *
 * <p>Provides access to OneKeyMiner's native config screen directly
 * from the Mod Menu mod list.</p>
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FabricConfigScreen::new;
    }
}