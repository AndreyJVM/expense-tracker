package com.felipeaugdev;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        String[] defaults = {
                "FOOD", "TRANSPORTATION", "UTILITIES", "HOUSING",
                "ENTERTAINMENT", "HEALTHCARE", "SHOPPING", "OTHER"
        };

        for (String name : defaults) {
            if (categoryRepository.findByNameAndUserIsNull(name).isEmpty()) {
                categoryRepository.save(new Category(name));
            }
        }
    }
}
