package com.qlphongtro;

import com.qlphongtro.dto.LandlordTenantView;
import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.User;
import com.qlphongtro.controller.LandlordWorkspaceController;
import com.qlphongtro.service.InvoiceService;
import com.qlphongtro.service.LandlordWorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LandlordWorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class LandlordWorkspaceControllerViewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LandlordWorkspaceService landlordWorkspaceService;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    void landlordInvoiceListRendersContractWithDirectTenantAccount() throws Exception {
        User tenant = new User();
        tenant.setUsername("nguoithue");
        tenant.setFullName("Người thuê Demo");

        Building building = new Building();
        building.setBuildingName("Khu trọ A");
        Room room = new Room();
        room.setRoomCode("P101");
        room.setBuilding(building);

        Contract contract = new Contract();
        contract.setContractId(1);
        contract.setContractCode("HD-101");
        contract.setRoom(room);
        contract.setTenantUser(tenant);
        contract.setStartDate(LocalDate.now().minusDays(1));
        contract.setEndDate(LocalDate.now().plusDays(10));

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10);
        invoice.setInvoiceCode("INV-P101-202611");
        invoice.setContract(contract);
        invoice.setPeriodStart(LocalDate.of(2026, 11, 1));
        invoice.setPeriodEnd(LocalDate.of(2026, 11, 30));
        invoice.setDueDate(LocalDate.of(2026, 11, 30));
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(new BigDecimal("1250000"));
        invoice.setStatus("UNPAID");

        when(landlordWorkspaceService.getInvoices("chutro")).thenReturn(List.of(invoice));
        when(landlordWorkspaceService.getContracts("chutro")).thenReturn(List.of(contract));

        mockMvc.perform(get("/landlord/invoices")
                        .principal(landlordPrincipal())
                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Người thuê Demo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("P101")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tổng công nợ")));
    }

    @Test
    void landlordTenantListRendersDirectTenantAccountAndContractLink() throws Exception {
        when(landlordWorkspaceService.getTenantOccupancies("chutro")).thenReturn(List.of(
                new LandlordTenantView(3, "nguoithue", "Người thuê Demo", "0909000003", null,
                        null, "P101", "HD-101", LocalDate.of(2026, 11, 18),
                        LocalDate.of(2026, 11, 30), "TENANT_ACCEPTED",
                        "Người thuê đã đồng ý", 1)));

        mockMvc.perform(get("/landlord/tenants").principal(landlordPrincipal()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Người thuê Demo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("P101")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/landlord/contracts/1")));
    }

    private static UsernamePasswordAuthenticationToken landlordPrincipal() {
        return UsernamePasswordAuthenticationToken.authenticated("chutro", "",
                List.of(new SimpleGrantedAuthority("ROLE_LANDLORD")));
    }
}
