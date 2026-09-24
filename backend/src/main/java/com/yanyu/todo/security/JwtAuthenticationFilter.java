package com.yanyu.todo.security;

import com.yanyu.todo.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


//@Component：交给 Spring 管理，可以自动注入。
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;



    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        //获取请求中的 Token
        String authorization = request.getHeader("Authorization");

        if (authorization != null
                && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = authorization.substring("Bearer ".length()).trim();

            try {
                String username = jwtService.username(token);

                userRepository.findByUsername(username).ifPresent(user -> {
                    if (Boolean.TRUE.equals(user.getEnabled())) {
                        // 构建权限列表
                        var authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                        );

                        var authentication = new UsernamePasswordAuthenticationToken(
                                username, null, authorities);

                        // 将认证信息放入上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });

            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }

        }

        filterChain.doFilter(request, response);
    }
}
