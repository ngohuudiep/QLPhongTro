package com.qlphongtro;

import com.qlphongtro.controller.InvoiceController;
import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Payment;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.AccessDeniedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "REMEMBER_ME_KEY=test-only-remember-me-key")
class PaymentConfirmationAuthorizationMockMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean InvoiceService invoiceService;
    @MockBean ContractService contractService;
    @MockBean PaymentService paymentService;
    @MockBean CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void tenantCannotConfirmPayment() throws Exception {
        when(invoiceService.getInvoiceById(80)).thenReturn(new Invoice());
        when(paymentService.processPayment(any(Payment.class))).thenThrow(new AccessDeniedException("forbidden"));
        mockMvc.perform(post("/invoices/80/pay").with(csrf())
                        .param("amount", "100")
                        .param("method", "transfer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanConfirmPayment() throws Exception {
        when(invoiceService.getInvoiceById(80)).thenReturn(new Invoice());
        when(paymentService.processPayment(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/invoices/80/pay").with(csrf())
                        .param("amount", "100")
                        .param("method", "transfer"))
                .andExpect(status().is3xxRedirection());

        verify(paymentService).processPayment(any(Payment.class));
    }
}
