package com.tasktracker.api.service;

import com.tasktracker.api.dto.PageResponse;
import com.tasktracker.api.dto.TaskDtos.ChangeStatusRequest;
import com.tasktracker.api.dto.TaskDtos.CreateTaskRequest;
import com.tasktracker.api.dto.TaskDtos.TaskResponse;
import com.tasktracker.api.dto.TaskDtos.UpdateTaskRequest;
import com.tasktracker.api.entity.Priority;
import com.tasktracker.api.entity.Project;
import com.tasktracker.api.entity.Role;
import com.tasktracker.api.entity.Task;
import com.tasktracker.api.entity.TaskStatus;
import com.tasktracker.api.entity.User;
import com.tasktracker.api.exception.ApiException;
import com.tasktracker.api.repository.ProjectRepository;
import com.tasktracker.api.repository.TaskRepository;
import com.tasktracker.api.repository.UserRepository;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private static final Map<TaskStatus, List<TaskStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(TaskStatus.TODO, List.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED));
        ALLOWED_TRANSITIONS.put(TaskStatus.IN_PROGRESS, List.of(TaskStatus.IN_REVIEW, TaskStatus.BLOCKED));
        ALLOWED_TRANSITIONS.put(TaskStatus.IN_REVIEW, List.of(TaskStatus.DONE, TaskStatus.BLOCKED));
        ALLOWED_TRANSITIONS.put(TaskStatus.BLOCKED, List.of(TaskStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(TaskStatus.DONE, List.of());
    }

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final CurrentUser currentUser;

    public TaskService(TaskRepository tasks, ProjectRepository projects, UserRepository users, CurrentUser currentUser) {
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
        this.currentUser = currentUser;
    }

    @Cacheable(value = "tasksByAssignee", condition = "#root.target.effectiveAssigneeForCache(#assigneeId) != null",
            key = "#root.target.cacheOrganizationId() + ':' + #root.target.effectiveAssigneeForCache(#assigneeId) + ':' + #status + ':' + #priority + ':' + #page + ':' + #limit")
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(TaskStatus status, Priority priority, Long assigneeId, int page, int limit) {
        if (page < 0 || limit < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_PAGINATION", "page must be >= 0 and limit must be >= 1");
        }
        User user = currentUser.get();
        Long effectiveAssignee = user.getRole() == Role.MEMBER ? user.getId() : assigneeId;
        var pageable = PageRequest.of(page, Math.min(limit, 100), Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(tasks.search(user.getOrganizationId(), status, priority, effectiveAssignee, pageable).map(this::toResponse));
    }

    public Long cacheOrganizationId() {
        return currentUser.get().getOrganizationId();
    }

    public Long effectiveAssigneeForCache(Long assigneeId) {
        User user = currentUser.get();
        return user.getRole() == Role.MEMBER ? user.getId() : assigneeId;
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return toResponse(getTask(id));
    }

    @Transactional
    @CacheEvict(value = "tasksByAssignee", allEntries = true)
    public TaskResponse create(CreateTaskRequest request) {
        User actor = currentUser.get();
        Project project = projects.findByIdAndOrganization_Id(request.projectId(), actor.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "project not found"));
        User assignee = users.findByIdAndOrganization_Id(request.assigneeId(), actor.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ASSIGNEE_NOT_FOUND", "assignee not found"));
        Task task = new Task();
        task.setOrganization(actor.getOrganization());
        task.setProject(project);
        task.setAssignee(assignee);
        task.setCreatedBy(actor);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setStatus(TaskStatus.TODO);
        return toResponse(tasks.save(task));
    }

    @Transactional
    @CacheEvict(value = "tasksByAssignee", allEntries = true)
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        User actor = currentUser.get();
        Task task = getTask(id);
        User assignee = users.findByIdAndOrganization_Id(request.assigneeId(), actor.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ASSIGNEE_NOT_FOUND", "assignee not found"));
        task.setAssignee(assignee);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return toResponse(task);
    }

    @Transactional
    @CacheEvict(value = "tasksByAssignee", allEntries = true)
    public TaskResponse changeStatus(Long id, ChangeStatusRequest request) {
        Task task = getTask(id);
        if (!ALLOWED_TRANSITIONS.get(task.getStatus()).contains(request.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION",
                    "cannot move task from " + task.getStatus() + " to " + request.status());
        }
        task.setStatus(request.status());
        task.setCompletedAt(request.status() == TaskStatus.DONE ? Instant.now() : null);
        return toResponse(task);
    }

    @Transactional
    @CacheEvict(value = "tasksByAssignee", allEntries = true)
    public void delete(Long id) {
        tasks.delete(getTask(id));
    }

    private Task getTask(Long id) {
        return tasks.findByIdAndOrganization_Id(id, currentUser.get().getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "task not found"));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getAssignee().getId(),
                task.getAssignee().getName(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getDueDate(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
