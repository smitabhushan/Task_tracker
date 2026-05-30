package com.tasktracker.api.repository;

import com.tasktracker.api.entity.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByName(String name);
}
