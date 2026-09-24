package com.yanyu.todo.config;

import com.yanyu.todo.entity.User;
import com.yanyu.todo.entity.UserRole;
import com.yanyu.todo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultAdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminInitializer(UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args){
        User admin = userRepository.findByUsername("admin")
                .orElseGet(() -> new User("admin",passwordEncoder.encode("admin123")));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
    }
}
