package com.khangmoihocit.VocabFlow.modules.user.repositories;

import com.khangmoihocit.VocabFlow.modules.user.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
