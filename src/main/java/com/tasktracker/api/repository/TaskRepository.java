package com.tasktracker.api.repository;

import com.tasktracker.api.entity.Priority;
import com.tasktracker.api.entity.Task;
import com.tasktracker.api.entity.TaskStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    Optional<Task> findByIdAndOrganization_Id(Long id, Long organizationId);

    Optional<Task> findByIdAndOrganization_IdAndAssignee_Id(Long id, Long organizationId, Long assigneeId);

    void deleteByProject_IdAndOrganization_Id(Long projectId, Long organizationId);

    @Query("""
        select t from Task t
        where t.organization.id = :organizationId
          and (:status is null or t.status = :status)
          and (:priority is null or t.priority = :priority)
          and (:assigneeId is null or t.assignee.id = :assigneeId)
        """)
    Page<Task> search(@Param("organizationId") Long organizationId,
                      @Param("status") TaskStatus status,
                      @Param("priority") Priority priority,
                      @Param("assigneeId") Long assigneeId,
                      Pageable pageable);

    @Query("""
        select t.assignee.id, t.assignee.name, count(t.id)
        from Task t
        where t.organization.id = :organizationId
          and t.status <> com.tasktracker.api.entity.TaskStatus.DONE
          and t.dueDate < :today
        group by t.assignee.id, t.assignee.name
        """)
    java.util.List<Object[]> overdueCountByUser(@Param("organizationId") Long organizationId, @Param("today") LocalDate today);

    @Query(value = """
        select avg(timestampdiff(second, created_at, completed_at))
        from tasks
        where organization_id = :organizationId and completed_at is not null
        """, nativeQuery = true)
    Double averageCompletionSeconds(@Param("organizationId") Long organizationId);
}
