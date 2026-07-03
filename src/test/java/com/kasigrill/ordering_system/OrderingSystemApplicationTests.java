package com.kasigrill.ordering_system;

import com.kasigrill.ordering_system.whatsapp.WhatsAppWebhookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
class OrderingSystemApplicationTests {

    private MockMvc mockMvc;

    @Autowired
    private WhatsAppWebhookController whatsAppWebhookController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(whatsAppWebhookController).build();

    }

    @Test
    void contextLoads() throws Exception {

        String fakeWhatsappPayload = """
                {
                "object":" whatsapp_business_account",
                "entry":[]
                }
                """;

        mockMvc.perform(post("/api/v1/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(fakeWhatsappPayload))
                .andExpect(status().isOk());
    }

}
