package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Role;
import com.SpringBootMVC.ExpensesTracker.repository.CategoryRepository;
import com.SpringBootMVC.ExpensesTracker.repository.RoleRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ReferenceDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;

    public ReferenceDataInitializer(
            CategoryRepository categoryRepository,
            RoleRepository roleRepository) {
        this.categoryRepository = categoryRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (roleRepository.findByName("ROLE_STANDARD") == null) {
            roleRepository.save(new Role(1, "ROLE_STANDARD"));
        }

        List<Category> categories = List.of(
                new Category(1, "groceries"),
                new Category(2, "Utilities(bills)"),
                new Category(3, "transportation"),
                new Category(4, "dining out"),
                new Category(5, "entertainment"),
                new Category(6, "shopping"),
                new Category(7, "travel"),
                new Category(8, "education"));

        for (Category category : categories) {
            if (categoryRepository.findByName(category.getName()) == null) {
                categoryRepository.save(category);
            }
        }
    }
}
