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

public final class RegisterCommand {

    private RegisterCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("register")
                .then(Commands.argument("password", StringArgumentType.word())
                    .then(Commands.argument("confirmPassword", StringArgumentType.word())
                        .executes(ctx -> execute(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "password"),
                            StringArgumentType.getString(ctx, "confirmPassword")
                        ))
                    )
                )
        );
    }

    private static int execute(CommandSourceStack source, String password, String confirmPassword) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        AuthManager auth = AuthManager.getInstance();

        if (auth.isRegistered(player.getUUID())) {
            player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_ALREADY_REGISTERED.get()));
            return 0;
        }

        int minLen = AuthConfig.MIN_PASSWORD_LENGTH.get();
        if (password.length() < minLen) {
            String msg = AuthConfig.MSG_PASSWORD_TOO_SHORT.get()
                    .replace("{min}", String.valueOf(minLen));
            player.sendSystemMessage(ColorUtils.parse(msg));
            return 0;
        }

        if (!password.equals(confirmPassword)) {
            player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_PASSWORD_MISMATCH.get()));
            return 0;
        }

        auth.register(player.getUUID(), password);
        auth.markAuthenticated(player.getUUID());
        player.sendSystemMessage(ColorUtils.parse(AuthConfig.MSG_REGISTERED.get()));
        return 1;
    }
}
