package com.felipeaugdev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringExpenseScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseScheduler.class);

    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseScheduler(RecurringExpenseService recurringExpenseService) {
        this.recurringExpenseService = recurringExpenseService;
    }

    /**
     * Runs every day at 00:05 (server time).
     * Cron format: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void generateDaily() {
        int created = recurringExpenseService.generateDueExpenses();
        if (created > 0) {
            log.info("Generated {} recurring expense(s)", created);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        int created = recurringExpenseService.generateDueExpenses();
        if (created > 0) {
            log.info("Generated {} recurring expense(s) on startup", created);
        }
    }
}
