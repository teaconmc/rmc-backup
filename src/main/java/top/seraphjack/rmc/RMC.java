package top.seraphjack.rmc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.seraphjack.backupcore.BackupCore;
import top.seraphjack.backupcore.BackupProfile;
import top.seraphjack.restic.entity.Snapshot;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Mod(value = RMC.MOD_ID)
@EventBusSubscriber(modid = RMC.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class RMC {

    public static final String MOD_ID = "rmc";
    private static final Logger log = LoggerFactory.getLogger(RMC.class);

    static BackupCore backupCore;
    static List<Snapshot> snapshotListCache = List.of();

    static IModInfo modInfo;

    public RMC(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modInfo = modContainer.getModInfo();
    }

    @SubscribeEvent
    private static void registerCommands(RegisterCommandsEvent event) {
        RMCCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    private static void setup(ServerStartedEvent event) {
        final MinecraftServer server = event.getServer();
        final MinecraftAdapter adapter = new MinecraftAdapter(server);

        final Path worldPath = server.getWorldPath(LevelResource.ROOT).normalize().toAbsolutePath();

        final Path resticExecutable;
        if (Config.USE_BUNDLED_RESTIC.get()) {
            Path extractedOrFallback;
            try {
                extractedOrFallback = BundledResticExecutable.extract();
            } catch (Exception e) {
                log.error("Failed to extract restic executable, falling back to specified restic executable path", e);
                extractedOrFallback = Path.of(Config.RESTIC_EXECUTABLE_PATH.get());
            }
            resticExecutable = extractedOrFallback;
        } else {
            resticExecutable = Path.of(Config.RESTIC_EXECUTABLE_PATH.get());
        }

        final BackupProfile backupProfile = BackupProfile.builder()
                .resticExecutable(resticExecutable)
                .repositoryPath(Config.REPOSITORY_PATH.get())
                .repositoryPassword(Config.REPOSITORY_PASSWORD.get())
                .awsAccessKeyId(Config.AWS_ACCESS_KEY_ID.get())
                .awsSecretAccessKey(Config.AWS_SECRET_ACCESS_KEY.get())
                .pathToBackup(worldPath)
                .backupTag(Config.BACKUP_TAG.get().isEmpty() ? null : Config.BACKUP_TAG.get())
                .enableForget(Config.ENABLE_FORGET.get())
                .forgetPolicy(List.of(Config.FORGET_POLICY.get().split(" ")))
                .backupBeforeRestoring(Config.BACKUP_BEFORE_RESTORE.get())
                .backupInterval(Duration.parse(Config.BACKUP_INTERVAL.get()))
                .adapter(adapter)
                .build();

        backupCore = BackupCore.create(backupProfile);
        updateSnapshotListCache();

        log.info("RMC Backup initialized.");
    }

    @SubscribeEvent
    private static void serverStop(ServerStoppingEvent event) throws InterruptedException {
        backupCore.shutdown();
    }

    static void updateSnapshotListCache() {
        try {
            snapshotListCache = backupCore.listSnapshots();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
