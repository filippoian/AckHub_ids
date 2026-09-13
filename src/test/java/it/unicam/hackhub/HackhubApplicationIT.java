package it.unicam.hackhub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class HackhubApplicationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerAndLoginReturnsToken() {
        String username = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "Pwd_12345";

        Map<String, Object> registerBody = new HashMap<>();
        registerBody.put("username", username);
        registerBody.put("password", password);

        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                registerBody,
                Map.class
        );

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        assertTrue(((Map<?, ?>) registerResponse.getBody().get("profile")).containsKey("userId"));
        assertNotNull(registerResponse.getBody().get("recoveryCode"));

        Map<String, Object> loginBody = new HashMap<>();
        loginBody.put("type", "USER");
        loginBody.put("identifier", username);
        loginBody.put("password", password);

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                loginBody,
                Map.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        Object tokenValue = loginResponse.getBody().get("token");
        assertNotNull(tokenValue);
        assertFalse(String.valueOf(tokenValue).isBlank());
    }
}
