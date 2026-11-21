package com.qlphongtro;

import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rbacscope;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
class ScopedRepositoryDataJpaTest {
    @Autowired TestEntityManager entityManager;
    @Autowired ContractRepository contractRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired MeterReadingRepository meterReadingRepository;
    @Autowired IncidentRepository incidentRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired StaffBuildingAssignmentRepository assignmentRepository;

    @Test
    void tenantAndAssignedBuildingQueriesReturnOnlyScopedRows() {
        Role userRole = new Role(null, "USER");
        Role staffRole = new Role(null, "STAFF");
        entityManager.persist(userRole);
        entityManager.persist(staffRole);
        User tenantAUser = user("tenant-a", userRole);
        User tenantBUser = user("tenant-b", userRole);
        User staffX = user("staff-x", staffRole);
        entityManager.persist(tenantAUser);
        entityManager.persist(tenantBUser);
        entityManager.persist(staffX);

        Building buildingX = building("X");
        Building buildingY = building("Y");
        entityManager.persist(buildingX);
        entityManager.persist(buildingY);
        Room roomX = room("X-101", buildingX);
        Room roomY = room("Y-101", buildingY);
        entityManager.persist(roomX);
        entityManager.persist(roomY);
        Tenant tenantA = tenant("Tenant A", "001234567891", tenantAUser);
        Tenant tenantB = tenant("Tenant B", "001234567892", tenantBUser);
        entityManager.persist(tenantA);
        entityManager.persist(tenantB);
        Contract contractX = contract("CON-X", roomX, tenantA);
        Contract contractY = contract("CON-Y", roomY, tenantB);
        entityManager.persist(contractX);
        entityManager.persist(contractY);
        Invoice invoiceX = invoice("INV-X", contractX);
        Invoice invoiceY = invoice("INV-Y", contractY);
        entityManager.persist(invoiceX);
        entityManager.persist(invoiceY);
        Payment paymentX = payment(invoiceX);
        Payment paymentY = payment(invoiceY);
        entityManager.persist(paymentX);
        entityManager.persist(paymentY);
        MeterReading readingX = reading(roomX);
        MeterReading readingY = reading(roomY);
        entityManager.persist(readingX);
        entityManager.persist(readingY);
        Incident incidentX = incident(roomX, tenantA);
        Incident incidentY = incident(roomY, tenantB);
        entityManager.persist(incidentX);
        entityManager.persist(incidentY);
        StaffBuildingAssignment assignment = new StaffBuildingAssignment();
        assignment.setStaff(staffX);
        assignment.setBuilding(buildingX);
        entityManager.persist(assignment);
        entityManager.flush();
        entityManager.clear();

        Integer contractXId = contractX.getContractId();
        Integer contractYId = contractY.getContractId();
        Integer invoiceXId = invoiceX.getInvoiceId();
        Integer invoiceYId = invoiceY.getInvoiceId();
        Integer paymentXId = paymentX.getPaymentId();
        Integer paymentYId = paymentY.getPaymentId();
        Integer readingXId = readingX.getReadingId();
        Integer readingYId = readingY.getReadingId();
        Integer incidentXId = incidentX.getIncidentId();
        Integer incidentYId = incidentY.getIncidentId();

        assertThat(contractRepository.findAllForTenant("tenant-a")).extracting(Contract::getContractId).containsExactly(contractXId);
        assertThat(contractRepository.findByIdAndTenantUsername(contractXId, "tenant-a")).isPresent();
        assertThat(contractRepository.findByIdAndTenantUsername(contractYId, "tenant-a")).isEmpty();
        assertThat(invoiceRepository.findAllForTenant("tenant-a")).extracting(Invoice::getInvoiceId).containsExactly(invoiceXId);
        assertThat(invoiceRepository.findByIdAndTenantUsername(invoiceYId, "tenant-a")).isEmpty();
        assertThat(paymentRepository.findAllForTenant("tenant-a")).extracting(Payment::getPaymentId).containsExactly(paymentXId);
        assertThat(paymentRepository.findByIdAndTenantUsername(paymentYId, "tenant-a")).isEmpty();
        assertThat(meterReadingRepository.findAllForTenant("tenant-a")).extracting(MeterReading::getReadingId).containsExactly(readingXId);
        assertThat(meterReadingRepository.findByIdAndTenantUsername(readingYId, "tenant-a")).isEmpty();
        assertThat(incidentRepository.findAllForTenant("tenant-a")).extracting(Incident::getIncidentId).containsExactly(incidentXId);
        assertThat(incidentRepository.findByIdAndTenantUsername(incidentYId, "tenant-a")).isEmpty();

        assertThat(contractRepository.findByContractIdAndRoom_Building_BuildingIdIn(contractYId, List.of(buildingX.getBuildingId()))).isEmpty();
        assertThat(invoiceRepository.findByInvoiceIdAndContract_Room_Building_BuildingIdIn(invoiceYId, List.of(buildingX.getBuildingId()))).isEmpty();
        assertThat(paymentRepository.findByInvoice_InvoiceIdAndInvoice_Contract_Room_Building_BuildingIdIn(paymentY.getInvoice().getInvoiceId(), List.of(buildingX.getBuildingId()))).isEmpty();
        assertThat(meterReadingRepository.findByReadingIdAndRoom_Building_BuildingIdIn(readingYId, List.of(buildingX.getBuildingId()))).isEmpty();
        assertThat(incidentRepository.findByIncidentIdAndRoom_Building_BuildingIdIn(incidentYId, List.of(buildingX.getBuildingId()))).isEmpty();
        assertThat(roomRepository.findByRoomIdAndBuilding_BuildingIdIn(roomY.getRoomId(), List.of(buildingX.getBuildingId()))).isEmpty();
        assertThat(assignmentRepository.existsByStaff_UsernameAndBuilding_BuildingId("staff-x", buildingX.getBuildingId())).isTrue();
        assertThat(assignmentRepository.existsByStaff_UsernameAndBuilding_BuildingId("staff-x", buildingY.getBuildingId())).isFalse();
        assertThat(contractRepository.findByContractIdAndRoom_Building_BuildingIdIn(contractXId, List.of(buildingX.getBuildingId()))).isPresent();
        assertThat(invoiceRepository.findByInvoiceIdAndContract_Room_Building_BuildingIdIn(invoiceXId, List.of(buildingX.getBuildingId()))).isPresent();
        assertThat(paymentRepository.findByInvoice_InvoiceIdAndInvoice_Contract_Room_Building_BuildingIdIn(paymentX.getInvoice().getInvoiceId(), List.of(buildingX.getBuildingId()))).hasSize(1);
        assertThat(meterReadingRepository.findByReadingIdAndRoom_Building_BuildingIdIn(readingXId, List.of(buildingX.getBuildingId()))).isPresent();
        assertThat(incidentRepository.findByIncidentIdAndRoom_Building_BuildingIdIn(incidentXId, List.of(buildingX.getBuildingId()))).isPresent();
    }

