package com.tasktracker.api.service;

import com.tasktracker.api.dto.ProjectDtos.ProjectRequest;
import com.tasktracker.api.dto.ProjectDtos.ProjectResponse;
import com.tasktracker.api.entity.Project;
import com.tasktracker.api.exception.ApiException;
import com.tasktracker.api.repository.ProjectRepository;
import com.tasktracker.api.repository.TaskRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final CurrentUser currentUser;

    public ProjectService(ProjectRepository projects, TaskRepository tasks, CurrentUser currentUser) {
        this.projects = projects;
        this.tasks = tasks;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projects.findByOrganization_Id(currentUser.get().getOrganizationId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        project.setOrganization(currentUser.get().getOrganization());
        project.setName(request.name());
        project.setDescription(request.description());
        projects.save(project);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getProject(id);
        project.setName(request.name());
        project.setDescription(request.description());
        return toResponse(project);
    }

    @Transactional
    @CacheEvict(value = "tasksByAssignee", allEntries = true)
    public void delete(Long id) {
        Project project = getProject(id);
        tasks.deleteByProject_IdAndOrganization_Id(project.getId(), currentUser.get().getOrganizationId());
        projects.delete(project);
    }

    private Project getProject(Long id) {
        return projects.findByIdAndOrganization_Id(id, currentUser.get().getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "project not found"));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription());
    }
}
