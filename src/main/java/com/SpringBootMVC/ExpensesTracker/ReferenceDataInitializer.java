package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Role;
import com.SpringBootMVC.ExpensesTracker.repository.CategoryRepository;
import com.SpringBootMVC.ExpensesTracker.repository.RoleRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ReferenceDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;
    private final DataSource dataSource;

    public ReferenceDataInitializer(
            CategoryRepository categoryRepository,
            RoleRepository roleRepository,
            DataSource dataSource) {
        this.categoryRepository = categoryRepository;
        this.roleRepository = roleRepository;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if ("MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("""
                        CREATE TABLE IF NOT EXISTS users_roles (
                            user_id INT NOT NULL,
                            role_id INT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES `user` (id),
                            CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES `role` (id)
                        ) ENGINE=InnoDB
                        """);
                }
            }
        }

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
