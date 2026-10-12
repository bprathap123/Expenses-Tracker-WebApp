package com.SpringBootMVC.ExpensesTracker.repository;

import com.SpringBootMVC.ExpensesTracker.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    void deleteByUser_Id(int userId);
    PasswordResetToken findByTokenHashAndUsedFalse(String tokenHash);
}
