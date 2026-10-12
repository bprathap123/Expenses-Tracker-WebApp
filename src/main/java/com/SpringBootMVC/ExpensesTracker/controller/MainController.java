package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.service.CategoryService;
import com.SpringBootMVC.ExpensesTracker.service.ExpenseService;
import jakarta.servlet.http.HttpSession;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private final ExpenseService expenseService;
    private final CategoryService categoryService;

    @Autowired
    public MainController(ExpenseService expenseService, CategoryService categoryService) {
        this.expenseService = expenseService;
        this.categoryService = categoryService;
    }

    @GetMapping("/showAdd")
    public String showAdd(Model model) {
        model.addAttribute("expense", new ExpenseDTO());
        return "add-expense";
    }

    @PostMapping("/submitAdd")
    public String submitAdd(@ModelAttribute("expense") ExpenseDTO expenseDTO, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/showLoginPage";
        }

        expenseDTO.setClientId(client.getId());
        expenseService.save(expenseDTO);
        return "redirect:/list";
    }

    @GetMapping("/showUpdate")
    public String showUpdate(@RequestParam("expId") int expenseId, Model model, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/showLoginPage";
        }

        Expense expense = expenseService.findExpenseById(expenseId);
        if (expense == null || expense.getClient() == null
                || expense.getClient().getId() != client.getId()) {
            return "redirect:/list";
        }

        ExpenseDTO expenseDTO = new ExpenseDTO();
        expenseDTO.setAmount(expense.getAmount());
        if (expense.getCategory() != null) {
            expenseDTO.setCategory(expense.getCategory().getName());
        }
        expenseDTO.setDescription(expense.getDescription());
        expenseDTO.setDateTime(expense.getDateTime());

        model.addAttribute("expense", expenseDTO);
        model.addAttribute("expenseId", expenseId);
        return "update-page";
    }

    @PostMapping("/submitUpdate")
    public String submitUpdate(
            @RequestParam("expId") int expenseId,
            @ModelAttribute("expense") ExpenseDTO expenseDTO,
            HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/showLoginPage";
        }

        Expense existingExpense = expenseService.findExpenseById(expenseId);
        if (existingExpense == null || existingExpense.getClient() == null
                || existingExpense.getClient().getId() != client.getId()) {
            return "redirect:/list";
        }

        expenseDTO.setExpenseId(expenseId);
        expenseDTO.setClientId(client.getId());
        expenseService.update(expenseDTO);
        return "redirect:/list";
    }

    @GetMapping("/delete")
    public String deleteExpense(@RequestParam("expId") int expenseId, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/showLoginPage";
        }

        Expense expense = expenseService.findExpenseById(expenseId);
        if (expense == null || expense.getClient() == null
                || expense.getClient().getId() != client.getId()) {
            return "redirect:/list";
        }

        expenseService.deleteExpenseById(expenseId);
        return "redirect:/list";
    }

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {

        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/";
        }

        int clientId = client.getId();

        List<Expense> expenseList = expenseService.findAllExpensesByClientId(clientId);

        for (Expense expense : expenseList) {
            if (expense.getCategory() != null) {
                Category category = categoryService.findCategoryById(expense.getCategory().getId());
                expense.setCategoryName(category == null ? "Unknown category" : category.getName());
            } else {
                expense.setCategoryName("No Category");
            }

            String storedDateTime = expense.getDateTime();
            if (storedDateTime == null || storedDateTime.isBlank()) {
                logger.warn("Expense {} has a missing date/time value", expense.getId());
                expense.setDate("Unavailable");
                expense.setTime("Unavailable");
                continue;
            }

            try {
                LocalDateTime dateTime = LocalDateTime.parse(
                        storedDateTime,
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );
                expense.setDate(dateTime.toLocalDate().toString());
                expense.setTime(dateTime.toLocalTime().toString());
            } catch (DateTimeException exception) {
                logger.warn("Expense {} has an invalid date/time value", expense.getId());
                expense.setDate("Unavailable");
                expense.setTime("Unavailable");
            }
        }

        model.addAttribute("expenseList", expenseList);
        model.addAttribute("filter", new FilterDTO());

        return "list-page";
    }
}
