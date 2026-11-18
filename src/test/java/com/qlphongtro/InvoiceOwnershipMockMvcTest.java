package com.qlphongtro;

import com.qlphongtro.controller.InvoiceController;
import com.qlphongtro.security.CustomUserDetailsService;
import com.qlphongtro.security.SecurityConfig;
import com.qlphongtro.service.ContractService;
import com.qlphongtro.service.InvoiceService;
import com.qlphongtro.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class InvoiceOwnershipMockMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean InvoiceService invoiceService;
    @MockBean ContractService contractService;
    @MockBean PaymentService paymentService;
    @MockBean CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "tenant-a", roles = "USER")
    void tenantAReceivesNotFoundForTenantBInvoiceId() throws Exception {
        when(invoiceService.getInvoiceById(903)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/invoices/903")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "tenant-a", roles = "USER")
    void paymentSubmissionAgainstTenantBInvoiceReceivesNotFound() throws Exception {
        when(invoiceService.getInvoiceById(904)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mockMvc.perform(post("/invoices/904/pay").with(csrf())
                        .param("amount", "100")
                        .param("method", "transfer"))
                .andExpect(status().isNotFound());
    }
}
