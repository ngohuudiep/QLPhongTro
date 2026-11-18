package com.qlphongtro;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.StaffBuildingAssignment;
import com.qlphongtro.entity.Tenant;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.InvoiceRepository;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.StaffBuildingAssignmentRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "REMEMBER_ME_KEY=test-only-remember-me-key",
        "spring.datasource.url=jdbc:h2:mem:staffBuildingHttp;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@Transactional
class StaffBuildingScopeMockMvcIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired StaffBuildingAssignmentRepository assignmentRepository;
    @PersistenceContext EntityManager entityManager;

    @Test
    @WithMockUser(username = "staff-x", roles = "STAFF")
    void assignedBuildingRowsAreVisibleAndOtherBuildingRowsReturnNotFound() throws Exception {
        Fixture fixture = createTwoBuildingFixture();

        mockMvc.perform(get("/contracts/edit/{id}", fixture.contractYId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/contracts/edit/{id}", fixture.contractXId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/invoices/{id}", fixture.invoiceYId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/invoices/{id}", fixture.invoiceXId()))
                .andExpect(status().isOk());
    }

    private Fixture createTwoBuildingFixture() {
        Role staffRole = roleRepository.save(new Role(null, "STAFF"));
        Role userRole = roleRepository.save(new Role(null, "USER"));
        User staff = new User();
        staff.setUsername("staff-x");
        staff.setPassword("test-hash");
        staff.setFullName("Staff X");
        staff.setEmail("staff-x@example.invalid");
        staff.setPhone("0900000001");
        staff.setRole(staffRole);
        staff.setAccountEnabled(true);
        userRepository.save(staff);
        User tenantXUser = tenantUser("tenant-x", "0900000002", userRole);
        User tenantYUser = tenantUser("tenant-y", "0900000003", userRole);
        userRepository.save(tenantXUser);
        userRepository.save(tenantYUser);

        Building buildingX = buildingRepository.save(building("X"));
        Building buildingY = buildingRepository.save(building("Y"));
        Room roomX = roomRepository.save(room("X-101", buildingX));
        Room roomY = roomRepository.save(room("Y-101", buildingY));
        Tenant tenantX = tenantRepository.save(tenant("000000000001", "tenant-x@example.invalid", tenantXUser));
        Tenant tenantY = tenantRepository.save(tenant("000000000002", "tenant-y@example.invalid", tenantYUser));
        Contract contractX = contractRepository.save(contract("CON-X-101", roomX, tenantX));
        Contract contractY = contractRepository.save(contract("CON-Y-101", roomY, tenantY));
        Invoice invoiceX = invoiceRepository.save(invoice("INV-X-202611", contractX));
        Invoice invoiceY = invoiceRepository.save(invoice("INV-Y-202611", contractY));

        StaffBuildingAssignment assignment = new StaffBuildingAssignment();
        assignment.setStaff(staff);
        assignment.setBuilding(buildingX);
        assignmentRepository.save(assignment);
        entityManager.flush();
        entityManager.clear();

        return new Fixture(contractX.getContractId(), contractY.getContractId(),
                invoiceX.getInvoiceId(), invoiceY.getInvoiceId());
    }

    private Building building(String code) {
        Building building = new Building();
        building.setBuildingCode("B-" + code);
        building.setBuildingName("Building " + code);
        building.setAddress(code + " address");
        return building;
    }

    private Room room(String code, Building building) {
        Room room = new Room();
        room.setRoomCode(code);
        room.setBuilding(building);
        room.setFloorNo((short) 1);
        room.setMaxOccupants((short) 2);
        room.setPriceOverride(new BigDecimal("3000000"));
        room.setStatus("Đang thuê");
        return room;
    }

    private User tenantUser(String username, String phone, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test-hash");
        user.setFullName("Tenant " + username);
        user.setEmail(username + "@example.invalid");
        user.setPhone(phone);
        user.setRole(role);
        user.setAccountEnabled(true);
        return user;
    }

    private Tenant tenant(String idNumber, String email, User user) {
        Tenant tenant = new Tenant();
        tenant.setFullName("Tenant " + idNumber);
        tenant.setIdNumber(idNumber);
        tenant.setPhone("0912345678");
        tenant.setEmail(email);
        tenant.setUser(user);
        return tenant;
    }

    private Contract contract(String code, Room room, Tenant tenant) {
        Contract contract = new Contract();
        contract.setContractCode(code);
        contract.setRoom(room);
        contract.setRepresentative(tenant);
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2027, 1, 1));
        contract.setStatus("Hiệu lực");
        return contract;
    }

    private Invoice invoice(String code, Contract contract) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(code);
        invoice.setContract(contract);
        invoice.setPeriodStart(LocalDate.of(2026, 11, 1));
        invoice.setPeriodEnd(LocalDate.of(2026, 11, 30));
        invoice.setDueDate(LocalDate.of(2026, 12, 5));
        invoice.setTotalAmount(new BigDecimal("3000000"));
        invoice.setStatus("Chưa thanh toán");
        return invoice;
    }

    private record Fixture(Integer contractXId, Integer contractYId, Integer invoiceXId, Integer invoiceYId) { }
}
