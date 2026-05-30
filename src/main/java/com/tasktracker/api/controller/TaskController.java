package com.tasktracker.api.controller;

import com.tasktracker.api.dto.PageResponse;
import com.tasktracker.api.dto.TaskDtos.ChangeStatusRequest;
import com.tasktracker.api.dto.TaskDtos.CreateTaskRequest;
import com.tasktracker.api.dto.TaskDtos.TaskResponse;
import com.tasktracker.api.dto.TaskDtos.UpdateTaskRequest;
import com.tasktracker.api.entity.Priority;
import com.tasktracker.api.entity.TaskStatus;
import com.tasktracker.api.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','MEMBER')")
    PageResponse<TaskResponse> list(@RequestParam(required = false) TaskStatus status,
                                    @RequestParam(required = false) Priority priority,
                                    @RequestParam(required = false) Long assignee,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int limit) {
        return taskService.list(status, priority, assignee, page, limit);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@taskAccess.canRead(#id)")
    TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@taskAccess.canChangeStatus(#id)")
    TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return taskService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
