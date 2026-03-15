package com.khangmoihocit.VocabFlow.modules.user.repositories;

import com.khangmoihocit.VocabFlow.modules.user.entities.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<@NonNull User, @NonNull UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
}
