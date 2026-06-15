package com.acousticguard.hub.auth.repository;

import com.acousticguard.hub.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for User entity persistence.
 * <p>
 * Provides data access operations for user authentication and authorization.
 * This repository extends JpaRepository for standard CRUD operations on User entities.
 * User entities store authentication credentials and role information for API access control.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
}