package com.felipeaugdev;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recurring")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseController(RecurringExpenseService recurringExpenseService) {
        this.recurringExpenseService = recurringExpenseService;
    }

    /**
     * Manually trigger generation of all due recurring expenses.
     * Useful for testing.
     */
    @PostMapping("/generate")
    public ResponseEntity<String> generateNow() {
        int created = recurringExpenseService.generateDueExpenses();
        return ResponseEntity.ok("Generated " + created + " expense(s)");
    }
}
