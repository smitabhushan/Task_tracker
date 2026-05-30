package com.tasktracker.api.service;

import com.tasktracker.api.entity.Role;
import com.tasktracker.api.entity.User;
import com.tasktracker.api.repository.TaskRepository;
import org.springframework.stereotype.Component;

@Component("taskAccess")
public class TaskAccess {
    private final CurrentUser currentUser;
    private final TaskRepository tasks;

    public TaskAccess(CurrentUser currentUser, TaskRepository tasks) {
        this.currentUser = currentUser;
        this.tasks = tasks;
    }

    public boolean canRead(Long taskId) {
        User user = currentUser.get();
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) {
            return tasks.findByIdAndOrganization_Id(taskId, user.getOrganizationId()).isPresent();
        }
        return tasks.findByIdAndOrganization_IdAndAssignee_Id(taskId, user.getOrganizationId(), user.getId()).isPresent();
    }

    public boolean canChangeStatus(Long taskId) {
        User user = currentUser.get();
        if (user.getRole() == Role.MANAGER || user.getRole() == Role.ADMIN) {
            return tasks.findByIdAndOrganization_Id(taskId, user.getOrganizationId()).isPresent();
        }
        return tasks.findByIdAndOrganization_IdAndAssignee_Id(taskId, user.getOrganizationId(), user.getId()).isPresent();
    }
}
