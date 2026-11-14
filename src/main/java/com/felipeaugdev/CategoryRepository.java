package com.felipeaugdev;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameAndUserIsNull(String name);

    Optional<Category> findByNameAndUserId(String name, Long userId);

    default Optional<Category> findVisibleByName(String name, User user) {
        Optional<Category> system = findByNameAndUserIsNull(name);
        if (system.isPresent() || user == null) {
            return system;
        }
        return findByNameAndUserId(name, user.getId());
    }

    List<Category> findByUserIsNull();

    List<Category> findByUserId(Long userId);

    List<Category> findByUserIsNullOrUserId(Long userId);

}
