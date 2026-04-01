package com.authmodindustrial.auth;

import com.authmodindustrial.config.AuthConfig;
import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for player authentication state, sessions, and persistent data.
 *
 * <p>Thread-safe for concurrent access from the server tick thread and network threads.
 */
public final class AuthManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AuthManager INSTANCE = new AuthManager();

    /** UUID string → SHA-256 hashed password */
    private final Map<String, String> registeredPlayers = new HashMap<>();

    /** Players that are authenticated in this server process lifetime */
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    /** UUID → timestamp (ms) of last successful login, persisted across restarts */
    private final Map<UUID, Long> sessionTimestamps = new ConcurrentHashMap<>();

    /** UUID → [x, y, z] spawn-freeze position for unauthenticated players */
    private final Map<UUID, double[]> frozenPositions = new ConcurrentHashMap<>();

    /** Cooldown to avoid spamming the "blocked" message; UUID → last sent time (ms) */
    private final Map<UUID, Long> blockedMsgCooldown = new ConcurrentHashMap<>();

    private Path dataFile;

    private AuthManager() {}

    public static AuthManager getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Initialization & persistence
    // -------------------------------------------------------------------------

    public void init(Path configDir) {
        Path authDir = configDir.resolve("authmodindustrial");
        try {
            Files.createDirectories(authDir);
        } catch (IOException e) {
            LOGGER.error("[AuthMod] Failed to create auth data directory: {}", e.getMessage());
        }
        dataFile = authDir.resolve("players.json");
        loadData();
        LOGGER.info("[AuthMod] Initialized. Registered players loaded: {}", registeredPlayers.size());
    }

    private void loadData() {
        if (!Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            JsonObject players = root.getAsJsonObject("players");
            if (players != null) {
                for (Map.Entry<String, JsonElement> e : players.entrySet()) {
                    registeredPlayers.put(e.getKey(), e.getValue().getAsString());
                }
            }

            JsonObject sessions = root.getAsJsonObject("sessions");
            if (sessions != null) {
                for (Map.Entry<String, JsonElement> e : sessions.entrySet()) {
                    try {
                        sessionTimestamps.put(UUID.fromString(e.getKey()), e.getValue().getAsLong());
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            LOGGER.error("[AuthMod] Failed to load player data: {}", e.getMessage());
        }
    }

    public synchronized void saveData() {
        if (dataFile == null) return;
        try {
            JsonObject root = new JsonObject();

            JsonObject players = new JsonObject();
            for (Map.Entry<String, String> e : registeredPlayers.entrySet()) {
                players.addProperty(e.getKey(), e.getValue());
            }
            root.add("players", players);

            JsonObject sessions = new JsonObject();
            for (Map.Entry<UUID, Long> e : sessionTimestamps.entrySet()) {
                sessions.addProperty(e.getKey().toString(), e.getValue());
            }
            root.add("sessions", sessions);

            try (Writer writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[AuthMod] Failed to save player data: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Registration & login
    // -------------------------------------------------------------------------

    public boolean isRegistered(UUID uuid) {
        return registeredPlayers.containsKey(uuid.toString());
    }

    /**
     * Registers a player with the given password.
     *
     * @return {@code true} if registration succeeded, {@code false} if already registered
     */
    public boolean register(UUID uuid, String password) {
        if (isRegistered(uuid)) return false;
        registeredPlayers.put(uuid.toString(), hashPassword(password));
        saveData();
        return true;
    }

    /**
     * Verifies a password and authenticates the player on success.
     *
     * @return {@code true} if login succeeded
     */
    public boolean login(UUID uuid, String password) {
        String stored = registeredPlayers.get(uuid.toString());
        if (stored == null) return false;
        if (!stored.equals(hashPassword(password))) return false;
        markAuthenticated(uuid);
        return true;
    }

    public void markAuthenticated(UUID uuid) {
        authenticatedPlayers.add(uuid);
        sessionTimestamps.put(uuid, System.currentTimeMillis());
        saveData();
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.contains(uuid);
    }

    // -------------------------------------------------------------------------
    // Session management
    // -------------------------------------------------------------------------

    /** Returns {@code true} if the player's last session is still within the configured window. */
    public boolean hasValidSession(UUID uuid) {
        Long last = sessionTimestamps.get(uuid);
        if (last == null) return false;
        long sessionMs = (long) AuthConfig.SESSION_DURATION_MINUTES.get() * 60_000L;
        return (System.currentTimeMillis() - last) < sessionMs;
    }

    // -------------------------------------------------------------------------
    // Player join / leave bookkeeping
    // -------------------------------------------------------------------------

    /**
     * Called when a player joins the server.
     * Automatically restores the session if it is still valid.
     *
     * @return {@code true} if the session was restored automatically
     */
    public boolean onPlayerJoin(UUID uuid, double x, double y, double z) {
        frozenPositions.put(uuid, new double[]{x, y, z});
        if (isRegistered(uuid) && hasValidSession(uuid)) {
            markAuthenticated(uuid);
            return true;
        }
        return false;
    }

    /** Called when a player leaves the server. */
    public void onPlayerLeave(UUID uuid) {
        authenticatedPlayers.remove(uuid);
        frozenPositions.remove(uuid);
        blockedMsgCooldown.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Freeze / movement restriction helpers
    // -------------------------------------------------------------------------

    public double[] getFrozenPosition(UUID uuid) {
        return frozenPositions.get(uuid);
    }

    public void updateFrozenPosition(UUID uuid, double x, double y, double z) {
        frozenPositions.put(uuid, new double[]{x, y, z});
    }

    // -------------------------------------------------------------------------
    // Blocked-message cooldown (avoids spam)
    // -------------------------------------------------------------------------

    /** Returns {@code true} if the "action blocked" message should be sent (3-second cooldown). */
    public boolean tryShowBlockedMessage(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = blockedMsgCooldown.get(uuid);
        if (last == null || now - last >= 3_000L) {
            blockedMsgCooldown.put(uuid, now);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Password utilities
    // -------------------------------------------------------------------------

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
