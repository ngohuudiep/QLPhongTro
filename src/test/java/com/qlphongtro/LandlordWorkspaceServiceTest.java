package com.qlphongtro;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.Tenant;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.BookingRequestRepository;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.repository.InvoiceRepository;
import com.qlphongtro.repository.MeterReadingRepository;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.RoomServiceRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.LandlordWorkspaceService;
import com.qlphongtro.service.RoomImageStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandlordWorkspaceServiceTest {

    @Mock BuildingRepository buildingRepository;
    @Mock RoomRepository roomRepository;
    @Mock RoomListingRepository roomListingRepository;
    @Mock BookingRequestRepository bookingRequestRepository;
    @Mock RoomImageStorageService roomImageStorageService;
    @Mock ContractRepository contractRepository;
    @Mock ContractTenantRepository contractTenantRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock UserRepository userRepository;
    @Mock TenantRepository tenantRepository;
    @Mock MeterReadingRepository meterReadingRepository;
    @Mock RoomServiceRepository roomServiceRepository;

    @InjectMocks
    LandlordWorkspaceService service;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void savingManagedRoomAlsoCreatesItsOwnedPublicListing() {
        User owner = new User();
        owner.setUserId(2);
        owner.setUsername("chutro");
        Building building = new Building();
        building.setBuildingId(10);
        building.setOwner(owner);
        when(buildingRepository.findByBuildingIdAndOwner_Username(10, "chutro")).thenReturn(Optional.of(building));
        when(roomRepository.findByRoomCode("P101")).thenReturn(Optional.empty());
        when(roomListingRepository.findByRoomCode("P101")).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setRoomId(101);
            return room;
        });
        when(roomListingRepository.save(any(RoomListing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();

        Room room = new Room();
        room.setRoomCode("P101");
        room.setFloorNo((short) 1);
        room.setPriceOverride(new BigDecimal("2500000"));
        room.setStatus("TRONG");
        RoomListing listing = new RoomListing();
        listing.setTitle("Phòng gần trung tâm");
        listing.setArea(new BigDecimal("24"));
        listing.setAmenities("Wifi");

        service.saveRoom(room, listing, 10, List.of(), null, null, List.of(), "chutro");

        ArgumentCaptor<RoomListing> listingCaptor = ArgumentCaptor.forClass(RoomListing.class);
        verify(roomListingRepository).save(listingCaptor.capture());
        RoomListing savedListing = listingCaptor.getValue();
        assertEquals(101, savedListing.getManagedRoom().getRoomId());
        assertEquals("P101", savedListing.getRoomCode());
        assertEquals(new BigDecimal("2500000"), savedListing.getPrice());
        assertEquals("TRONG", savedListing.getStatus());
        assertEquals(2, savedListing.getCreatedBy().getUserId());
    }

    @Test
    void landlordCannotReadRoomListingOwnedByAnotherAccount() {
        when(roomRepository.findByRoomIdAndBuilding_Owner_Username(42, "other-landlord"))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.getRoomListingForRoom(42, "other-landlord"));
    }

    @Test
    void editingContractPersistsTermFieldsAndRenewedEndDate() {
        User owner = new User();
        owner.setUsername("chutro");
        Building building = new Building();
        building.setOwner(owner);
        Room room = new Room();
        room.setBuilding(building);
        room.setPriceOverride(new BigDecimal("2500000"));
        Contract existing = new Contract();
        existing.setContractId(7);
        existing.setContractCode("C-OLD");
        existing.setRoom(room);
        existing.setStartDate(LocalDate.of(2025, 1, 1));
        existing.setEndDate(LocalDate.of(2025, 12, 31));
        existing.setBillingCycleMonths((short) 1);
        existing.setStatus("Hiệu lực");
        when(contractRepository.findById(7)).thenReturn(Optional.of(existing));
        when(contractRepository.existsByContractCodeAndContractIdNot("C-NEW", 7)).thenReturn(false);
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract input = new Contract();
        input.setContractCode("C-NEW");
        input.setStartDate(LocalDate.of(2025, 1, 1));
        input.setEndDate(LocalDate.of(2026, 12, 31));
        input.setBillingCycleMonths((short) 1);
        input.setContractType("Thuê phòng ở");
        input.setPaymentTerms("Thanh toán trước ngày 5 hàng tháng");

        Contract updated = service.updateContract(7, input, "chutro");

        assertEquals(LocalDate.of(2026, 12, 31), updated.getEndDate());
        assertEquals("Thuê phòng ở", updated.getContractType());
        assertEquals("Thanh toán trước ngày 5 hàng tháng", updated.getPaymentTerms());
    }

    @Test
    void creatingContractWithExistingTenantAccountReusesItsProfile() {
        User landlord = new User();
        landlord.setUsername("chutro");
        User tenantAccount = new User();
        tenantAccount.setUserId(21);
        tenantAccount.setEnabled(true);
        tenantAccount.setAccountStatus("APPROVED");
        tenantAccount.setRole(new Role(3, Role.TENANT));

        Building building = new Building();
        building.setOwner(landlord);
        Room room = new Room();
        room.setRoomId(101);
        room.setRoomCode("P101");
        room.setBuilding(building);
        room.setStatus("TRONG");

        Tenant profile = new Tenant();
        profile.setTenantId(12);
        profile.setFullName("Người thuê");

        when(roomRepository.findByIdForUpdate(101)).thenReturn(Optional.of(room));
        when(contractRepository.findByRoom_RoomId(101)).thenReturn(List.of());
        when(contractRepository.existsByContractCode("C-101")).thenReturn(false);
        when(userRepository.findById(21)).thenReturn(Optional.of(tenantAccount));
        when(tenantRepository.findByUser_UserId(21)).thenReturn(Optional.of(profile));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
            Contract saved = invocation.getArgument(0);
            saved.setContractId(51);
            return saved;
        });

        Contract details = new Contract();
        details.setContractCode("C-101");
        details.setStartDate(LocalDate.of(2026, 1, 1));
        details.setEndDate(LocalDate.of(2026, 12, 31));
        details.setMonthlyRent(new BigDecimal("2500000"));

        Contract saved = service.createContract(details, 101, null, 21, new Tenant(), "chutro");

        assertEquals(profile, saved.getRepresentative());
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void landlordTenantListUsesDirectContractAccountAndExcludesEndedContracts() {
        User tenantAccount = new User();
        tenantAccount.setUserId(3);
        tenantAccount.setUsername("nguoithue");
        tenantAccount.setFullName("Người thuê Demo");
        tenantAccount.setPhone("0909000003");

        Room room = new Room();
        room.setRoomId(1);
        room.setRoomCode("P101");

        Contract current = new Contract();
        current.setContractId(1);
        current.setContractCode("HD-101");
        current.setRoom(room);
        current.setTenantUser(tenantAccount);
        current.setStatus("TENANT_ACCEPTED");
        current.setStartDate(LocalDate.now().minusDays(1));
        current.setEndDate(LocalDate.now().plusDays(10));

        Contract ended = new Contract();
        ended.setContractId(2);
        ended.setContractCode("HD-OLD");
        ended.setRoom(room);
        ended.setTenantUser(tenantAccount);
        ended.setStatus("TERMINATED");
        ended.setStartDate(LocalDate.now().minusMonths(2));
        ended.setEndDate(LocalDate.now().minusDays(1));

        when(contractRepository.findByRoom_Building_Owner_UsernameOrderByStartDateDesc("owner"))
                .thenReturn(List.of(current, ended));

        var occupancies = service.getTenantOccupancies("owner");

        assertEquals(1, occupancies.size());
        assertEquals("nguoithue", occupancies.get(0).username());
        assertEquals("P101", occupancies.get(0).roomCode());
        assertEquals("HD-101", occupancies.get(0).contractCode());
        assertEquals("TENANT_ACCEPTED", occupancies.get(0).contractStatus());
        assertTrue(current.isTerminationRequestAllowed());
        current.setStatus("TERMINATION_REQUESTED");
        assertFalse(current.isTerminationRequestAllowed());
    }
}
