package top.seraphjack.rmc;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.seraphjack.restic.entity.ForgetGroup;
import top.seraphjack.restic.messages.BackupStatusMessage;
import top.seraphjack.restic.messages.BackupSummaryMessage;
import top.seraphjack.restic.messages.RestoreStatusMessage;
import top.seraphjack.restic.messages.RestoreSummaryMessage;

public final class MinecraftAdapter implements top.seraphjack.backupcore.MinecraftAdapter {

    private static final Logger log = LoggerFactory.getLogger(MinecraftAdapter.class);
    private final MinecraftServer server;
    private boolean anyBackupSinceNoPlayer = true;

    MinecraftAdapter(MinecraftServer server) {
        this.server = server;
    }

    private void logAndBroadcastMessage(String message, Object... args) {
        final String logMessage = String.format(message, args);
        server.getPlayerList().broadcastSystemMessage(Component.literal(logMessage), false);
        log.info(logMessage);
    }

    @Override
    public void preBackup() {
        logAndBroadcastMessage("Backup started");
        server.saveEverything(true, true, true);
        for (ServerLevel level : server.getAllLevels()) {
            level.noSave = true;
        }
    }

    @Override
    public void postBackup() {
        for (ServerLevel level : server.getAllLevels()) {
            level.noSave = false;
        }
    }

    @Override
    public void onException(Throwable throwable) {
        // TODO: styling
        server.getPlayerList().broadcastSystemMessage(Component.literal("Error caught during backup, see console logs for details."), false);
        log.error("Error caught during backup", throwable);
    }

    @Override
    public boolean shouldSkipBackup() {
        if (!Config.SKIP_WHEN_NO_PLAYERS.get()) {
            return false;
        }
        final int playerCount = server.getPlayerCount();
        if (playerCount > 0) {
            anyBackupSinceNoPlayer = false;
        }
        return playerCount == 0 && anyBackupSinceNoPlayer;
    }

    @Override
    public void onRestoreDone(RestoreSummaryMessage message) {
        // TODO: be more graceful
        log.info("Restore done, halting the server.");
        // Halt the JVM before server overwrites the restored save
        Runtime.getRuntime().halt(1);
    }

    @Override
    public void onBackupDone(BackupSummaryMessage message) {
        final String logMessage = String.format("Backup done! Took %.1f seconds, %.2f MiB added", message.getTotalDuration(), message.getDataAddedPacked() / 1024.0 / 1024.0);
        logAndBroadcastMessage(logMessage);
        RMC.updateSnapshotListCache();
    }

    @Override
    public void sendForgetGroup(List<ForgetGroup> forgetGroup) {
        if (forgetGroup.stream().anyMatch(g -> g.getRemove() != null && !g.getRemove().isEmpty())) {
            logAndBroadcastMessage("Removed %s old snapshots", forgetGroup.stream().mapToLong(g -> g.getRemove().size()).sum());
        }
        RMC.updateSnapshotListCache();
    }

    @Override
    public void sendRestoreStatus(RestoreStatusMessage message) {
        final String logMessage = String.format("Restore progress %.0f%%", message.getPercentDone() * 100);
        logAndBroadcastMessage(logMessage);
    }

    @Override
    public void sendBackupStatus(BackupStatusMessage message) {
        final String logMessage = String.format("Back up progress %.0f%%", message.getPercentDone() * 100);
        logAndBroadcastMessage(logMessage);
    }
}
