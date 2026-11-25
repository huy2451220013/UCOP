package com.ucop.service.impl;

import com.ucop.dto.RegisterRequest;
import com.ucop.entity.*;
import com.ucop.enums.UserRole;
import com.ucop.repository.*;
import com.ucop.service.AccountService;
import com.ucop.service.AuditService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final WalletRepository walletRepository;
    private final AuditService auditService;

    public AccountServiceImpl(AccountRepository accountRepository,
                              RoleRepository roleRepository,
                              WalletRepository walletRepository,
                              AuditService auditService) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.walletRepository = walletRepository;
        this.auditService = auditService;
    }

    private Role getOrCreateRole(UserRole userRole) {
        return roleRepository.findByRoleName(userRole)
                .orElseGet(() -> roleRepository.save(new Role(userRole, "Vai tro " + userRole.name())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> authenticate(String username, String rawPassword) {
        return accountRepository.findByUsernameIgnoreCase(username.trim())
                .filter(acc -> {
                    try {
                        return BCrypt.checkpw(rawPassword, acc.getPassword());
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    @Override
    public Account registerCustomer(RegisterRequest req) {
        return createAccountInternal(
                req.getUsername().trim(),
                req.getPassword(),
                req.getEmail().trim(),
                req.getFullName(),
                req.getPhone(),
                req.getAddress(),
                UserRole.CUSTOMER,
                req.getUsername().trim()
        );
    }

    @Override
    public Account createAccountByAdmin(String username, String rawPassword, String email,
                                        String fullName, String phone, UserRole role, String adminUser) {
        return createAccountInternal(username.trim(), rawPassword, email.trim(),
                fullName, phone, "", role, adminUser);
    }

    private Account createAccountInternal(String username, String rawPassword, String email,
                                          String fullName, String phone, String address,
                                          UserRole role, String performedBy) {
        if (accountRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + username + "' đã tồn tại!");
        }
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng!");
        }

        Account acc = new Account();
        acc.setUsername(username);
        acc.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)));
        acc.setEmail(email);
        acc.setStatus("ACTIVE");
        acc.setCreatedBy(performedBy);
        acc.setUpdatedBy(performedBy);
        acc.getRoles().add(getOrCreateRole(role));

        AccountProfile profile = new AccountProfile(fullName, phone, address);
        acc.setProfile(profile);

        Account saved = accountRepository.save(acc);

        Wallet wallet = new Wallet();
        wallet.setAccount(saved);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);

        auditService.log("CREATE_USER", "Account", saved.getId(), performedBy,
                null, role.name(), "Tạo tài khoản mới: " + username);
        return saved;
    }

    @Override
    public Account updateRole(Long accountId, UserRole newRole, String performedBy) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
        String oldRole = acc.getPrimaryRoleName();
        acc.getRoles().clear();
        acc.getRoles().add(getOrCreateRole(newRole));
        acc.setUpdatedBy(performedBy);
        Account saved = accountRepository.save(acc);

        auditService.log("ROLE_CHANGE", "Account", saved.getId(), performedBy,
                oldRole, newRole.name(), "Đổi quyền tài khoản " + acc.getUsername());
        return saved;
    }

    @Override
    public void toggleLock(Long accountId, boolean lock, String performedBy) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
        String oldStatus = acc.getStatus();
        String newStatus = lock ? "LOCKED" : "ACTIVE";
        acc.setStatus(newStatus);
        acc.setUpdatedBy(performedBy);
        accountRepository.save(acc);

        auditService.log(lock ? "LOCK_USER" : "UNLOCK_USER", "Account", acc.getId(), performedBy,
                oldStatus, newStatus, (lock ? "Khóa" : "Mở khóa") + " tài khoản " + acc.getUsername());
    }

    @Override
    public void updateProfile(Long accountId, String fullName, String phone, String address) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
        AccountProfile p = acc.getProfile();
        if (p == null) {
            p = new AccountProfile();
            acc.setProfile(p);
        }
        p.setFullName(fullName);
        p.setPhone(phone);
        p.setAddress(address);
        acc.setUpdatedBy(acc.getUsername());
        accountRepository.save(acc);
    }

    @Override
    public void changePassword(Long accountId, String oldPassword, String newPassword) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
        if (!BCrypt.checkpw(oldPassword, acc.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác!");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải từ 6 ký tự trở lên!");
        }
        acc.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt(10)));
        acc.setUpdatedBy(acc.getUsername());
        accountRepository.save(acc);
        auditService.log("CHANGE_PASSWORD", "Account", acc.getId(), acc.getUsername(),
                "***", "***", "Người dùng đổi mật khẩu");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> searchAccounts(String keyword, UserRole role) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return accountRepository.searchAccounts(kw, role);
    }
}
