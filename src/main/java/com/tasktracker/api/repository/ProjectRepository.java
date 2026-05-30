package com.tasktracker.api.repository;

import com.tasktracker.api.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByIdAndOrganization_Id(Long id, Long organizationId);

    List<Project> findByOrganization_Id(Long organizationId);
}
