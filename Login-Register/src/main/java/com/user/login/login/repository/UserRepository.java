package com.user.login.login.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.user.login.login.model.Users;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUserName(String userName);
}
