package top.seraphjack.rmc;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import java.time.Duration;
import java.util.UUID;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ConfigValue<Boolean> USE_BUNDLED_RESTIC = BUILDER
            .comment("Whether to use bundled restic executable")
            .define("useBundledRestic", BundledResticExecutable::isAvailable);

    public static final ConfigValue<String> RESTIC_EXECUTABLE_PATH = BUILDER
            .comment("Path to the restic executable")
            .define("resticExecutablePath", "restic");

    public static final ConfigValue<String> REPOSITORY_PATH = BUILDER
            .comment("Path of the backup restic repository")
            .define("repositoryPath", "rmc-backup");

    public static final ConfigValue<String> REPOSITORY_PASSWORD = BUILDER
            .comment("Password of the repository")
            .define("repositoryPassword", UUID.randomUUID().toString());

    public static final ConfigValue<String> AWS_ACCESS_KEY_ID = BUILDER
            .comment("Access key id for s3 backed repository")
            .define("awsAccessKeyId", "");

    public static final ConfigValue<String> AWS_SECRET_ACCESS_KEY = BUILDER
            .comment("Secret Access key for s3 backed repository")
            .define("awsSecretAccessKey", "");

    public static final ConfigValue<String> BACKUP_TAG = BUILDER
            .comment("Backup tag")
            .define("backupTag", "");

    public static final ConfigValue<Boolean> ENABLE_FORGET = BUILDER
            .comment("Enable forget")
            .define("enableForge", true);

    public static final ConfigValue<String> FORGET_POLICY = BUILDER
            .comment("Forget policy")
            .define("forgetPolicy", "--keep-daily 7 --keep-hourly 12 --keep-last 3");

    public static final ConfigValue<Boolean> BACKUP_BEFORE_RESTORE = BUILDER
            .comment("Backup before restoring")
            .define("backupBeforeRestore", true);

    public static final ConfigValue<String> BACKUP_INTERVAL = BUILDER
            .comment("Backup interval")
            .define("backupInterval", "PT1H", s -> s instanceof String && Duration.parse((String) s).isPositive());

    public static final ConfigValue<Boolean> SKIP_WHEN_NO_PLAYERS = BUILDER
            .comment("Skip backup when no players online")
            .define("skipWhenNoPlayers", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
