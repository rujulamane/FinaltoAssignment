# Async File Logger
This system implements an asynchronous, thread-safe logging mechanism in Java.
The logger:
1. Accepts the logs from multiple threads.
2. Write into one file.
3. Keeps the correct order of logs.
4. Does not slow down thread.

## Problem Statement
let's consider 5 threads are writting :
   Thread 1-> A
   Thread 2-> B
   Thread 3-> C
   Thread 4-> D
   Thread 5-> E
1. File corruption:
   If all threads write directly → output may become: ABCDE, or even broken lines.
2. Blocking
   If we do synchronized writeToFile(), Only one thread can write → others wait and System becomes slow.
   
## Solution
To solve these issues Use a Producer–Consumer Design. so basically Threads = Producers → they generate logs and Logger = Consumer → writes logs to file.

Flow will be:  Producer Threads → BlockingQueue → Writer Thread → File.

Key Features:
1. Thread Safety
- Uses `BlockingQueue` to handle concurrent log submissions
- Avoids explicit synchronization
- Ensures safe interaction between threads
2. Asynchronous Logging
- Producer threads only enqueue messages
- File writing is handled by a separate thread
- Reduces latency for logging calls
3. Ordering Guarantee
- Maintains FIFO (First-In-First-Out) order of messages in the queue
- Logs are written in the order they are received
Note: Due to concurrent execution, the order reflects arrival time, not numeric sequence.
4. Bounded Queue (Backpressure)
- Uses `ArrayBlockingQueue` with fixed capacity
- Prevents excessive memory usage
- If the queue is full, producer threads wait until space is available
5. Graceful Shutdown
- Implements `Closeable`
- Uses a poison pill mechanism to stop the writer thread
- Ensures all queued messages are written before shutdown
6. Log Formatting
Each log entry includes:
- Timestamp
- Thread name
- Log message

Example:
[2026-05-02 16:45:37.963] [pool-1-thread-4] Log message number 4, I have added Output file also( application.log) 

## Project Structure

AsyncFileLogger.java      -> Core logger implementation  
LoggerDemo.java           -> Demo to simulate concurrent logging  
AsyncFileLoggerTest.java  -> Unit tests  
README.md                 -> Documentation  

### Compile and Run Demo

javac AsyncFileLogger.java LoggerDemo.java  
java LoggerDemo  

Check output file:
type application.log  

## Testing Strategy
Positive Tests
- Single log message
- Multi-threaded logging
- Sequential log order
- High-volume logging

Negative Tests
- Logging after logger is closed
- Null log message
- Invalid file path
- Invalid queue capacity

## Limitations

- No log levels (INFO, ERROR, etc.)
- No log rotation
- No retry mechanism on failure
- Only file output supported


## Possible Enhancements

- Add log levels
- Implement log rotation
- Batch writes for performance
- Support multiple output targets
- Add configuration support

This project demonstrates: Thread-safe design, Producer–consumer pattern, Efficient handling of concurrent logging, Real-world system design principles.
The solution is scalable, efficient, and suitable for concurrent logging scenarios

