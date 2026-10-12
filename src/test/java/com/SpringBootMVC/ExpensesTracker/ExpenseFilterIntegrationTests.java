package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.repository.CategoryRepository;
import com.SpringBootMVC.ExpensesTracker.repository.ClientRepository;
import com.SpringBootMVC.ExpensesTracker.repository.ExpenseRepository;
import com.SpringBootMVC.ExpensesTracker.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:expense-filter;MODE=MySQL;NON_KEYWORDS=USER"
})
class ExpenseFilterIntegrationTests {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @Transactional
    void filtersExpensesByCriteriaAndLoggedInClient() {
        Client owner = clientRepository.save(new Client("Filter", "Owner", "owner@example.invalid"));
        Client anotherClient = clientRepository.save(
                new Client("Other", "Client", "other@example.invalid")
        );
        Category groceries = categoryRepository.findByName("groceries");
        assertTrue(groceries != null);

        Expense matchingExpense = saveExpense(owner, groceries, 25, "2026-10-12T12:00:00");
        saveExpense(owner, groceries, 30, "2026-09-12T12:00:00");
        saveExpense(anotherClient, groceries, 25, "2026-10-12T12:00:00");

        FilterDTO filter = new FilterDTO();
        filter.setCategory("groceries");
        filter.setFrom(20);
        filter.setTo(25);
        filter.setMonth("10");
        filter.setYear("2026");

        List<Expense> results = expenseService.findFilterResult(filter, owner.getId());

        assertEquals(List.of(matchingExpense.getId()), results.stream().map(Expense::getId).toList());
    }

    private Expense saveExpense(Client client, Category category, int amount, String dateTime) {
        return expenseRepository.save(new Expense(amount, dateTime, "filter test", client, category));
    }
}
