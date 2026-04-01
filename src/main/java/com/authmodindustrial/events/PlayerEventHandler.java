package com.authmodindustrial.events;

import com.authmodindustrial.AuthModIndustrial;
import com.authmodindustrial.auth.AuthManager;
import com.authmodindustrial.config.AuthConfig;
import com.authmodindustrial.util.ColorUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = AuthModIndustrial.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerEventHandler {

    private PlayerEventHandler() {}

    // -------------------------------------------------------------------------
    // Join / Leave
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        boolean sessionRestored = AuthManager.getInstance()
                .onPlayerJoin(uuid, player.getX(), player.getY(), player.getZ());

        if (sessionRestored) {
            player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_SESSION_RESTORED.get()));
        } else if (AuthManager.getInstance().isRegistered(uuid)) {
            player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_PLEASE_LOGIN.get()));
        } else {
            player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_PLEASE_REGISTER.get()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AuthManager.getInstance().onPlayerLeave(player.getUUID());
    }

    // -------------------------------------------------------------------------
    // Chat (blocked until authenticated)
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChat(ServerChatEvent event) {
        UUID uuid = event.getPlayer().getUUID();
        if (!AuthManager.getInstance().isAuthenticated(uuid)) {
            event.setCanceled(true);
            if (AuthManager.getInstance().tryShowBlockedMessage(uuid)) {
                event.getPlayer().sendSystemMessage(
                        ColorUtils.parse(AuthConfig.MSG_ACTION_BLOCKED.get()));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Commands (only /register and /login are allowed before auth)
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommand(CommandEvent event) {
        var source = event.getParseResults().getContext().getSource();
        try {
            ServerPlayer player = source.getPlayerOrException();
            if (AuthManager.getInstance().isAuthenticated(player.getUUID())) return;

            // Extract the root command name from the raw input
            String input = event.getParseResults().getReader().getString().trim();
            String cmdName = input.split("\\s+")[0].toLowerCase();
            if (cmdName.equals("register") || cmdName.equals("login")) return;

            event.setCanceled(true);
            if (AuthManager.getInstance().tryShowBlockedMessage(player.getUUID())) {
                player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_ACTION_BLOCKED.get()));
            }
        } catch (Exception ignored) {
            // Not a player source — allow
        }
    }

    // -------------------------------------------------------------------------
    // Block & item interactions
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        blockIfUnauthenticated(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        blockIfUnauthenticated(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        blockIfUnauthenticated(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        blockIfUnauthenticated(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        blockIfUnauthenticated(event);
    }

    private static void blockIfUnauthenticated(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!AuthManager.getInstance().isAuthenticated(uuid)) {
            event.setCanceled(true);
            if (AuthManager.getInstance().tryShowBlockedMessage(uuid)) {
                player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_ACTION_BLOCKED.get()));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Block breaking
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!AuthManager.getInstance().isAuthenticated(uuid)) {
            event.setCanceled(true);
            if (AuthManager.getInstance().tryShowBlockedMessage(uuid)) {
                player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_ACTION_BLOCKED.get()));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Attacking
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AuthManager.getInstance().isAuthenticated(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Protect the unauthenticated player from damage
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AuthManager.getInstance().isAuthenticated(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Item pickup & drop
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!AuthManager.getInstance().isAuthenticated(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (!AuthManager.getInstance().isAuthenticated(event.getPlayer().getUUID())) {
            event.setCanceled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Server tick: freeze unauthenticated players & send periodic reminders
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        int tick = server.getTickCount();
        int remindTicks = AuthConfig.REMIND_INTERVAL_SECONDS.get() * 20;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            AuthManager auth = AuthManager.getInstance();

            if (auth.isAuthenticated(uuid)) continue;

            // --- Freeze position ---
            // Every 5 ticks (0.25 s) snap back to spawn position
            if (tick % 5 == 0) {
                double[] pos = auth.getFrozenPosition(uuid);
                if (pos != null) {
                    double dx = player.getX() - pos[0];
                    double dy = player.getY() - pos[1];
                    double dz = player.getZ() - pos[2];
                    if (dx * dx + dy * dy + dz * dz > 0.25) {
                        player.teleportTo(pos[0], pos[1], pos[2]);
                    }
                    player.setDeltaMovement(0, 0, 0);
                }
            }

            // --- Periodic reminder ---
            if (tick % remindTicks == 0) {
                if (auth.isRegistered(uuid)) {
                    player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_REMIND_LOGIN.get()));
                } else {
                    player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_REMIND_REGISTER.get()));
                }
            }
        }
    }
}
