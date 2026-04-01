package com.authmodindustrial.commands;

import com.authmodindustrial.auth.AuthManager;
import com.authmodindustrial.config.AuthConfig;
import com.authmodindustrial.util.ColorUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class LoginCommand {

    private LoginCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("login")
                .then(Commands.argument("password", StringArgumentType.word())
                    .executes(ctx -> execute(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "password")
                    ))
                )
        );
    }

    private static int execute(CommandSourceStack source, String password) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        AuthManager auth = AuthManager.getInstance();

        if (auth.isAuthenticated(player.getUUID())) {
            player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_ALREADY_LOGGED_IN.get()));
            return 1;
        }

        if (!auth.isRegistered(player.getUUID())) {
            player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_NOT_REGISTERED.get()));
            return 0;
        }

        if (auth.login(player.getUUID(), password)) {
            player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_LOGGED_IN.get()));
            return 1;
        }

        player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_WRONG_PASSWORD.get()));
        return 0;
    }
}
