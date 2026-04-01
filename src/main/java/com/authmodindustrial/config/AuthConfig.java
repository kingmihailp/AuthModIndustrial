package com.authmodindustrial.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class AuthConfig {

    public static final ForgeConfigSpec SPEC;

    // --- Messages ---
    public static final ForgeConfigSpec.ConfigValue<String> MSG_PLEASE_REGISTER;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_PLEASE_LOGIN;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_REGISTERED;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_LOGGED_IN;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_WRONG_PASSWORD;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_ALREADY_REGISTERED;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_NOT_REGISTERED;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_PASSWORD_MISMATCH;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_ACTION_BLOCKED;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_SESSION_RESTORED;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_REMIND_REGISTER;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_REMIND_LOGIN;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_ALREADY_LOGGED_IN;
    public static final ForgeConfigSpec.ConfigValue<String> MSG_PASSWORD_TOO_SHORT;

    // --- Settings ---
    public static final ForgeConfigSpec.IntValue REMIND_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue MIN_PASSWORD_LENGTH;
    public static final ForgeConfigSpec.IntValue SESSION_DURATION_MINUTES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "=================================================",
                "  AuthMod Industrial - Server Configuration",
                "=================================================",
                "  Color codes: use &#RRGGBB for hex colors",
                "               use &a, &b, &c ... for legacy codes",
                "               use &l for bold, &o italic, &r reset",
                "================================================="
        ).push("messages");

        MSG_PLEASE_REGISTER = builder
                .comment("Shown to new players who need to register")
                .define("please_register",
                        "&#FFD700Welcome to the server! You must register to play.\n&#FFFFFF Use: &#00BFFF/register <password> <confirm_password>");

        MSG_PLEASE_LOGIN = builder
                .comment("Shown to returning players who need to log in")
                .define("please_login",
                        "&#FFD700Welcome back! Please log in to continue.\n&#FFFFFF Use: &#00BFFF/login <password>");

        MSG_REGISTERED = builder
                .comment("Shown on successful registration")
                .define("registered",
                        "&#00FF7F&lRegistration successful! &#FFFFFFYou can now play. Have fun!");

        MSG_LOGGED_IN = builder
                .comment("Shown on successful login")
                .define("logged_in",
                        "&#00FF7F&lLogin successful! &#FFFFFFWelcome back, enjoy your game!");

        MSG_SESSION_RESTORED = builder
                .comment("Shown when a session is automatically restored (within 20 min)")
                .define("session_restored",
                        "&#00FF7F&lSession restored! &#FFFFFFNo need to log in again. Welcome back!");

        MSG_WRONG_PASSWORD = builder
                .comment("Shown when an incorrect password is entered")
                .define("wrong_password",
                        "&#FF4444&lIncorrect password! &#FFFFFFPlease try again.");

        MSG_ALREADY_REGISTERED = builder
                .comment("Shown when trying to /register again")
                .define("already_registered",
                        "&#FF6600You are already registered! &#FFFFFFUse &#00BFFF/login <password> &#FFFFFFto log in.");

        MSG_NOT_REGISTERED = builder
                .comment("Shown when trying to /login without registering first")
                .define("not_registered",
                        "&#FF4444You are not registered! &#FFFFFFUse &#00BFFF/register <password> <confirm_password>");

        MSG_PASSWORD_MISMATCH = builder
                .comment("Shown when passwords do not match during registration")
                .define("password_mismatch",
                        "&#FF4444Passwords do not match! &#FFFFFFPlease try again.");

        MSG_PASSWORD_TOO_SHORT = builder
                .comment("Shown when the password is too short. Use {min} as placeholder for minimum length.")
                .define("password_too_short",
                        "&#FF4444Password is too short! &#FFFFFFMinimum length: &#FFD700{min} &#FFFFFFcharacters.");

        MSG_ACTION_BLOCKED = builder
                .comment("Shown when an unauthenticated player tries to do something")
                .define("action_blocked",
                        "&#FF6600&lAuthentication required! &#FFFFFFCheck your chat and log in first.");

        MSG_ALREADY_LOGGED_IN = builder
                .comment("Shown when a player who is already logged in uses /login")
                .define("already_logged_in",
                        "&#00FF7FYou are already logged in!");

        MSG_REMIND_REGISTER = builder
                .comment("Periodic reminder sent to players who haven't registered yet")
                .define("remind_register",
                        "&#FFD700&#lReminder: &#FFFFFFPlease register with &#00BFFF/register <password> <confirm_password>");

        MSG_REMIND_LOGIN = builder
                .comment("Periodic reminder sent to players who haven't logged in yet")
                .define("remind_login",
                        "&#FFD700&lReminder: &#FFFFFFPlease log in with &#00BFFF/login <password>");

        builder.pop().push("settings");

        REMIND_INTERVAL_SECONDS = builder
                .comment("How often (in seconds) to send authentication reminders to unauthenticated players")
                .defineInRange("remind_interval_seconds", 10, 1, 300);

        MIN_PASSWORD_LENGTH = builder
                .comment("Minimum password length required for registration")
                .defineInRange("min_password_length", 6, 1, 64);

        SESSION_DURATION_MINUTES = builder
                .comment("How many minutes a session remains valid after login.",
                         "If a player rejoins within this time, they won't need to log in again.")
                .defineInRange("session_duration_minutes", 20, 1, 1440);

        builder.pop();

        SPEC = builder.build();
    }
}
