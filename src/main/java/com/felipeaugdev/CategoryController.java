package com.felipeaugdev;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.felipeaugdev.dto.CategoryRequest;
import com.felipeaugdev.dto.CategoryResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    @GetMapping
    public List<CategoryResponse> getCategories() {
        User currentUser = getCurrentUser();

        List<Category> categories;
        if (currentUser == null) {
            // Guest -> only system categories
            categories = categoryRepository.findByUserIsNull();
        } else {
            // Logged-in user -> system categories + their own
            categories = categoryRepository.findByUserIsNullOrUserId(currentUser.getId());
        }

        return categories.stream()
                .sorted((c1, c2) -> {
                    // System categories (user == null) first
                    boolean c1System = c1.getUser() == null;
                    boolean c2System = c2.getUser() == null;

                    if (c1System && !c2System)
                        return -1;
                    if (!c1System && c2System)
                        return 1;

                    return c1.getId().compareTo(c2.getId());
                })
                .map(CategoryResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged in to create a category");
        }

        String name = request.getName().trim().toUpperCase();

        if (categoryRepository.findByNameAndUserIsNull(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This category name is reserved");
        }
        if (categoryRepository.findByNameAndUserId(name, currentUser.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a category with this name");
        }

        Category saved = categoryRepository.save(new Category(name, currentUser));
        return CategoryResponse.from(saved);
    }
}
