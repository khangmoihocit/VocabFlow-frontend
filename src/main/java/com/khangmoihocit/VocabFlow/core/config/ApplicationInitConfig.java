package com.khangmoihocit.VocabFlow.core.config;

import com.khangmoihocit.VocabFlow.core.enums.Role;
import com.khangmoihocit.VocabFlow.modules.auth.entities.User;
import com.khangmoihocit.VocabFlow.modules.auth.repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.swing.text.html.Option;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class ApplicationInitConfig {
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args ->{
            Optional<User> userAdmin = userRepository.findByEmail("admin@gmail.com");
            if(userAdmin.isPresent()){ //không có bản ghi
                User user = userAdmin.get();
                user = User.builder()
                        .email("admin@gmail.com")
                        .passwordHash(passwordEncoder.encode("admin12345"))
                        .fullName("admin")
                        .role(Role.ADMIN.toString())
                        .build();
                userRepository.save(userAdmin.get());
            }
        };
    }
}
