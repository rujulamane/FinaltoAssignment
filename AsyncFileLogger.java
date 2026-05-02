import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AsyncFileLogger implements Closeable {

    private static final LogMessage POISON_PILL = new LogMessage("__STOP__", true);

    private final BlockingQueue<LogMessage> queue;
    private final BufferedWriter writer;
    private final Thread writerThread;
    private volatile boolean closed = false;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public AsyncFileLogger(Path filePath, int queueCapacity) throws IOException {
        this.queue = new ArrayBlockingQueue<>(queueCapacity);

        this.writer = Files.newBufferedWriter(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );

        this.writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                processLogs();
            }
        }, "async-file-logger-writer");

        this.writerThread.start();
    }

    public void log(String message) {
        if (closed) {
            throw new IllegalStateException("Logger is already closed");
        }

        String formattedMessage = formatMessage(message);

        try {
            queue.put(new LogMessage(formattedMessage, false));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while submitting log message", e);
        }
    }

    private String formatMessage(String message) {
        return String.format(
                "[%s] [%s] %s",
                LocalDateTime.now().format(FORMATTER),
                Thread.currentThread().getName(),
                message
        );
    }

    private void processLogs() {
        try {
            while (true) {
                LogMessage logMessage = queue.take();

                if (logMessage.isPoisonPill()) {
                    break;
                }

                writer.write(logMessage.getMessage());
                writer.newLine();
            }

            writer.flush();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new RuntimeException("Error while writing logs", e);
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;

        try {
            queue.put(POISON_PILL);
            writerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Logger shutdown interrupted", e);
        } finally {
            writer.close();
        }
    }

    private static class LogMessage {
        private final String message;
        private final boolean poisonPill;

        public LogMessage(String message, boolean poisonPill) {
            this.message = message;
            this.poisonPill = poisonPill;
        }

        public String getMessage() {
            return message;
        }

        public boolean isPoisonPill() {
            return poisonPill;
        }
    }
}
