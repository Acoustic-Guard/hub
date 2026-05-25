package com.acousticguard.hub.auth.repository;

import com.acousticguard.hub.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
