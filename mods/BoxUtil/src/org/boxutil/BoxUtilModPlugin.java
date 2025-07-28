package org.boxutil;

import com.fs.starfarer.Version;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.boxutil.config.BoxConfigGUI;
import org.boxutil.config.BoxConfigs;
import org.boxutil.define.BoxDatabase;
import org.boxutil.manager.CampaignRenderingManager;
import org.boxutil.manager.ModelManager;
import org.boxutil.manager.ShaderCore;

/**
 * Development environment: JDK17.0.10_zulu<p>
 * Hardware: ASRock Z490M-ITX/ac / i5-10400F (no OC) / 16GB DDR4 2666MHz * 2 (no OC) / NVIDIA RTX 3070Ti (OC to 1965MHz@0.975V)<p>
 * @since 2024.08.18
 * @author ShioZakana
 * @version 2025.06.05 - 1.3.5
 */
public final class BoxUtilModPlugin extends BaseModPlugin {
    private static final String _ADAPTATION_VERSION = "0.98";
    private static boolean isPreInitialized = false;

    /**
     * Optional.
     */
    public static void initPre() {
        if (isPreInitialized) return;
        BoxDatabase.initGLState();
        BoxConfigs.init();
        ShaderCore.initScreenSize();
//        KernelCore.init();
        ModelManager.loadModelDataCSV(BoxDatabase.BUILTIN_OBJ_CSV);
        isPreInitialized = true;
    }

    /**
     * Optional.<p>
     * Make sure OpenGL context has created.
     */
    public static void initLater() {
        BoxConfigGUI.globalInitLater();
    }

    public static boolean isPreInitialized() {
        return isPreInitialized;
    }

    public static boolean isGlobalInitialized() {
        return BoxConfigGUI.isGlobalInitialized();
    }

    public void onApplicationLoad() {
        if (!Version.versionInfoForMods.getMajor().contentEquals(_ADAPTATION_VERSION)) {
            String version = Global.getSettings().getModManager().getModSpec(BoxDatabase.MOD_ID).getVersionInfo().getString();
            throw new RuntimeException("'BoxUtil' this mod with version '" + version + "' should running at Starsector '" + _ADAPTATION_VERSION + "'.");
        }
        initPre();
    }

    public void onGameLoad(boolean newGame) {
        if (Global.getSector() != null) {
            Global.getSector().addTransientScript(new CampaignRenderingManager());
        }
    }
}