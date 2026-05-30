package com.tasktracker.api.repository;

import com.tasktracker.api.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "organization")
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByIdAndOrganization_Id(Long id, Long organizationId);

    List<User> findByOrganization_IdAndActiveTrue(Long organizationId);
}
