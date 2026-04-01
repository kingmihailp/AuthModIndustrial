package com.authmodindustrial;

import com.authmodindustrial.auth.AuthManager;
import com.authmodindustrial.commands.LoginCommand;
import com.authmodindustrial.commands.RegisterCommand;
import com.authmodindustrial.config.AuthConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AuthModIndustrial.MOD_ID)
public class AuthModIndustrial {

    public static final String MOD_ID = "authmodindustrial";
    private static final Logger LOGGER = LogManager.getLogger();

    public AuthModIndustrial() {
        // Register server-side config (generated as config/authmodindustrial-server.toml)
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.SERVER, AuthConfig.SPEC, "authmodindustrial-server.toml");

        // Register our Forge event listeners (PlayerEventHandler auto-registers via @EventBusSubscriber)
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[AuthMod] AuthMod Industrial loaded.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        AuthManager.getInstance().init(FMLPaths.CONFIGDIR.get());
        LOGGER.info("[AuthMod] Authentication manager initialized.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RegisterCommand.register(event.getDispatcher());
        LoginCommand.register(event.getDispatcher());
        LOGGER.info("[AuthMod] Commands /register and /login registered.");
    }
}
