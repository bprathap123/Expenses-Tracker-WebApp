package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.controller.MainController;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.service.CategoryService;
import com.SpringBootMVC.ExpensesTracker.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MainControllerListTests {

    @Test
    void listRendersExpensesWithInvalidDateAndMissingCategory() {
        ExpenseService expenseService = mock(ExpenseService.class);
        CategoryService categoryService = mock(CategoryService.class);
        MainController controller = new MainController(expenseService, categoryService);

        Client client = new Client();
        client.setId(7);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("client", client);

        Expense expense = new Expense();
        expense.setId(12);
        expense.setDateTime("not-a-date");
        expense.setCategory(new Category(99, "missing"));
        when(expenseService.findAllExpensesByClientId(7)).thenReturn(List.of(expense));
        when(categoryService.findCategoryById(99)).thenReturn(null);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.list(model, session);

        assertEquals("list-page", view);
        assertEquals("Unknown category", expense.getCategoryName());
        assertEquals("Unavailable", expense.getDate());
        assertEquals("Unavailable", expense.getTime());
    }
}
