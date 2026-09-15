package com.yanyu.todo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.yanyu.todo.entity.User;
import com.yanyu.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;

    @BeforeEach
    void setUp() {
        username = "test_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 20);
    }

    @Test
    void registerStoresBCryptHashInsteadOfPlainPassword()
            throws Exception {

        String rawPassword = "123456";

        register(username, rawPassword);

        User user = userRepository
                .findByUsername(username)
                .orElseThrow();

        assertNotEquals(rawPassword, user.getPasswordHash());

        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        user.getPasswordHash()
                )
        );
    }

    @Test
    void duplicateUsernameReturnsConflict()
            throws Exception {

        register(username, "123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "123456"
                        }
                        """.formatted(username)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void registerWithShortPasswordReturnsBadRequest()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "123"
                        }
                        """.formatted(username)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void loginWithCorrectPasswordReturnsJwt()
            throws Exception {

        register(username, "123456");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "123456"
                        }
                        """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.username")
                        .value(username))
                .andReturn();

        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        String token = json
                .path("data")
                .path("token")
                .asText();

        assertFalse(token.isBlank());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized()
            throws Exception {

        register(username, "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "wrong-password"
                        }
                        """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message")
                        .value("用户名或密码错误"));
    }

    @Test
    void loginWithUnknownUsernameReturnsUnauthorized()
            throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "123456"
                        }
                        """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message")
                        .value("用户名或密码错误"));
    }

    @Test
    void todoApiWithoutTokenReturnsUnauthorized()
            throws Exception {

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void todoApiWithValidTokenCanBeAccessed()
            throws Exception {

        register(username, "123456");

        String token = loginAndGetToken(
                username,
                "123456"
        );

        mockMvc.perform(get("/api/todos")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isOk());
    }

    @Test
    void changePasswordRequiresOldPassword()
            throws Exception {

        register(username, "123456");

        String token = loginAndGetToken(
                username,
                "123456"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "oldPassword": "wrong-password",
                            "newPassword": "654321",
                            "confirmPassword": "654321"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void changePasswordRequiresMatchingConfirmation()
            throws Exception {

        register(username, "123456");

        String token = loginAndGetToken(
                username,
                "123456"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "oldPassword": "123456",
                            "newPassword": "654321",
                            "confirmPassword": "111111"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void changePasswordAllowsNewPasswordLogin()
            throws Exception {

        register(username, "123456");

        String token = loginAndGetToken(
                username,
                "123456"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "oldPassword": "123456",
                            "newPassword": "654321",
                            "confirmPassword": "654321"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "654321"
                        }
                        """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists());
    }

    private void register(
            String username,
            String password
    ) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "%s"
                        }
                        """.formatted(username, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String loginAndGetToken(
            String username,
            String password
    ) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "username": "%s",
                            "password": "%s"
                        }
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        String token = json
                .path("data")
                .path("token")
                .asText();

        assertFalse(token.isBlank());

        return token;
    }
}