    private User user(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test-hash");
        user.setEmail(username + "@example.invalid");
        user.setPhone("09" + String.format("%08d", Math.abs(username.hashCode() % 100_000_000)));
        user.setRole(role);
        return user;
    }

    private Building building(String code) {
        Building building = new Building();
        building.setBuildingCode(code);
        building.setBuildingName("Building " + code);
        building.setAddress(code + " address");
        return building;
    }

    private Room room(String code, Building building) {
        Room room = new Room();
        room.setRoomCode(code);
        room.setBuilding(building);
        room.setStatus("AVAILABLE");
        return room;
    }

    private Tenant tenant(String name, String idNumber, User user) {
        Tenant tenant = new Tenant();
        tenant.setFullName(name);
        tenant.setIdNumber(idNumber);
        tenant.setPhone("0912345678");
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
        contract.setStatus("Active");
        return contract;
    }

    private Invoice invoice(String code, Contract contract) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(code);
        invoice.setContract(contract);
        invoice.setPeriodStart(LocalDate.of(2026, 1, 1));
        invoice.setPeriodEnd(LocalDate.of(2026, 1, 31));
        invoice.setDueDate(LocalDate.of(2026, 2, 5));
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setStatus("UNPAID");
        return invoice;
    }

    private Payment payment(Invoice invoice) {
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(new BigDecimal("100"));
        payment.setPaidAt(LocalDateTime.of(2026, 2, 1, 12, 0));
        payment.setMethod("transfer");
        return payment;
    }

    private MeterReading reading(Room room) {
        MeterReading reading = new MeterReading();
        reading.setRoom(room);
        reading.setBillingMonth(LocalDate.of(2026, 1, 1));
        reading.setStartIndex(new BigDecimal("10"));
        reading.setEndIndex(new BigDecimal("15"));
        reading.setConsumption(new BigDecimal("5"));
        reading.setReadDate(LocalDate.of(2026, 2, 1));
        return reading;
    }

    private Incident incident(Room room, Tenant tenant) {
        Incident incident = new Incident();
        incident.setRoom(room);
        incident.setReportedBy(tenant);
        incident.setTitle("Repair request");
        incident.setPriority("Low");
        incident.setStatus("Received");
        return incident;
    }
}
