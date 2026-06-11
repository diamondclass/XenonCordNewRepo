package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlacklistManager {
    private static final File BLACKLIST_FILE = new File("XenonCord/blacklist.csv");
    private static final File OLD_BLACKLIST_FILE = new File("blacklist.txt");

    private final ConcurrentHashMap<String, Long> blacklistedIps = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public BlacklistManager() {
        loadBlacklist();
        XenonCore.instance.getTaskManager().repeatingTask(this::flushIfDirty, 5, 5, TimeUnit.MINUTES);
    }

    public void add(String ip, int durationHours) {
        blacklistedIps.put(ip, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(durationHours));
        dirty.set(true);
    }

    public void remove(String ip) {
        if (blacklistedIps.remove(ip) != null) {
            dirty.set(true);
        }
    }

    public boolean isBlacklisted(String ip) {
        Long expiry = blacklistedIps.get(ip);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            blacklistedIps.remove(ip);
            dirty.set(true);
            return false;
        }
        return true;
    }

    public void flushIfDirty() {
        if (dirty.compareAndSet(true, false)) {
            saveBlacklist();
        }
    }

    private void loadBlacklist() {
        if (OLD_BLACKLIST_FILE.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(OLD_BLACKLIST_FILE.toPath())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        blacklistedIps.put(trimmed, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(72));
                    }
                }
                OLD_BLACKLIST_FILE.delete();
                dirty.set(true);
                saveBlacklist();
            } catch (IOException e) {
                XenonCore.instance.getLogger().error("Failed to migrate old blacklist: " + e.getMessage());
            }
        }

        if (!BLACKLIST_FILE.exists()) {
            try {
                if (BLACKLIST_FILE.getParentFile() != null && !BLACKLIST_FILE.getParentFile().exists()) {
                    BLACKLIST_FILE.getParentFile().mkdirs();
                }
                BLACKLIST_FILE.createNewFile();
            } catch (IOException e) {
                XenonCore.instance.getLogger().error("Failed to create blacklist file: " + e.getMessage());
            }
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(BLACKLIST_FILE.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    String[] parts = trimmed.split(",");
                    if (parts.length == 2) {
                        try {
                            blacklistedIps.put(parts[0], Long.parseLong(parts[1]));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            XenonCore.instance.getLogger().error("Failed to load blacklist: " + e.getMessage());
        }
    }

    private void saveBlacklist() {
        try {
            if (BLACKLIST_FILE.getParentFile() != null && !BLACKLIST_FILE.getParentFile().exists()) {
                BLACKLIST_FILE.getParentFile().mkdirs();
            }
            try (BufferedWriter writer = Files.newBufferedWriter(BLACKLIST_FILE.toPath())) {
                for (Map.Entry<String, Long> entry : blacklistedIps.entrySet()) {
                    writer.write(entry.getKey() + "," + entry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            XenonCore.instance.getLogger().error("Failed to save blacklist: " + e.getMessage());
        }
    }
}