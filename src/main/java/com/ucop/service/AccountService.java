package com.ucop.service;

import com.ucop.dto.RegisterRequest;
import com.ucop.entity.Account;
import com.ucop.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface AccountService {
    Optional<Account> authenticate(String username, String rawPassword);
    Account registerCustomer(RegisterRequest req);
    Account createAccountByAdmin(String username, String rawPassword, String email,
                                 String fullName, String phone, UserRole role, String adminUser);
    Account updateRole(Long accountId, UserRole newRole, String performedBy);
    void toggleLock(Long accountId, boolean lock, String performedBy);
    void updateProfile(Long accountId, String fullName, String phone, String address);
    void changePassword(Long accountId, String oldPassword, String newPassword);
    Optional<Account> findById(Long id);
    List<Account> searchAccounts(String keyword, UserRole role);
}
