package com.felipeaugdev.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.felipeaugdev.CategoryRepository;

class ApplicationContextIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void appStartsAndSystemCategoriesAreSeeded() {
        assertTrue(categoryRepository.findByNameAndUserIsNull("FOOD").isPresent());
    }
}
