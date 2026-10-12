package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.service.CategoryService;
import com.SpringBootMVC.ExpensesTracker.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class AddExpensePageTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private CategoryService categoryService;

    @Test
    void homePageRendersLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing-page"));
    }

    @Test
    @WithMockUser
    void showAddRendersExpenseForm() throws Exception {
        mockMvc.perform(get("/showAdd"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Add Expense")));
    }

    @Test
    @WithMockUser
    void listRendersExpenseWithInvalidDateAndMissingCategory() throws Exception {
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

        mockMvc.perform(get("/list").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unavailable")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unknown category")));
    }

    @Test
    @WithMockUser
    void showUpdateRendersExpenseWithoutCategory() throws Exception {
        Client client = new Client();
        client.setId(7);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("client", client);

        Expense expense = new Expense();
        expense.setId(16);
        expense.setClient(client);
        expense.setDateTime("2026-10-07T21:00");
        when(expenseService.findExpenseById(16)).thenReturn(expense);

        mockMvc.perform(get("/showUpdate").param("expId", "16").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Update Expense")));
    }
}
