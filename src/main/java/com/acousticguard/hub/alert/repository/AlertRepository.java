package com.acousticguard.hub.alert.repository;

import com.acousticguard.hub.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
}
