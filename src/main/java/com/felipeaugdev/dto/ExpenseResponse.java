package com.felipeaugdev.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.felipeaugdev.Expense;

public class ExpenseResponse {
    private Integer id;
    private BigDecimal amount;
    private LocalDate date;
    private String description;
    private String category;
    private boolean recurring;
    private Integer frequencyDays;

    public ExpenseResponse() {
    }

    public ExpenseResponse(Integer id, BigDecimal amount, LocalDate date, String description,
            String category, boolean recurring, Integer frequencyDays) {
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.category = category;
        this.recurring = recurring;
        this.frequencyDays = frequencyDays;
    }

    public static ExpenseResponse from(Expense expense) {
        boolean isRecurring = expense.getRecurringExpense() != null
                && expense.getRecurringExpense().isActive();
        Integer freq = isRecurring ? expense.getRecurringExpense().getFrequencyDays() : null;

        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDate(),
                expense.getDescription(),
                expense.getCategory() != null ? expense.getCategory().getName() : null,
                isRecurring,
                freq);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean getRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public Integer getFrequencyDays() {
        return frequencyDays;
    }

    public void setFrequencyDays(Integer frequencyDays) {
        this.frequencyDays = frequencyDays;
    }
}
