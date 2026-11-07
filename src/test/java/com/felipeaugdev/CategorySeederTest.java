package com.felipeaugdev;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CategorySeederTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategorySeeder categorySeeder;

    @Test
    void testRunSeedsAllDefaultsWhenRepositoryIsEmpty() {
        when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());

        categorySeeder.run();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(8)).save(captor.capture());

        List<String> savedNames = captor.getAllValues().stream()
                .map(Category::getName)
                .toList();

        List<String> expectedNames = List.of(
                "FOOD", "TRANSPORTATION", "UTILITIES", "HOUSING",
                "ENTERTAINMENT", "HEALTHCARE", "SHOPPING", "OTHER"
        );

        assertEquals(expectedNames, savedNames);
    }

    @Test
    void testRunSkipsExistingCategories() {
        when(categoryRepository.findByName("FOOD")).thenReturn(Optional.of(new Category("FOOD")));
        when(categoryRepository.findByName(argThat(name -> !"FOOD".equals(name)))).thenReturn(Optional.empty());

        categorySeeder.run();

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(7)).save(captor.capture());

        List<String> savedNames = captor.getAllValues().stream()
                .map(Category::getName)
                .toList();

        assertFalse(savedNames.contains("FOOD"));
        assertTrue(savedNames.contains("TRANSPORTATION"));
        assertTrue(savedNames.contains("OTHER"));
    }

    @Test
    void testRunDoesNothingWhenAllCategoriesAlreadyExist() {
        when(categoryRepository.findByName(anyString()))
                .thenAnswer(invocation -> Optional.of(new Category(invocation.getArgument(0))));

        categorySeeder.run();

        verify(categoryRepository, never()).save(any(Category.class));
    }
}
