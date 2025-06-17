package top.seraphjack.rmc;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

public final class BundledResticExecutable {

    public static boolean isAvailable() {
        final var resource = BundledResticExecutable.class.getClassLoader().getResource(getBinaryResourcePath());
        return resource != null;
    }

    public static Path extract() throws IOException {
        final var extractPath = getExtractPath();
        final InputStream stream = BundledResticExecutable.class.getClassLoader()
                .getResourceAsStream(getBinaryResourcePath());
        if (stream == null) {
            throw new IllegalStateException("Cannot find resource " + getBinaryResourcePath());
        }
        Files.copy(stream, extractPath, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.setPosixFilePermissions(extractPath, Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));
        } catch (UnsupportedOperationException ignore) {
        }
        return extractPath;
    }

    private static Path getExtractPath() {
        return Path.of("rmc-bundled-restic" + (getOs().equals("windows") ? ".exe" : "")).toAbsolutePath();
    }

    private static String getBinaryResourcePath() {
        final String os = getOs();
        final String prefix = "restic/binaries/restic_0.18.0_" + os + "_" + getArch();
        if (os.equals("windows")) {
            return prefix + ".exe";
        } else {
            return prefix;
        }
    }

    private static String getOs() {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            return "darwin";
        } else if (os.contains("nux")) {
            return "linux";
        } else {
            return "unknown";
        }
    }

    private static String getArch() {
        final String arch = System.getProperty("os.arch");
        return switch (arch) {
            case "i686", "amd64" -> "amd64";
            case "nacl" -> "arm";
            case "aarch64" -> "arm64";
            default -> "unknown";
        };
    }

}
