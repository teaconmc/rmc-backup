package top.seraphjack.rmc;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import top.seraphjack.restic.entity.Snapshot;

import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

public class SnapshotArgumentType implements ArgumentType<Snapshot> {

    private static final SimpleCommandExceptionType ERR_SNAPSHOT_NOT_FOUND = new SimpleCommandExceptionType(new LiteralMessage("Snapshot not found"));

    private static final SnapshotArgumentType INSTANCE = new SnapshotArgumentType();

    private SnapshotArgumentType() {
    }

    public static SnapshotArgumentType snapshot() {
        return INSTANCE;
    }

    @Override
    public Snapshot parse(StringReader reader) throws CommandSyntaxException {
        final var snapshotTimeStr = readTimeString(reader);
        var snapshot = RMC.snapshotListCache.stream().filter(s -> getSnapshotTimeStr(s).equals(snapshotTimeStr)).findFirst();
        if (snapshot.isEmpty()) {
            throw ERR_SNAPSHOT_NOT_FOUND.create();
        }
        return snapshot.get();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(RMC.snapshotListCache.stream()
                .map(SnapshotArgumentType::getSnapshotTimeStr)
                .toList(), builder);
    }

    private static String getSnapshotTimeStr(Snapshot snapshot) {
        var localDateTime = snapshot.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return localDateTime.toString();
    }

    private static String readTimeString(StringReader reader) {
        final StringBuilder result = new StringBuilder();
        while (reader.canRead() && reader.peek() != ' ') {
            result.append(reader.read());
        }
        return result.toString();
    }

}
