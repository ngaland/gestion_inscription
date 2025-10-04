package com.groupe.gestin_inscription.repository;

import com.groupe.gestin_inscription.model.Administrator;
import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import com.groupe.gestin_inscription.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {


    List findByStatus(ApplicationStatus status);

    /**
     * Finds all applications assigned to a specific administrator.
     * @param assignedAdminId The ID of the administrator.
     * @return A list of applications assigned to the administrator.
     */
    // Assuming a Many-to-One relationship from Application to Administrator
    //@Query("SELECT a FROM Application a WHERE a.assignedAdmin.id = :assignedAdminId")
    //List<Application> findByAssignedAdminId(@Param("assignedAdminId") Long assignedAdminId);
    List<Application> findByAssignedAdmin_Id(Long assignedAdminId);

    @Query("SELECT a FROM Application a WHERE a.status = 'BLOCKED' AND a.lastUpdated < :blockedDateTime")
    List<Application> findBlockedApplications(@Param("blockedDateTime") LocalDateTime blockedDateTime);
    //List<Application> findBlockedApplications(LocalDateTime cutoff);

    // Corrected method using a custom JPQL query to count applications by their status
    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countApplicationsByStatus();
    //Map<String, Integer> countApplicationsByStage();

    List<Application> findByCompletionRateGreaterThanEqual(double rate);

    List<Application> findByApplicantName(User user);

    Long countByStatus(ApplicationStatus applicationStatus);

    /**
     * Finding the Administrator with the minimum number of currently assigned applications
     * (MANUAL_REVIEW or PENDING_RECOURSE status), implementing Load Balancing.
     */
    @Query(value = "SELECT a.* FROM administrator a " +
            "LEFT JOIN application app ON app.assigned_admin_id = a.id " +
            "  AND app.status IN ('MANUAL_REVIEW', 'PENDING_RECOURSE') " +
            "WHERE a.role = 'AGENT' " +
            "GROUP BY a.id " +
            "ORDER BY COUNT(app.id) ASC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<Administrator> findLeastBusyAgent();

    @Query("SELECT a FROM Application a WHERE a.status = :status AND a.submissionDate < :cutoffDate")
    List<Application> findIncompleteApplicationsOlderThan(
            @Param("status") ApplicationStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate);


    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.documents WHERE a.id = :id")
    Optional<Application> findByIdWithDocuments(@Param("id") Long id);

}
