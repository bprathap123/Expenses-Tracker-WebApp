package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.repository.ExpenseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    ExpenseRepository expenseRepository;
    ClientService clientService;
    CategoryService categoryService;
    EntityManager entityManager;

    @Autowired
    public ExpenseServiceImpl(ExpenseRepository expenseRepository, ClientService clientService
            , CategoryService categoryService, EntityManager entityManager) {
        this.expenseRepository = expenseRepository;
        this.clientService = clientService;
        this.categoryService = categoryService;
        this.entityManager = entityManager;
    }


    @Override
    public Expense findExpenseById(int id) {
        return expenseRepository.findById(id).orElse(null);
    }

    @Transactional
    @Override
    public void save(ExpenseDTO expenseDTO) {
        System.out.println(expenseDTO);
        Expense expense = new Expense();
        expense.setAmount(expenseDTO.getAmount());
        expense.setDateTime(expenseDTO.getDateTime());
        expense.setDescription(expenseDTO.getDescription());
        expense.setClient(clientService.findClientById(expenseDTO.getClientId()));
        Category category = categoryService.findCategoryByName(expenseDTO.getCategory());
        expense.setCategory(category);
        expenseRepository.save(expense);
    }

    @Override
    public void update(ExpenseDTO expenseDTO) {
        Expense existingExpense = expenseRepository.findById(expenseDTO.getExpenseId()).orElse(null);
        existingExpense.setAmount(expenseDTO.getAmount());
        existingExpense.setDateTime(expenseDTO.getDateTime());
        existingExpense.setDescription(expenseDTO.getDescription());
        Category category = categoryService.findCategoryByName(expenseDTO.getCategory());
        existingExpense.setCategory(category);
        expenseRepository.save(existingExpense);
    }

    @Override
    public List<Expense> findAllExpenses() {
        return expenseRepository.findAll();
    }

    @Override
    public List<Expense> findAllExpensesByClientId(int id) {
        return expenseRepository.findByClientId(id);
    }

    @Override
    public void deleteExpenseById(int id) {
        expenseRepository.deleteById(id);
    }

    @Override
    public List<Expense> findFilterResult(FilterDTO filter, int clientId) {
        StringBuilder query = new StringBuilder(
                "select e from Expense e where e.client.id = :clientId and e.amount between :from and :to"
        );
        boolean filterByCategory = filter.getCategory() != null
                && !"all".equalsIgnoreCase(filter.getCategory());
        boolean filterByYear = filter.getYear() != null
                && !"all".equalsIgnoreCase(filter.getYear());
        boolean filterByMonth = filter.getMonth() != null
                && !"all".equalsIgnoreCase(filter.getMonth());

        if (filterByCategory) {
            query.append(" and e.category.name = :category");
        }
        if (filterByYear) {
            query.append(" and substring(e.dateTime, 1, 4) = :year");
        }
        if (filterByMonth) {
            query.append(" and substring(e.dateTime, 6, 2) = :month");
        }

        TypedQuery<Expense> expenseQuery = entityManager.createQuery(query.toString(), Expense.class)
                .setParameter("clientId", clientId)
                .setParameter("from", filter.getFrom())
                .setParameter("to", filter.getTo());
        if (filterByCategory) {
            expenseQuery.setParameter("category", filter.getCategory());
        }
        if (filterByYear) {
            expenseQuery.setParameter("year", filter.getYear());
        }
        if (filterByMonth) {
            expenseQuery.setParameter("month", filter.getMonth());
        }
        return expenseQuery.getResultList();
    }




}
