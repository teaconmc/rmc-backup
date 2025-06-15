package top.seraphjack.rmc;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
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

    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARG_TYPES = DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, RMC.MOD_ID);
    private static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<SnapshotArgumentType>> ARG_TYPE_SNAPSHOT = ARG_TYPES.register("snapshot", () -> SingletonArgumentInfo.contextFree(SnapshotArgumentType::snapshot));

    static BackupCore backupCore;
    static List<Snapshot> snapshotListCache = List.of();

    public RMC(IEventBus eventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ARG_TYPES.register(eventBus);
        eventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        ArgumentTypeInfos.registerByClass(SnapshotArgumentType.class, ARG_TYPE_SNAPSHOT.get());
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

        final BackupProfile backupProfile = BackupProfile.builder()
                .resticExecutable(Path.of(Config.RESTIC_EXECUTABLE_PATH.get()))
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
