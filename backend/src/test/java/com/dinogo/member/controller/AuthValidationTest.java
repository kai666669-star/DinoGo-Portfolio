package com.dinogo.member.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.config.SecurityConfig;
import com.dinogo.member.service.LoginService;
import com.dinogo.member.service.GoogleLoginService;
import com.dinogo.member.service.MemberService;
import com.dinogo.member.service.PasswordResetService;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

@WebMvcTest({ RegisterController.class, LoginController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class AuthValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private GoogleLoginService googleLoginService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @ParameterizedTest(name = "register rejects {0}")
    @MethodSource("invalidRegistrationRequests")
    void registerRejectsInvalidRequest(String scenario, String requestBody, String fieldName) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("輸入資料驗證失敗"))
                .andExpect(jsonPath("$.fieldErrors." + fieldName).isNotEmpty());

        verifyNoInteractions(memberService);
    }

    @ParameterizedTest(name = "login rejects {0}")
    @MethodSource("invalidLoginRequests")
    void loginRejectsInvalidRequest(String scenario, String requestBody, String fieldName) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("輸入資料驗證失敗"))
                .andExpect(jsonPath("$.fieldErrors." + fieldName).isNotEmpty());

        verifyNoInteractions(loginService);
    }

    private static Stream<Arguments> invalidRegistrationRequests() {
        String validPassword = "password123";
        return Stream.of(
                Arguments.of("blank email", registerJson("", validPassword, validPassword, "王", "小明"), "email"),
                Arguments.of("malformed email", registerJson("invalid-email", validPassword, validPassword, "王", "小明"), "email"),
                Arguments.of("short password", registerJson("user@example.com", "short", "short", "王", "小明"), "password"),
                Arguments.of("blank last name", registerJson("user@example.com", validPassword, validPassword, "", "小明"), "lastName"),
                Arguments.of("overlong first name", registerJson(
                        "user@example.com", validPassword, validPassword, "王", "名".repeat(51)), "firstName"));
    }

    private static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of("blank email", loginJson("", "password123"), "email"),
                Arguments.of("malformed email", loginJson("invalid-email", "password123"), "email"),
                Arguments.of("blank password", loginJson("user@example.com", ""), "password"));
    }

    @Test
    void registerReturnsStructuredErrorForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("請求內容格式錯誤"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        verifyNoInteractions(memberService);
    }

    @Test
    void googleLoginRejectsOversizedCredential() throws Exception {
        String requestBody = """
                {"credential":"%s"}
                """.formatted("x".repeat(10001));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.credential").isNotEmpty());

        verifyNoInteractions(googleLoginService);
    }

    private static String registerJson(
            String email,
            String password,
            String confirmPassword,
            String lastName,
            String firstName) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "confirmPassword": "%s",
                  "lastName": "%s",
                  "firstName": "%s"
                }
                """.formatted(email, password, confirmPassword, lastName, firstName);
    }

    private static String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
