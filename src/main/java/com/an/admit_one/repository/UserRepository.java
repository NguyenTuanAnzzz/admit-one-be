package com.an.admit_one.repository;

import com.an.admit_one.entity.User;
import com.an.admit_one.enums.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // Trang quản lý Người dùng (admin): role để trống thì lấy đủ cả 3 role, pattern luôn có giá trị ("%" khi không tìm)
    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (lower(u.fullName) like :pattern or lower(u.email) like :pattern)
            """)
    Page<User> searchUsers(@Param("role") UserRole role, @Param("pattern") String pattern, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
}
