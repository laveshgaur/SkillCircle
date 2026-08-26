package com.skillcircle.project.repository;

import com.skillcircle.project.entity.Task;
import com.skillcircle.project.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<Task> findByProjectIdAndStatusOrderByPriorityDesc(UUID projectId, TaskStatus status);

    List<Task> findByAssigneeIdOrderByDueDateAsc(UUID assigneeId);

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);
}
