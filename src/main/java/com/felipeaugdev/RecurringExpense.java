package com.felipeaugdev;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "recurring_expenses")
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private int frequencyDays;

    @Column(nullable = false)
    private LocalDate nextOccurrenceDate;

    @Column(nullable = false)
    private boolean active = true;

    public RecurringExpense() {
    }

    public RecurringExpense(BigDecimal amount, String description, Category category,
            int frequencyDays, LocalDate nextOccurrenceDate, User user) {

        this.amount = amount;
        this.description = description;
        this.category = category;
        this.frequencyDays = frequencyDays;
        this.nextOccurrenceDate = nextOccurrenceDate;
        this.user = user;
        this.active = true;
    }

    // GETTERS & SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getFrequencyDays() {
        return frequencyDays;
    }

    public void setFrequencyDays(int frequencyDays) {
        this.frequencyDays = frequencyDays;
    }

    public LocalDate getNextOccurrenceDate() {
        return nextOccurrenceDate;
    }

    public void setNextOccurrenceDate(LocalDate nextOccurrenceDate) {
        this.nextOccurrenceDate = nextOccurrenceDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
