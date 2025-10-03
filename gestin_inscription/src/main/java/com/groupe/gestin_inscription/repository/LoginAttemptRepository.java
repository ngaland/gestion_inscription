package com.groupe.gestin_inscription.repository;

import com.groupe.gestin_inscription.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

}
