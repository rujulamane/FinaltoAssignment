import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoggerDemo {

    public static void main(String[] args) throws Exception {

        Path logFile = Path.of("application.log");

        try (AsyncFileLogger logger = new AsyncFileLogger(logFile, 100)) {

            ExecutorService executor = Executors.newFixedThreadPool(5);

            for (int i = 1; i <= 50; i++) {
                int logNumber = i;

                executor.submit(() -> {
                    logger.log("Log message number " + logNumber);
                });
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        System.out.println("Logging completed. Check application.log file.");
    }
}
