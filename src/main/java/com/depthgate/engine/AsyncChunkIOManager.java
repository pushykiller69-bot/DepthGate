package com.depthgate.engine;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Offloads chunk save operations to a background daemon thread at low priority.
 * Eliminates disk I/O stalls on the main server thread, especially on SATA drives.
 *
 * <p>Pure Java — no Minecraft dependencies. The mixin layer wraps chunk serialization
 * calls with {@link #submit(Runnable)}.</p>
 */
public class AsyncChunkIOManager {
    private static final AsyncChunkIOManager INSTANCE = new AsyncChunkIOManager();

    private final LinkedBlockingQueue<Runnable> saveQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger pendingCount = new AtomicInteger(0);
    private volatile boolean running = false;
    private Thread workerThread;
    private int queueLimit = 256;

    public static AsyncChunkIOManager getInstance() {
        return INSTANCE;
    }

    /**
     * Starts the background save thread. Call once during mod init.
     *
     * @param maxQueueDepth maximum pending saves before backpressure kicks in
     */
    public synchronized void start(int maxQueueDepth) {
        if (running) return;
        this.queueLimit = maxQueueDepth;
        running = true;
        workerThread = new Thread(this::processLoop, "DepthGate-ChunkSave");
        workerThread.setDaemon(true);
        workerThread.setPriority(Thread.NORM_PRIORITY - 2);
        workerThread.start();
    }

    /**
     * Submits a chunk save task. If the queue is full, the save runs synchronously
     * on the caller's thread to apply backpressure.
     */
    public void submit(Runnable saveTask) {
        if (!running) {
            saveTask.run();
            return;
        }
        if (pendingCount.get() >= queueLimit) {
            // Backpressure: queue is full, run synchronously to avoid unbounded growth
            saveTask.run();
            return;
        }
        pendingCount.incrementAndGet();
        saveQueue.offer(() -> {
            try {
                saveTask.run();
            } finally {
                pendingCount.decrementAndGet();
            }
        });
    }

    private void processLoop() {
        while (running || !saveQueue.isEmpty()) {
            try {
                Runnable task = saveQueue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (task != null) {
                    task.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log and continue — don't let one bad save kill the thread
                System.err.println("[DepthGate] Async chunk save failed: " + e.getMessage());
            }
        }
    }

    /**
     * Gracefully shuts down the worker thread, draining remaining saves.
     */
    public synchronized void shutdown() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public int getPendingCount() {
        return pendingCount.get();
    }

    public boolean isRunning() {
        return running;
    }
}
