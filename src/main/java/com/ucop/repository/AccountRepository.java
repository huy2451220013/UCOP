package com.ucop.repository;

import com.ucop.entity.Account;
import com.ucop.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsernameIgnoreCase(String username);
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT DISTINCT a FROM Account a LEFT JOIN a.roles r " +
           "WHERE (:keyword IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:role IS NULL OR r.roleName = :role) " +
           "ORDER BY a.id ASC")
    List<Account> searchAccounts(@Param("keyword") String keyword, @Param("role") UserRole role);

    @Query("SELECT COUNT(DISTINCT a) FROM Account a JOIN a.roles r WHERE r.roleName = :role")
    long countByRole(@Param("role") UserRole role);
}
