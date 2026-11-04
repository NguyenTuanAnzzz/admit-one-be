package com.an.admit_one.repository;

import com.an.admit_one.entity.User;
import com.an.admit_one.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByIdAndRole(UUID id, UserRole role);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Tìm theo tên hoặc email trong một vai trò. pattern luôn có giá trị ("%" khi không tìm), tránh tham số null trong LIKE
    @Query("""
            select u from User u
            where u.role = :role
              and (lower(u.fullName) like :pattern or lower(u.email) like :pattern)
            """)
    Page<User> searchByRole(@Param("role") UserRole role, @Param("pattern") String pattern, Pageable pageable);
}
