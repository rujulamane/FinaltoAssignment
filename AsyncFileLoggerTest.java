import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AsyncFileLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteSingleLogMessageToFile() throws Exception {
        Path logFile = tempDir.resolve("single.log");

        try (AsyncFileLogger logger = new AsyncFileLogger(logFile, 10)) {
            logger.log("Hello logger");
        }

        List<String> lines = Files.readAllLines(logFile);

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("Hello logger"));
    }

    @Test
    void shouldWriteLogsFromMultipleThreads() throws Exception {
        Path logFile = tempDir.resolve("multi-thread.log");

        int totalMessages = 100;

        try (AsyncFileLogger logger = new AsyncFileLogger(logFile, 50)) {
            ExecutorService executor = Executors.newFixedThreadPool(10);

            for (int i = 1; i <= totalMessages; i++) {
                int messageNumber = i;
                executor.submit(() -> logger.log("Message " + messageNumber));
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        List<String> lines = Files.readAllLines(logFile);

        assertEquals(totalMessages, lines.size());
    }

    @Test
    void shouldPreserveOrderForSequentialLogs() throws Exception {
        Path logFile = tempDir.resolve("order.log");

        try (AsyncFileLogger logger = new AsyncFileLogger(logFile, 10)) {
            logger.log("First");
            logger.log("Second");
            logger.log("Third");
        }

        List<String> lines = Files.readAllLines(logFile);

        assertTrue(lines.get(0).contains("First"));
        assertTrue(lines.get(1).contains("Second"));
        assertTrue(lines.get(2).contains("Third"));
    }

    @Test
    void shouldRejectLogsAfterClose() throws Exception {
        Path logFile = tempDir.resolve("closed.log");

        AsyncFileLogger logger = new AsyncFileLogger(logFile, 10);
        logger.close();

        assertThrows(IllegalStateException.class, () -> {
            logger.log("This should fail");
        });
    }

    @Test
    void shouldRejectNullMessage() throws Exception {
        Path logFile = tempDir.resolve("null.log");

        try (AsyncFileLogger logger = new AsyncFileLogger(logFile, 10)) {
            assertThrows(IllegalArgumentException.class, () -> {
                logger.log(null);
            });
        }
    }

    @Test
    void shouldHandleHighVolumeLogging() throws Exception {
        Path logFile = tempDir.resolve("stress.log");

        int totalMessages = 1000;

        try (AsyncFileLogger logger = new AsyncFileLogger(logFile, 100)) {
            for (int i = 1; i <= totalMessages; i++) {
                logger.log("Stress message " + i);
            }
        }

        List<String> lines = Files.readAllLines(logFile);

        assertEquals(totalMessages, lines.size());
    }

    @Test
    void shouldFailForInvalidFilePath() {
        Path invalidPath = Path.of("Z:/invalid-folder/log.txt");

        assertThrows(IOException.class, () -> {
            new AsyncFileLogger(invalidPath, 10);
        });
    }
}
