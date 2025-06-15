package top.seraphjack.rmc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.seraphjack.restic.entity.Snapshot;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public final class RMCCommand {

    public static final DynamicCommandExceptionType ERROR_INVALID_INTERVAL =
            new DynamicCommandExceptionType(msg -> new LiteralMessage("Invalid ISO8601 duration: " + msg));
    private static final Logger log = LoggerFactory.getLogger(RMCCommand.class);

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                literal("rmc")
                        .then(literal("schedule")
                                .requires(p -> p.hasPermission(3))
                                .executes(RMCCommand::scheduleBackup))
                        .then(literal("setInterval")
                                .requires(p -> p.hasPermission(3))
                                .then(argument("interval", StringArgumentType.string())
                                        .executes(RMCCommand::setInterval)))
                        .then(literal("snapshots")
                                .requires(p -> p.hasPermission(3))
                                .executes(RMCCommand::listSnapshots))
                        .then(literal("restore")
                                .requires(p -> p.hasPermission(4))
                                .then(argument("snapshot", SnapshotArgumentType.snapshot())
                                        .executes(RMCCommand::restore)))

        );
    }

    private static int scheduleBackup(CommandContext<CommandSourceStack> context) {
        RMC.backupCore.scheduleNow();
        context.getSource().sendSuccess(() -> Component.literal("Backup scheduled"), true);
        return SINGLE_SUCCESS;
    }

    private static int setInterval(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final String intervalString = StringArgumentType.getString(context, "interval");
        final Duration interval;
        try {
            interval = Duration.parse(intervalString);
        } catch (DateTimeParseException ex) {
            throw ERROR_INVALID_INTERVAL.create(intervalString);
        }
        Config.BACKUP_INTERVAL.set(intervalString);
        Config.BACKUP_INTERVAL.save();
        RMC.backupCore.modifyBackupInterval(interval);
        return SINGLE_SUCCESS;
    }

    private static int listSnapshots(CommandContext<CommandSourceStack> context) {
        final List<Snapshot> snapshots = RMC.snapshotListCache;

        snapshots.stream().sorted(Comparator.comparing(Snapshot::getTime)).forEach(snapshot -> {
            final var time = snapshot.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            final String item = String.format("Snapshot %s at %s %.2f MiB", snapshot.getShortId(), time, snapshot.getSummary().getTotalBytesProcessed() / 1024.0 / 1024.0);
            context.getSource().sendSuccess(() -> Component.literal(item), false);
        });

        return SINGLE_SUCCESS;
    }

    private static int restore(CommandContext<CommandSourceStack> context) {
        final var snapshot = context.getArgument("snapshot", Snapshot.class);
        try {
            RMC.backupCore.restore(snapshot.getId());
        } catch (Exception e) {
            log.error("Error restoring snapshot", e);
            throw new RuntimeException(e);
        }
        return SINGLE_SUCCESS;
    }

}
