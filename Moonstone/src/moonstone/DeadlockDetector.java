package moonstone;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

interface DeadlockHandler {
    void handleDeadlock(final ThreadInfo[] deadlockedThreads);
}

class DeadlockDetector {
    private final DeadlockHandler deadlockHandler;
    private final long period;
    private final TimeUnit unit;
    private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledFuture;

    public DeadlockDetector(final DeadlockHandler deadlockHandler,
                            final long period, final TimeUnit unit) {
        this.deadlockHandler = deadlockHandler;
        this.period = period;
        this.unit = unit;
    }

    private void deadlockCheck() {
        long[] deadlockedThreadIds = mbean.findDeadlockedThreads();

        if (deadlockedThreadIds != null) {
            ThreadInfo[] threadInfos = mbean.getThreadInfo(deadlockedThreadIds, true, true);

            deadlockHandler.handleDeadlock(threadInfos);
        }
    }

    public void start() {
        scheduledFuture = scheduler.scheduleAtFixedRate(this::deadlockCheck, this.period, this.period, this.unit);
    }

    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }
}