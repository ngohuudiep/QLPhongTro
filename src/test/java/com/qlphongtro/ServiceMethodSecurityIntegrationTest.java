package com.qlphongtro;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.RoomService;
import com.qlphongtro.entity.Service;
import com.qlphongtro.entity.StaffBuildingAssignment;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.RoomServiceRepository;
import com.qlphongtro.repository.ServiceRepository;
import com.qlphongtro.repository.StaffBuildingAssignmentRepository;
import com.qlphongtro.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
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
class ServiceMethodSecurityIntegrationTest {
    @Autowired com.qlphongtro.service.InvoiceService invoiceService;
    @Autowired com.qlphongtro.service.RoomService roomManager;
    @Autowired com.qlphongtro.service.RoomServiceService roomServiceManager;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired RoomServiceRepository roomServiceRepository;
    @Autowired StaffBuildingAssignmentRepository assignmentRepository;
    @PersistenceContext EntityManager entityManager;

    @Test
    @WithMockUser(username = "staff-f3", roles = "STAFF")
    void staffCannotDeleteInvoicesOrMutateRoomServicePrices() {
        createFixture();

        assertThatThrownBy(() -> invoiceService.deleteInvoice(900))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> roomServiceManager.assignServiceToRoom(new RoomService()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "staff-f3", roles = "STAFF")
    void staffRoomSaveCannotChangePriceOverride() {
        Fixture fixture = createFixture();
        Room submitted = new Room();
        submitted.setRoomId(fixture.roomId());
        submitted.setStatus("Bảo trì");
        submitted.setPriceOverride(new BigDecimal("1"));

        roomManager.saveRoom(submitted);

        assertThat(roomRepository.findById(fixture.roomId()).orElseThrow().getPriceOverride())
                .isEqualByComparingTo("3000000");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanMutateRoomServicePriceAndRoomPriceOverride() {
        Fixture fixture = createFixture();
        RoomService assignedPrice = new RoomService();
        assignedPrice.setService(serviceRepository.findById(fixture.serviceId()).orElseThrow());
        assignedPrice.setRoom(roomRepository.findById(fixture.roomId()).orElseThrow());
        assignedPrice.setUnitPrice(new BigDecimal("45000"));
        assignedPrice.setEffectiveFrom(LocalDate.of(2026, 11, 1));

        RoomService savedPrice = roomServiceManager.assignServiceToRoom(assignedPrice);
        Room submitted = new Room();
        submitted.setRoomId(fixture.roomId());
        submitted.setRoomCode("X-101");
        submitted.setBuilding(buildingRepository.findById(fixture.buildingId()).orElseThrow());
        submitted.setFloorNo((short) 1);
        submitted.setMaxOccupants((short) 2);
        submitted.setStatus("Đang thuê");
        submitted.setPriceOverride(new BigDecimal("4000000"));

        roomManager.saveRoom(submitted);

        assertThat(savedPrice.getPriceId()).isNotNull();
        assertThat(roomServiceRepository.findById(savedPrice.getPriceId()).orElseThrow().getUnitPrice())
                .isEqualByComparingTo("45000");
        assertThat(roomRepository.findById(fixture.roomId()).orElseThrow().getPriceOverride())
                .isEqualByComparingTo("4000000");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminStillCannotHardDeleteInvoice() {
        assertThatThrownBy(() -> invoiceService.deleteInvoice(900))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Fixture createFixture() {
        Role staffRole = roleRepository.save(new Role(null, "STAFF"));
        Role adminRole = roleRepository.save(new Role(null, "ADMIN"));
        User staff = userRepository.save(user("staff-f3", "0900000011", staffRole));
        userRepository.save(user("admin-f3", "0900000012", adminRole));
        Building building = new Building();
        building.setBuildingCode("F3-X");
        building.setBuildingName("F3 Building X");
        building.setAddress("F3 address");
        building = buildingRepository.save(building);

        Room room = new Room();
        room.setRoomCode("X-101");
        room.setBuilding(building);
        room.setFloorNo((short) 1);
        room.setMaxOccupants((short) 2);
        room.setStatus("Đang thuê");
        room.setPriceOverride(new BigDecimal("3000000"));
        room = roomRepository.save(room);

        Service service = new Service();
        service.setServiceCode("F3-WIFI");
        service.setServiceName("WiFi");
        service.setUnit("tháng");
        service.setCalcType("FIXED");
        service = serviceRepository.save(service);

        StaffBuildingAssignment assignment = new StaffBuildingAssignment();
        assignment.setStaff(staff);
        assignment.setBuilding(building);
        assignmentRepository.save(assignment);
        entityManager.flush();
        entityManager.clear();
        return new Fixture(building.getBuildingId(), room.getRoomId(), service.getServiceId());
    }

    private User user(String username, String phone, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test-hash");
        user.setFullName(username);
        user.setEmail(username + "@example.invalid");
        user.setPhone(phone);
        user.setRole(role);
        user.setAccountEnabled(true);
        return user;
    }

    private record Fixture(Integer buildingId, Integer roomId, Integer serviceId) { }
}
