package otus.krasnyanskysa.jwtapi.repository;

import otus.krasnyanskysa.jwtapi.entity.Task;
import otus.krasnyanskysa.jwtapi.entity.TaskStatus;
import otus.krasnyanskysa.jwtapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);

    List<Task> findByUserUsername(String username);

    @Query(value = "SELECT t FROM Task t JOIN FETCH t.user LEFT JOIN FETCH t.category",
           countQuery = "SELECT COUNT(t) FROM Task t")
    Page<Task> findAllWithUserAndCategory(Pageable pageable);

    @Query(value = "SELECT t FROM Task t JOIN FETCH t.user LEFT JOIN FETCH t.category WHERE t.user.username = :username AND (:status IS NULL OR t.status = :status)",
           countQuery = "SELECT COUNT(t) FROM Task t WHERE t.user.username = :username AND (:status IS NULL OR t.status = :status)")
    Page<Task> findByUsernameAndOptionalStatus(@Param("username") String username,
                                               @Param("status") TaskStatus status,
                                               Pageable pageable);

    List<Task> findByCategoryId(Long categoryId);
}

