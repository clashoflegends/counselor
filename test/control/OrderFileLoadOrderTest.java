package control;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderFileLoadOrderTest {
    @TempDir Path folder;

    @Test void newestCopyLoadsLastRegardlessOfFilenameOrCreationOrder() throws IOException {
        File newest = file("orders_913_8.player-new.rc.egf", 3000);
        File oldest = file("orders_913_8.player-old.rc.egf", 1000);
        File middle = file("orders_913_8.player-middle.rc.egf", 2000);
        file("orders_913_9.player.rc.egf", 4000);
        file("orders_914_8.player.rc.egf", 4000);
        file("game_913_8.player.rr.egf", 4000);

        assertEquals(List.of(oldest, middle, newest),
                WorldControler.listCommandFilesOldestFirst(folder.toFile(), "orders_913_8."));
    }

    @Test void equalTimestampsUseFilenameOrder() throws IOException {
        File second = file("orders_913_8.z-player.rc.egf", 2000);
        File first = file("orders_913_8.a-player.rc.egf", 2000);

        assertEquals(List.of(first, second),
                WorldControler.listCommandFilesOldestFirst(folder.toFile(), "orders_913_8."));
    }

    @Test void emptyFolderHasNoOrders() {
        assertEquals(List.of(),
                WorldControler.listCommandFilesOldestFirst(folder.toFile(), "orders_913_8."));
    }

    private File file(String name, long modified) throws IOException {
        Path path = Files.createFile(folder.resolve(name));
        Files.setLastModifiedTime(path, FileTime.fromMillis(modified));
        return path.toFile();
    }
}
