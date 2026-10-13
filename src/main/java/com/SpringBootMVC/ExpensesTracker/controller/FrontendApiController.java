package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.WebUser;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.service.CategoryService;
import com.SpringBootMVC.ExpensesTracker.service.ExpenseService;
import com.SpringBootMVC.ExpensesTracker.service.PasswordResetService;
import com.SpringBootMVC.ExpensesTracker.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class FrontendApiController {

    private final ExpenseService expenseService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final ClientRegistrationRepository registrations;

    public FrontendApiController(
            ExpenseService expenseService,
            CategoryService categoryService,
            UserService userService,
            PasswordResetService passwordResetService,
            ClientRegistrationRepository registrations) {
        this.expenseService = expenseService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.registrations = registrations;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new SessionResponse(false, null, null);
        }

        User user = userService.findUserByUserName(authentication.getName());
        String firstName = user == null || user.getClient() == null
                ? authentication.getName()
                : user.getClient().getFirstName();
        return new SessionResponse(true, authentication.getName(), firstName);
    }

    @GetMapping("/config")
    public FrontendConfig config() {
        return new FrontendConfig(
                registrations.findByRegistrationId("google") != null,
                registrations.findByRegistrationId("facebook") != null,
                passwordResetService.isEmailDeliveryConfigured());
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody WebUser webUser) {
        if (userService.findUserByUserName(webUser.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "That username already has an account."));
        }
        userService.save(webUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created. You can sign in now."));
    }

    @GetMapping("/expenses")
    public List<ExpenseResponse> expenses(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "2147483647") int to,
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "all") String month,
            @RequestParam(defaultValue = "all") String year,
            HttpSession session) {
        if (from < 0 || to < from) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid amount range.");
        }

        Client client = requireClient(session);
        FilterDTO filter = new FilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setCategory(category);
        filter.setMonth(month);
        filter.setYear(year);
        List<Expense> expenses = isUnfiltered(filter)
                ? expenseService.findAllExpensesByClientId(client.getId())
                : expenseService.findFilterResult(filter, client.getId());
        return expenses.stream().map(this::toResponse).toList();
    }

    @PostMapping("/expenses")
    public ResponseEntity<Void> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            HttpSession session) {
        validateExpense(request);
        ExpenseDTO expense = toDto(request);
        expense.setClientId(requireClient(session).getId());
        expenseService.save(expense);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<Void> updateExpense(
            @PathVariable int id,
            @Valid @RequestBody ExpenseRequest request,
            HttpSession session) {
        validateExpense(request);
        Client client = requireClient(session);
        Expense existingExpense = requireOwnedExpense(id, client);

        ExpenseDTO expense = toDto(request);
        expense.setExpenseId(existingExpense.getId());
        expense.setClientId(client.getId());
        expenseService.update(expense);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable int id, HttpSession session) {
        Client client = requireClient(session);
        requireOwnedExpense(id, client);
        expenseService.deleteExpenseById(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    private boolean isUnfiltered(FilterDTO filter) {
        return filter.getFrom() == 0
                && filter.getTo() == Integer.MAX_VALUE
                && "all".equalsIgnoreCase(filter.getCategory())
                && "all".equalsIgnoreCase(filter.getMonth())
                && "all".equalsIgnoreCase(filter.getYear());
    }

    private void validateExpense(ExpenseRequest request) {
        if (request.amount() == null || request.amount() < 0) {
            throw new IllegalArgumentException("Enter a valid amount.");
        }
        if (request.category() == null || categoryService.findCategoryByName(request.category()) == null) {
            throw new IllegalArgumentException("Choose a valid expense category.");
        }
        if (request.dateTime() == null || request.dateTime().isBlank()) {
            throw new IllegalArgumentException("Enter a date and time.");
        }
        try {
            LocalDateTime.parse(request.dateTime());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Enter a valid date and time.");
        }
        if (request.description() != null && request.description().length() > 400) {
            throw new IllegalArgumentException("Description must be 400 characters or fewer.");
        }
    }

    private ExpenseDTO toDto(ExpenseRequest request) {
        ExpenseDTO expense = new ExpenseDTO();
        expense.setAmount(request.amount());
        expense.setCategory(request.category());
        expense.setDateTime(request.dateTime());
        expense.setDescription(request.description());
        return expense;
    }

    private ExpenseResponse toResponse(Expense expense) {
        Category category = expense.getCategory();
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDateTime(),
                category == null ? "No Category" : category.getName(),
                expense.getDescription());
    }

    private Client requireClient(HttpSession session) {
        Object client = session.getAttribute("client");
        if (client instanceof Client authenticatedClient) {
            return authenticatedClient;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
    }

    private Expense requireOwnedExpense(int id, Client client) {
        Expense expense = expenseService.findExpenseById(id);
        if (expense == null || expense.getClient() == null
                || expense.getClient().getId() != client.getId()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found.");
        }
        return expense;
    }

    public record SessionResponse(boolean authenticated, String username, String firstName) {}

    public record FrontendConfig(
            boolean googleLoginEnabled,
            boolean facebookLoginEnabled,
            boolean passwordResetEnabled) {}

    public record ExpenseRequest(
            Integer amount,
            String category,
            String dateTime,
            String description) {}

    public record ExpenseResponse(
            int id,
            int amount,
            String dateTime,
            String category,
            String description) {}
}
