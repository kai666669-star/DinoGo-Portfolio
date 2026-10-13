package com.dinogo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "jwt.secret=test-secret-for-jwt-context-only-32-bytes")
@AutoConfigureMockMvc
class DinogoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void protectedCartApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/cart/1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedOrderApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/orders/member"))
				.andExpect(status().isUnauthorized());
	}

}
