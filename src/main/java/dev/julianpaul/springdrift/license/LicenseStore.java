package dev.julianpaul.springdrift.license;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class LicenseStore {

    private static final Path STORE_DIR = Path.of(System.getProperty("user.home"), ".spring-drift");
    private static final Path KEY_FILE = STORE_DIR.resolve("license.key");
    private static final Path CACHE_FILE = STORE_DIR.resolve("license.cache");
    private static final long CACHE_TTL_DAYS = 7;

    public void save(String licenseKey) throws IOException {
        Files.createDirectories(STORE_DIR);
        Files.writeString(KEY_FILE, licenseKey.strip());
    }

    public String load() throws IOException {
        if (Files.exists(KEY_FILE)) {
            return Files.readString(KEY_FILE).strip();
        }
        return null;
    }

    public boolean hasKey() {
        return Files.exists(KEY_FILE);
    }

    public void saveVerified() throws IOException {
        Files.createDirectories(STORE_DIR);
        Files.writeString(CACHE_FILE, String.valueOf(Instant.now().getEpochSecond()));
    }

    public boolean isVerifiedRecently() {
        if (!Files.exists(CACHE_FILE)) return false;
        try {
            long epoch = Long.parseLong(Files.readString(CACHE_FILE).strip());
            Instant verifiedAt = Instant.ofEpochSecond(epoch);
            return Instant.now().isBefore(verifiedAt.plus(CACHE_TTL_DAYS, ChronoUnit.DAYS));
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }

    public boolean hasCachedVerification() {
        return Files.exists(CACHE_FILE);
    }

    public void clear() throws IOException {
        Files.deleteIfExists(KEY_FILE);
        Files.deleteIfExists(CACHE_FILE);
    }
}
