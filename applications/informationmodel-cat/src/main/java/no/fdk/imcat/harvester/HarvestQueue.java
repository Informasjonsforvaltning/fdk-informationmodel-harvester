package no.fdk.imcat.harvester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.Queue;

@Service
public class HarvestQueue {
    private static final Logger logger = LoggerFactory.getLogger(HarvestQueue.class);

    private final Queue<String> scheduledTasks = new LinkedList<>();

    @PostConstruct
    @Scheduled(cron = "${application.harvestCron}")
    public void startHarvestAll() {
        String task = HarvestExecutor.HARVEST_ALL;
        logger.debug("Initial trigger task {}", task);
        addTask(task);
    }

    public void addTask(String task) {
        synchronized (scheduledTasks) {
            if (scheduledTasks.contains(task)) {
                logger.debug("Task already exists in queue, skipping: {}", task);
                return;
            }
            scheduledTasks.add(task);
            logger.debug("Task added to queue: {}", task);
        }
    }

    public String poll() {
        synchronized (scheduledTasks) {
            return scheduledTasks.poll();
        }
    }
}
