package com.upc.pre.peaceapp.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import com.upc.pre.peaceapp.repositories.UserRepository;
import com.upc.pre.peaceapp.security.iam.application.internal.outboundservices.notifications.PasswordResetNotifier;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.UserIAMRepository;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIAMIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserIAMRepository userIAMRepository;
    @Autowired private UserRepository profileRepository;
    @Autowired private TestAuthHelper authHelper;
    @SpyBean private PasswordResetNotifier notifier;

    private ResultActions signUp(String email, String password) throws Exception {
        return post("/api/v1/authentication/sign-up", new SignUpResource(email, password, "Ana", "Pérez", "999888777"));
    }

    private ResultActions post(String url, Object body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    void whenSignUp_thenIdentityAndProfileAreCreatedAndLinked() throws Exception {
        signUp("Ana@Example.com", "secure123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ana@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        var user = userIAMRepository.findByUsername("ana@example.com").orElseThrow();
        var profile = profileRepository.findByEmail("ana@example.com").orElseThrow();
        assertEquals(user.getId(), profile.getUser_id());
        assertEquals("Ana", profile.getName());
    }

    @Test
    void whenSignUpWithDuplicatedEmail_thenConflict() throws Exception {
        signUp("dup@example.com", "secure123").andExpect(status().isCreated());
        signUp("DUP@example.com", "secure123").andExpect(status().isConflict());
        assertEquals(1, profileRepository.findAll().stream().filter(p -> p.getEmail().equals("dup@example.com")).count());
    }

    @Test
    void whenSignUpWithInvalidFields_thenBadRequestWithFieldErrors() throws Exception {
        post("/api/v1/authentication/sign-up", new SignUpResource("no-es-correo", "123", "", null, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.lastname").exists())
                .andExpect(jsonPath("$.errors.phonenumber").exists());
    }

    @Test
    void whenSignUpRequestsAdminRole_thenRoleIsIgnored() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sneaky@example.com\",\"password\":\"secure123\",\"name\":\"S\",\"lastname\":\"N\",\"roles\":[\"ROLE_ADMIN\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void whenSignInWithValidCredentials_thenReturnsToken() throws Exception {
        signUp("login@example.com", "secure123").andExpect(status().isCreated());
        post("/api/v1/authentication/sign-in", new SignInResource("login@example.com", "secure123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void whenSignInWithWrongPasswordOrUnknownUser_thenSameUnauthorizedMessage() throws Exception {
        signUp("wrong@example.com", "secure123").andExpect(status().isCreated());
        post("/api/v1/authentication/sign-in", new SignInResource("wrong@example.com", "wrongpassword"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
        post("/api/v1/authentication/sign-in", new SignInResource("nadie@example.com", "secure123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
    }

    @Test
    void whenSignInWithBackdoorPassword_thenUnauthorized() throws Exception {
        signUp("backdoor@example.com", "secure123").andExpect(status().isCreated());
        post("/api/v1/authentication/sign-in", new SignInResource("backdoor@example.com", "owo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenChangePasswordWithoutToken_thenUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/authentication/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordResource("a", "newpass456"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenChangePasswordWithCurrentPassword_thenNewOneWorks() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("change@example.com", "oldpass123");

        var request = put("/api/v1/authentication/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request.content(objectMapper.writeValueAsString(new ChangePasswordResource("incorrecta1", "newpass456"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/authentication/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordResource("oldpass123", "newpass456"))))
                .andExpect(status().isNoContent());

        post("/api/v1/authentication/sign-in", new SignInResource("change@example.com", "newpass456")).andExpect(status().isOk());
        post("/api/v1/authentication/sign-in", new SignInResource("change@example.com", "oldpass123")).andExpect(status().isUnauthorized());
    }

    @Test
    void whenForgotAndResetPassword_thenTokenWorksOnlyOnce() throws Exception {
        signUp("forgot@example.com", "oldpass123").andExpect(status().isCreated());

        post("/api/v1/authentication/forgot-password", new ForgotPasswordResource("forgot@example.com"))
                .andExpect(status().isAccepted());
        var captor = ArgumentCaptor.forClass(String.class);
        verify(notifier).sendResetToken(eq("forgot@example.com"), captor.capture());
        String token = captor.getValue();

        post("/api/v1/authentication/reset-password", new ResetPasswordResource(token, "brandnew789"))
                .andExpect(status().isNoContent());
        post("/api/v1/authentication/sign-in", new SignInResource("forgot@example.com", "brandnew789")).andExpect(status().isOk());

        post("/api/v1/authentication/reset-password", new ResetPasswordResource(token, "another789"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenForgotPasswordForUnknownEmail_thenAcceptedWithoutSendingAnything() throws Exception {
        post("/api/v1/authentication/forgot-password", new ForgotPasswordResource("ghost@example.com"))
                .andExpect(status().isAccepted());
        verify(notifier, never()).sendResetToken(eq("ghost@example.com"), anyString());
    }

    @Test
    void whenResetPasswordWithUnknownToken_thenBadRequest() throws Exception {
        post("/api/v1/authentication/reset-password", new ResetPasswordResource("token-falso", "brandnew789"))
                .andExpect(status().isBadRequest());
    }
}
