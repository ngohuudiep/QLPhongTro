package com.qlphongtro.service;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.ContractTenant;
import com.qlphongtro.entity.ContractTenantId;
import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.Tenant;
import com.qlphongtro.entity.User;
import com.qlphongtro.dto.TenantAccountOption;
import com.qlphongtro.dto.LandlordTenantView;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.repository.BookingRequestRepository;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.repository.InvoiceRepository;
import com.qlphongtro.repository.MeterReadingRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.repository.RoomServiceRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class LandlordWorkspaceService {

    private static final Map<String, String> ROOM_STATUSES = Map.of(
            "TRONG", "TRONG",
            "TRỐNG", "TRONG",
            "ĐANG THUÊ", "DA_THUE",
            "DA_THUE", "DA_THUE",
            "BẢO TRÌ", "BAO_TRI",
            "BAO_TRI", "BAO_TRI");

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final RoomListingRepository roomListingRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final RoomImageStorageService roomImageStorageService;
    private final ContractRepository contractRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RoomServiceRepository roomServiceRepository;

    public LandlordWorkspaceService(BuildingRepository buildingRepository,
                                    RoomRepository roomRepository,
    RoomListingRepository roomListingRepository,
    BookingRequestRepository bookingRequestRepository,
    RoomImageStorageService roomImageStorageService,
                                    ContractRepository contractRepository,
                                    ContractTenantRepository contractTenantRepository,
                                    InvoiceRepository invoiceRepository,
                                    TenantRepository tenantRepository,
                                    UserRepository userRepository,
                                    MeterReadingRepository meterReadingRepository,
                                    RoomServiceRepository roomServiceRepository) {
        this.buildingRepository = buildingRepository;
        this.roomRepository = roomRepository;
        this.roomListingRepository = roomListingRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.roomImageStorageService = roomImageStorageService;
        this.contractRepository = contractRepository;
        this.contractTenantRepository = contractTenantRepository;
        this.invoiceRepository = invoiceRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.roomServiceRepository = roomServiceRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(String username) {
        List<Room> rooms = roomRepository.findByBuilding_Owner_UsernameOrderByBuilding_BuildingNameAscRoomCodeAsc(username);
        List<Contract> contracts = contractRepository.findByRoom_Building_Owner_UsernameOrderByStartDateDesc(username);
        List<Invoice> invoices = invoiceRepository.findByContract_Room_Building_Owner_UsernameOrderByPeriodStartDesc(username);
        List<LandlordTenantView> currentTenants = buildTenantOccupancies(contracts);

        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        BigDecimal monthlyRevenue = invoices.stream()
                .filter(invoice -> invoice.getPeriodStart() != null && invoice.getPeriodStart().equals(currentMonth))
                .filter(invoice -> isPaid(invoice.getStatus()))
                .map(Invoice::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unpaidInvoices = invoices.stream().filter(invoice -> !isPaid(invoice.getStatus())).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("buildingCount", buildingRepository.findByOwner_UsernameOrderByBuildingNameAsc(username).size());
        stats.put("roomCount", rooms.size());
        stats.put("occupiedRoomCount", rooms.stream().filter(room -> "DA_THUE".equals(normalizeStatus(room.getStatus()))).count());
        stats.put("availableRoomCount", rooms.stream().filter(room -> "TRONG".equals(normalizeStatus(room.getStatus()))).count());
        stats.put("maintenanceRoomCount", rooms.stream().filter(room -> "BAO_TRI".equals(normalizeStatus(room.getStatus()))).count());
        stats.put("tenantCount", currentTenants.size());
        stats.put("monthlyRevenue", monthlyRevenue);
        stats.put("unpaidInvoiceCount", unpaidInvoices);
        stats.put("contractCount", contracts.size());
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Building> getBuildings(String username) {
        return buildingRepository.findByOwner_UsernameOrderByBuildingNameAsc(username);
    }

    @Transactional(readOnly = true)
    public Building getBuilding(Integer buildingId, String username) {
        return buildingRepository.findByBuildingIdAndOwner_Username(buildingId, username)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy hoặc bạn không có quyền truy cập khu trọ này."));
    }

    public Building saveBuilding(Building input, String username) {
        validateBuilding(input);
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản chủ trọ."));
        Building building;
        if (input.getBuildingId() == null) {
            if (buildingRepository.existsByBuildingCode(input.getBuildingCode().trim())) {
                throw new IllegalArgumentException("Mã khu trọ đã tồn tại.");
            }
            if (buildingRepository.existsByOwner_UserIdAndBuildingNameIgnoreCase(owner.getUserId(), input.getBuildingName().trim())) {
                throw new IllegalArgumentException("Tên khu trọ đã tồn tại trong tài khoản của bạn.");
            }
            building = new Building();
            building.setOwner(owner);
        } else {
            building = getBuilding(input.getBuildingId(), username);
            if (buildingRepository.existsByBuildingCodeAndBuildingIdNot(input.getBuildingCode().trim(), input.getBuildingId())) {
                throw new IllegalArgumentException("Mã khu trọ đã tồn tại.");
            }
            if (buildingRepository.existsByOwner_UserIdAndBuildingNameIgnoreCaseAndBuildingIdNot(
                    owner.getUserId(), input.getBuildingName().trim(), input.getBuildingId())) {
                throw new IllegalArgumentException("Tên khu trọ đã tồn tại trong tài khoản của bạn.");
            }
        }
        building.setBuildingCode(input.getBuildingCode().trim());
        building.setBuildingName(input.getBuildingName().trim());
        building.setAddress(input.getAddress().trim());
        building.setCity(input.getCity() != null ? input.getCity().trim() : null);
        building.setDescription(input.getDescription() != null ? input.getDescription().trim() : null);
        building.setContactPhone(input.getContactPhone() != null ? input.getContactPhone().trim() : null);
        building.setTotalFloors(input.getTotalFloors());
        return buildingRepository.save(building);
    }

    public void deleteBuilding(Integer buildingId, String username) {
        Building building = getBuilding(buildingId, username);
        if (roomRepository.countByBuilding_BuildingId(buildingId) > 0) {
            throw new IllegalStateException("Không thể xóa khu trọ đang có phòng. Hãy xử lý các phòng trước.");
        }
        buildingRepository.delete(building);
    }

    @Transactional(readOnly = true)
    public List<Room> getRooms(String username, String keyword, String status) {
        String normalizedStatus = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null : normalizeStatus(status);
        return roomRepository.findOwnedRooms(username,
                keyword == null || keyword.isBlank() ? null : keyword.trim(),
                normalizedStatus);
    }

    @Transactional(readOnly = true)
    public Room getRoom(Integer roomId, String username) {
        return roomRepository.findByRoomIdAndBuilding_Owner_Username(roomId, username)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy hoặc bạn không có quyền truy cập phòng này."));
    }

    @Transactional(readOnly = true)
    public RoomListing getRoomListingForRoom(Integer roomId, String username) {
        getRoom(roomId, username);
        return roomListingRepository.findByManagedRoom_RoomId(roomId)
                .orElseThrow(() -> new IllegalStateException("Phòng quản lý chưa được liên kết với bài đăng."));
    }

    public Room saveRoom(Room input,
                         RoomListing listingInput,
                         Integer buildingId,
                         List<String> retainedImageUrls,
                         String primaryImageUrl,
                         Integer primaryUploadIndex,
                         List<MultipartFile> uploadedImages,
                         String username) {
        validateRoom(input);
        validateListing(listingInput);
        Building building = getBuilding(buildingId, username);
        Room room;
        if (input.getRoomId() == null) {
            if ("DA_THUE".equals(normalizeStatus(input.getStatus()))) {
                throw new IllegalStateException("Hãy tạo hợp đồng để chuyển phòng sang trạng thái đang thuê.");
            }
            if (roomRepository.findByRoomCode(input.getRoomCode().trim()).isPresent()) {
                throw new IllegalArgumentException("Mã phòng đã tồn tại.");
            }
            room = new Room();
        } else {
            room = getRoom(input.getRoomId(), username);
            List<Contract> roomContracts = contractRepository.findByRoom_RoomId(input.getRoomId());
            boolean hasActiveContract = roomContracts.stream().anyMatch(LandlordWorkspaceService::isActiveContract);
            if (hasActiveContract && !"DA_THUE".equals(normalizeStatus(input.getStatus()))) {
                throw new IllegalStateException("Phòng có hợp đồng còn hiệu lực; hãy kết thúc hợp đồng trước khi đổi trạng thái.");
            }
            if (!hasActiveContract && "DA_THUE".equals(normalizeStatus(input.getStatus()))) {
                throw new IllegalStateException("Hãy tạo hợp đồng để chuyển phòng sang trạng thái đang thuê.");
            }
            if (!roomContracts.isEmpty() && !room.getBuilding().getBuildingId().equals(buildingId)) {
                throw new IllegalStateException("Không thể chuyển phòng có lịch sử hợp đồng sang khu trọ khác.");
            }
            roomRepository.findByRoomCode(input.getRoomCode().trim()).ifPresent(existing -> {
                if (!existing.getRoomId().equals(input.getRoomId())) {
                    throw new IllegalArgumentException("Mã phòng đã tồn tại.");
                }
            });
        }

        User owner = building.getOwner();
        if (owner == null || !username.equals(owner.getUsername())) {
            throw new AccessDeniedException("Bạn không có quyền quản lý khu trọ này.");
        }
        RoomListing listing;
        if (input.getRoomId() == null) {
            if (roomListingRepository.findByRoomCode(input.getRoomCode().trim()).isPresent()) {
                throw new IllegalArgumentException("Mã phòng đã tồn tại trong bài đăng.");
            }
            listing = new RoomListing();
            listing.setCreatedBy(owner);
        } else {
            listing = roomListingRepository.findByManagedRoom_RoomId(input.getRoomId())
                    .orElseThrow(() -> new IllegalStateException("Phòng quản lý chưa được liên kết với bài đăng."));
            if (listing.getCreatedBy() == null || !owner.getUserId().equals(listing.getCreatedBy().getUserId())) {
                throw new AccessDeniedException("Bạn không có quyền cập nhật bài đăng phòng này.");
            }
            if (!listing.getRoomCode().equalsIgnoreCase(input.getRoomCode().trim())
                    && roomListingRepository.findByRoomCode(input.getRoomCode().trim()).isPresent()) {
                throw new IllegalArgumentException("Mã phòng đã tồn tại trong bài đăng.");
            }
        }

        Set<String> oldImageUrls = imageUrls(listing);
        Set<String> allowedRetainedUrls = new LinkedHashSet<>(imageUrls(listing));
        List<String> retained = retainedImageUrls == null ? List.of() : retainedImageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();
        if (!allowedRetainedUrls.containsAll(retained)) {
            throw new IllegalArgumentException("Danh sách ảnh giữ lại không hợp lệ.");
        }
        List<String> newImageUrls = roomImageStorageService.store(uploadedImages);
        if (retained.size() + newImageUrls.size() > 5) {
            newImageUrls.forEach(roomImageStorageService::deleteManagedFile);
            throw new IllegalArgumentException("Mỗi phòng chỉ được lưu tối đa 5 ảnh.");
        }
        scheduleImageCleanup(oldImageUrls, Set.of(), newImageUrls);
        String primaryImage = resolvePrimaryImage(retained, newImageUrls, primaryImageUrl, primaryUploadIndex);

        room.setRoomCode(input.getRoomCode().trim());
        room.setBuilding(building);
        room.setFloorNo(input.getFloorNo());
        room.setMaxOccupants(input.getMaxOccupants());
        room.setPriceOverride(input.getPriceOverride());
        room.setStatus(normalizeStatus(input.getStatus()));
        Room savedRoom = roomRepository.save(room);

        listing.setManagedRoom(savedRoom);
        listing.setRoomCode(savedRoom.getRoomCode());
        listing.setTitle(listingInput.getTitle().trim());
        listing.setDescription(trimToNull(listingInput.getDescription()));
        listing.setFloorNo(savedRoom.getFloorNo() == null ? null : savedRoom.getFloorNo().intValue());
        listing.setArea(listingInput.getArea());
        listing.setPrice(savedRoom.getPriceOverride());
        listing.setStatus(savedRoom.getStatus());
        listing.setAmenities(trimToNull(listingInput.getAmenities()));
        listing.setCreatedBy(owner);
        listing.setImageUrl(primaryImage);
        List<String> gallery = new ArrayList<>();
        retained.forEach(url -> { if (!url.equals(primaryImage)) gallery.add(url); });
        newImageUrls.forEach(url -> { if (!url.equals(primaryImage)) gallery.add(url); });
        listing.setImageGallery(gallery.isEmpty() ? null : String.join(",", gallery));
        RoomListing savedListing = roomListingRepository.save(listing);
        savedRoom.setRoomCode(savedListing.getRoomCode());
        return savedRoom;
    }

    public void deleteRoom(Integer roomId, String username) {
        Room room = getRoom(roomId, username);
        if (contractRepository.findByRoom_RoomId(roomId).size() > 0) {
            throw new IllegalStateException("Không thể xóa phòng đã có hợp đồng; cần giữ nguyên lịch sử thuê.");
        }
        if (meterReadingRepository.existsByRoom_RoomId(roomId) || roomServiceRepository.existsByRoom_RoomId(roomId)) {
            throw new IllegalStateException("Không thể xóa phòng có lịch sử chỉ số hoặc dịch vụ.");
        }
        if ("DA_THUE".equals(normalizeStatus(room.getStatus()))) {
            throw new IllegalStateException("Không thể xóa phòng đang có người thuê.");
        }
        RoomListing listing = roomListingRepository.findByManagedRoom_RoomId(roomId).orElse(null);
        if (listing != null) {
            if (bookingRequestRepository.existsByRoom_ListingId(listing.getListingId())) {
                throw new IllegalStateException("Không thể xóa phòng có lịch sử yêu cầu thuê; hãy giữ lại để bảo toàn lịch sử.");
            }
            Set<String> imageUrls = imageUrls(listing);
            roomListingRepository.delete(listing);
            scheduleImageCleanup(imageUrls, Set.of(), List.of());
        }
        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<Contract> getContracts(String username) {
        return contractRepository.findByRoom_Building_Owner_UsernameOrderByStartDateDesc(username);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoices(String username) {
        return invoiceRepository.findByContract_Room_Building_Owner_UsernameOrderByPeriodStartDesc(username);
    }

    @Transactional(readOnly = true)
    public List<Tenant> getTenants(String username) {
        Set<Integer> tenantIds = new LinkedHashSet<>();
        List<Tenant> tenants = new ArrayList<>();
        for (Contract contract : getContracts(username)) {
            if (contract.getRepresentative() != null && tenantIds.add(contract.getRepresentative().getTenantId())) {
                tenants.add(contract.getRepresentative());
            }
            contract.getContractTenants().forEach(contractTenant -> {
                Tenant tenant = contractTenant.getTenant();
                if (tenant != null && tenantIds.add(tenant.getTenantId())) {
                    tenants.add(tenant);
                }
            });
        }
        return tenants;
    }

    @Transactional(readOnly = true)
    public List<LandlordTenantView> getTenantOccupancies(String username) {
        return buildTenantOccupancies(getContracts(username));
    }

    @Transactional(readOnly = true)
    public List<User> getApprovedTenantAccounts() {
        return userRepository.searchAccountsByRole(Role.TENANT, null).stream()
                .filter(User::isEnabled)
                .filter(User::isApproved)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TenantAccountOption> getTenantAccountOptions() {
        Map<Integer, Tenant> profilesByUserId = new LinkedHashMap<>();
        tenantRepository.findAll().stream()
                .filter(tenant -> tenant.getUser() != null)
                .forEach(tenant -> profilesByUserId.put(tenant.getUser().getUserId(), tenant));
        return getApprovedTenantAccounts().stream()
                .map(user -> {
                    Tenant profile = profilesByUserId.get(user.getUserId());
                    return new TenantAccountOption(user.getUserId(), user.getUsername(), user.getFullName(),
                            user.getPhone(), user.getEmail(),
                            profile == null ? null : profile.getTenantId(),
                            profile == null ? null : profile.getIdNumber(),
                            profile == null ? null : profile.getHometown(),
                            profile == null ? null : profile.getDateOfBirth());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms(String username) {
        return roomRepository.findByBuilding_Owner_UsernameOrderByBuilding_BuildingNameAscRoomCodeAsc(username)
                .stream()
                .filter(room -> "TRONG".equals(normalizeStatus(room.getStatus())))
                .filter(room -> contractRepository.findByRoom_RoomId(room.getRoomId()).stream()
                        .noneMatch(LandlordWorkspaceService::isActiveContract))
                .toList();
    }

    public Contract createContract(String contractCode,
                                   Integer roomId,
                                   Integer tenantId,
                                   Tenant newTenant,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   BigDecimal depositAmount,
                                   Short billingCycleMonths,
                                   String username) {
        Contract details = new Contract();
        details.setContractCode(contractCode);
        details.setStartDate(startDate);
        details.setEndDate(endDate);
        details.setDepositAmount(depositAmount);
        details.setBillingCycleMonths(billingCycleMonths);
        return createContract(details, roomId, tenantId, newTenant, username);
    }

    public Contract createContract(Contract details,
                                   Integer roomId,
                                   Integer tenantId,
                                   Tenant newTenant,
                                   String username) {
        return createContract(details, roomId, tenantId, null, newTenant, username);
    }

    public Contract createContract(Contract details,
                                   Integer roomId,
                                   Integer tenantId,
                                   Integer tenantUserId,
                                   Tenant newTenant,
                                   String username) {
        if (details == null) {
            throw new IllegalArgumentException("Thông tin hợp đồng không hợp lệ.");
        }
        if (tenantId != null && tenantUserId != null) {
            throw new IllegalArgumentException("Chỉ chọn một hồ sơ người thuê hoặc một tài khoản.");
        }
        String contractCode = details.getContractCode();
        LocalDate startDate = details.getStartDate();
        LocalDate endDate = details.getEndDate();
        BigDecimal depositAmount = details.getDepositAmount();
        Short billingCycleMonths = details.getBillingCycleMonths();
        if (contractCode == null || contractCode.isBlank() || contractCode.trim().length() > 30) {
            throw new IllegalArgumentException("Mã hợp đồng bắt buộc và tối đa 30 ký tự.");
        }
        if (contractRepository.existsByContractCode(contractCode.trim())) {
            throw new IllegalArgumentException("Mã hợp đồng đã tồn tại.");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.");
        }
        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền đặt cọc không được âm.");
        }
        if (billingCycleMonths != null && (billingCycleMonths < 1 || billingCycleMonths > 12)) {
            throw new IllegalArgumentException("Chu kỳ thanh toán phải từ 1 đến 12 tháng.");
        }
        validateContractDetails(details);

        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy phòng."));
        if (room.getBuilding().getOwner() == null || !username.equals(room.getBuilding().getOwner().getUsername())) {
            throw new AccessDeniedException("Bạn không có quyền tạo hợp đồng cho phòng này.");
        }
        if (!"TRONG".equals(normalizeStatus(room.getStatus()))) {
            throw new IllegalStateException("Chỉ phòng còn trống mới được đưa vào hợp đồng.");
        }
        boolean hasActiveContract = contractRepository.findByRoom_RoomId(roomId).stream()
                .anyMatch(LandlordWorkspaceService::isActiveContract);
        if (hasActiveContract) {
            throw new IllegalStateException("Phòng đã có hợp đồng còn hiệu lực.");
        }

        Tenant tenant;
        if (tenantId != null) {
            tenant = getTenants(username).stream()
                    .filter(existing -> existing.getTenantId().equals(tenantId))
                    .findFirst()
                    .orElseThrow(() -> new AccessDeniedException("Người thuê không thuộc các hợp đồng của bạn."));
        } else if (tenantUserId != null) {
            User tenantAccount = userRepository.findById(tenantUserId)
                    .filter(User::isEnabled)
                    .filter(User::isApproved)
                    .filter(user -> user.getRole() != null
                            && Role.TENANT.equalsIgnoreCase(user.getRole().getRoleName()))
                    .orElseThrow(() -> new AccessDeniedException("Tài khoản người thuê không hợp lệ hoặc chưa được duyệt."));
            tenant = tenantRepository.findByUser_UserId(tenantUserId).orElse(null);
            if (tenant == null) {
                if (newTenant == null) {
                    throw new IllegalArgumentException("Cần nhập CCCD/CMND để tạo hồ sơ thuê cho tài khoản đã chọn.");
                }
                Tenant accountTenant = new Tenant();
                accountTenant.setFullName(tenantAccount.getFullName());
                accountTenant.setPhone(tenantAccount.getPhone());
                accountTenant.setEmail(tenantAccount.getEmail());
                accountTenant.setIdNumber(newTenant.getIdNumber());
                accountTenant.setHometown(newTenant.getHometown());
                accountTenant.setDateOfBirth(newTenant.getDateOfBirth());
                accountTenant.setUser(tenantAccount);
                validateNewTenant(accountTenant);
                if (tenantRepository.existsByIdNumber(accountTenant.getIdNumber().trim())) {
                    throw new IllegalArgumentException("Số CCCD/CMND đã tồn tại; hãy chọn hồ sơ người thuê hiện có.");
                }
                accountTenant.setFullName(accountTenant.getFullName().trim());
                accountTenant.setPhone(accountTenant.getPhone().trim());
                accountTenant.setIdNumber(accountTenant.getIdNumber().trim());
                tenant = tenantRepository.save(accountTenant);
            }
        } else {
            validateNewTenant(newTenant);
            if (tenantRepository.existsByIdNumber(newTenant.getIdNumber().trim())) {
                throw new IllegalArgumentException("Số CCCD/CMND đã tồn tại; hãy chọn người thuê hiện có.");
            }
            newTenant.setFullName(newTenant.getFullName().trim());
            newTenant.setPhone(newTenant.getPhone().trim());
            newTenant.setIdNumber(newTenant.getIdNumber().trim());
            newTenant.setEmail(newTenant.getEmail() == null ? null : newTenant.getEmail().trim());
            tenant = tenantRepository.save(newTenant);
        }

        Contract contract = new Contract();
        contract.setContractCode(contractCode.trim());
        contract.setRoom(room);
        contract.setRepresentative(tenant);
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setDepositAmount(depositAmount);
        BigDecimal monthlyRent = details.getMonthlyRent() != null ? details.getMonthlyRent() : room.getPriceOverride();
        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tiền thuê tháng phải lớn hơn 0.");
        }
        contract.setMonthlyRent(monthlyRent);
        contract.setBillingCycleMonths(billingCycleMonths != null ? billingCycleMonths : Short.valueOf((short) 1));
        contract.setStatus("PENDING_TENANT_CONFIRMATION");
        copyContractDetails(details, contract);
        Contract saved = contractRepository.save(contract);

        ContractTenantId relationId = new ContractTenantId(saved.getContractId(), tenant.getTenantId());
        contractTenantRepository.save(ContractTenant.builder()
                .id(relationId)
                .contract(saved)
                .tenant(tenant)
                .moveInDate(startDate)
                .build());
        return saved;
    }

    public Contract updateContract(Integer contractId, Contract input, String username) {
        Contract contract = getContract(contractId, username);
        if ("TERMINATION_REQUESTED".equalsIgnoreCase(contract.getStatus())) {
            throw new IllegalStateException("Không thể sửa hợp đồng trong khi hai bên đang xử lý thanh lý.");
        }
        if (!isActiveContract(contract)) {
            throw new IllegalStateException("Chỉ hợp đồng còn hiệu lực mới được chỉnh sửa hoặc gia hạn.");
        }
        if (input.getContractCode() == null || input.getContractCode().isBlank()
                || input.getContractCode().trim().length() > 30) {
            throw new IllegalArgumentException("Mã hợp đồng bắt buộc và tối đa 30 ký tự.");
        }
        if (contractRepository.existsByContractCodeAndContractIdNot(input.getContractCode().trim(), contractId)) {
            throw new IllegalArgumentException("Mã hợp đồng đã tồn tại.");
        }
        if (input.getStartDate() == null || input.getEndDate() == null
                || input.getEndDate().isBefore(input.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.");
        }
        if (input.getDepositAmount() != null && input.getDepositAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền đặt cọc không được âm.");
        }
        if (input.getBillingCycleMonths() == null
                || input.getBillingCycleMonths() < 1 || input.getBillingCycleMonths() > 12) {
            throw new IllegalArgumentException("Chu kỳ thanh toán phải từ 1 đến 12 tháng.");
        }
        validateContractDetails(input);
        contract.setContractCode(input.getContractCode().trim());
        contract.setStartDate(input.getStartDate());
        contract.setEndDate(input.getEndDate());
        contract.setDepositAmount(input.getDepositAmount());
        BigDecimal monthlyRent = input.getMonthlyRent() != null ? input.getMonthlyRent() : contract.getRoom().getPriceOverride();
        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tiền thuê tháng phải lớn hơn 0.");
        }
        contract.setMonthlyRent(monthlyRent);
        contract.setBillingCycleMonths(input.getBillingCycleMonths());
        copyContractDetails(input, contract);
        return contractRepository.save(contract);
    }

    @Transactional(readOnly = true)
    public Contract getContract(Integer contractId, String username) {
        return contractRepository.findById(contractId)
                .filter(contract -> contract.getRoom() != null
                        && contract.getRoom().getBuilding() != null
                        && contract.getRoom().getBuilding().getOwner() != null
                        && username.equals(contract.getRoom().getBuilding().getOwner().getUsername()))
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy hoặc bạn không có quyền truy cập hợp đồng này."));
    }

    @Transactional(readOnly = true)
    public boolean hasInvoiceForPeriod(Integer contractId, LocalDate periodStart) {
        return invoiceRepository.existsByContract_ContractIdAndPeriodStart(contractId, periodStart);
    }

    private static void validateBuilding(Building building) {
        if (building.getBuildingCode() == null || building.getBuildingCode().isBlank()
                || building.getBuildingCode().length() > 20) {
            throw new IllegalArgumentException("Mã khu trọ bắt buộc và tối đa 20 ký tự.");
        }
        if (building.getBuildingName() == null || building.getBuildingName().isBlank()
                || building.getBuildingName().length() > 150) {
            throw new IllegalArgumentException("Tên khu trọ bắt buộc và tối đa 150 ký tự.");
        }
        if (building.getAddress() == null || building.getAddress().isBlank()
                || building.getAddress().length() > 255) {
            throw new IllegalArgumentException("Địa chỉ bắt buộc và tối đa 255 ký tự.");
        }
        if (building.getTotalFloors() != null && building.getTotalFloors() < 1) {
            throw new IllegalArgumentException("Số tầng phải lớn hơn 0.");
        }
        if (building.getContactPhone() != null && !building.getContactPhone().isBlank()
                && !building.getContactPhone().matches("^(0|\\+84)[0-9]{9,10}$")) {
            throw new IllegalArgumentException("Số điện thoại liên hệ không hợp lệ.");
        }
    }

    private static void validateRoom(Room room) {
        if (room.getRoomCode() == null || room.getRoomCode().isBlank() || room.getRoomCode().length() > 20) {
            throw new IllegalArgumentException("Mã phòng bắt buộc và tối đa 20 ký tự.");
        }
        if (room.getFloorNo() != null && room.getFloorNo() < 0) {
            throw new IllegalArgumentException("Tầng không được âm.");
        }
        if (room.getMaxOccupants() != null && room.getMaxOccupants() < 1) {
            throw new IllegalArgumentException("Sức chứa phải lớn hơn 0.");
        }
        if (room.getPriceOverride() == null || room.getPriceOverride().compareTo(new BigDecimal("100000")) < 0) {
            throw new IllegalArgumentException("Giá thuê phải từ 100.000 VNĐ trở lên.");
        }
        if (!ROOM_STATUSES.containsKey(room.getStatus() == null ? "" : room.getStatus().trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Trạng thái phòng không hợp lệ.");
        }
    }

    private static void validateListing(RoomListing listing) {
        if (listing == null || listing.getTitle() == null || listing.getTitle().isBlank()
                || listing.getTitle().trim().length() > 255) {
            throw new IllegalArgumentException("Tên hiển thị bắt buộc và tối đa 255 ký tự.");
        }
        if (listing.getArea() == null || listing.getArea().compareTo(new BigDecimal("5.0")) < 0) {
            throw new IllegalArgumentException("Diện tích phải từ 5 m² trở lên.");
        }
        if (listing.getAmenities() != null && listing.getAmenities().length() > 500) {
            throw new IllegalArgumentException("Tiện nghi tối đa 500 ký tự.");
        }
    }

    private static void validateContractDetails(Contract contract) {
        if (contract.getContractType() != null && contract.getContractType().trim().length() > 50) {
            throw new IllegalArgumentException("Loại hợp đồng tối đa 50 ký tự.");
        }
        if (contract.getPaymentMethod() != null && contract.getPaymentMethod().trim().length() > 100) {
            throw new IllegalArgumentException("Phương thức thanh toán tối đa 100 ký tự.");
        }
        if (contract.getPaymentDay() != null && (contract.getPaymentDay() < 1 || contract.getPaymentDay() > 28)) {
            throw new IllegalArgumentException("Ngày thanh toán phải từ 1 đến 28.");
        }
        List<BigDecimal> fees = List.of(
                contract.getMonthlyRent() == null ? BigDecimal.ZERO : contract.getMonthlyRent(),
                contract.getMonthlyElectricPrice() == null ? BigDecimal.ZERO : contract.getMonthlyElectricPrice(),
                contract.getMonthlyWaterPrice() == null ? BigDecimal.ZERO : contract.getMonthlyWaterPrice(),
                contract.getInternetFee() == null ? BigDecimal.ZERO : contract.getInternetFee(),
                contract.getServiceFee() == null ? BigDecimal.ZERO : contract.getServiceFee(),
                contract.getSanitationFee() == null ? BigDecimal.ZERO : contract.getSanitationFee(),
                contract.getOtherServiceFee() == null ? BigDecimal.ZERO : contract.getOtherServiceFee());
        if (fees.stream().anyMatch(fee -> fee.compareTo(BigDecimal.ZERO) < 0)) {
            throw new IllegalArgumentException("Chi phí trong hợp đồng không được âm.");
        }
    }

    private static void copyContractDetails(Contract source, Contract target) {
        target.setContractType(trimToNull(source.getContractType()));
        target.setContractDate(source.getContractDate() != null ? source.getContractDate() : LocalDate.now());
        target.setPaymentDay(source.getPaymentDay());
        target.setMonthlyElectricPrice(source.getMonthlyElectricPrice());
        target.setMonthlyWaterPrice(source.getMonthlyWaterPrice());
        target.setInternetFee(source.getInternetFee());
        target.setServiceFee(source.getServiceFee());
        target.setSanitationFee(source.getSanitationFee());
        target.setOtherServiceFee(source.getOtherServiceFee());
        target.setPaymentMethod(trimToNull(source.getPaymentMethod()));
        target.setPaymentTerms(trimToNull(source.getPaymentTerms()));
        target.setDepositTerms(trimToNull(source.getDepositTerms()));
        target.setUtilityTerms(trimToNull(source.getUtilityTerms()));
        target.setAssetTerms(trimToNull(source.getAssetTerms()));
        target.setRepairTerms(trimToNull(source.getRepairTerms()));
        target.setTerminationTerms(trimToNull(source.getTerminationTerms()));
        target.setOccupantTerms(trimToNull(source.getOccupantTerms()));
        target.setRenewalTerms(trimToNull(source.getRenewalTerms()));
        target.setLandlordResponsibilities(trimToNull(source.getLandlordResponsibilities()));
        target.setTenantResponsibilities(trimToNull(source.getTenantResponsibilities()));
        target.setAdditionalTerms(trimToNull(source.getAdditionalTerms()));
    }

    private static String resolvePrimaryImage(List<String> retained,
                                             List<String> uploaded,
                                             String selectedRetainedUrl,
                                             Integer primaryUploadIndex) {
        if (primaryUploadIndex != null) {
            if (primaryUploadIndex < 0 || primaryUploadIndex >= uploaded.size()) {
                throw new IllegalArgumentException("Ảnh đại diện được chọn không hợp lệ.");
            }
            return uploaded.get(primaryUploadIndex);
        }
        if (selectedRetainedUrl != null && !selectedRetainedUrl.isBlank()) {
            if (!retained.contains(selectedRetainedUrl)) {
                throw new IllegalArgumentException("Ảnh đại diện được chọn không hợp lệ.");
            }
            return selectedRetainedUrl;
        }
        if (!retained.isEmpty()) {
            return retained.get(0);
        }
        return uploaded.isEmpty() ? null : uploaded.get(0);
    }

    private Set<String> imageUrls(RoomListing listing) {
        Set<String> urls = new LinkedHashSet<>();
        if (listing.getStoredImageUrl() != null && !listing.getStoredImageUrl().isBlank()) {
            urls.add(listing.getStoredImageUrl());
        }
        if (listing.getImageGallery() != null && !listing.getImageGallery().isBlank()) {
            for (String url : listing.getImageGallery().split(",")) {
                if (!url.isBlank()) {
                    urls.add(url.trim());
                }
            }
        }
        return urls;
    }

    private void scheduleImageCleanup(Set<String> oldUrls, Set<String> ignoredCurrentUrls, List<String> newlyStoredUrls) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Không thể quản lý vòng đời ảnh ngoài transaction.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    newlyStoredUrls.forEach(roomImageStorageService::deleteManagedFile);
                    return;
                }
                Set<String> referencedUrls = new LinkedHashSet<>();
                roomListingRepository.findAll().forEach(listing -> referencedUrls.addAll(imageUrls(listing)));
                oldUrls.stream()
                        .filter(url -> !ignoredCurrentUrls.contains(url))
                        .filter(roomImageStorageService::isManagedUrl)
                        .filter(url -> !referencedUrls.contains(url))
                        .forEach(roomImageStorageService::deleteManagedFile);
            }
        });
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validateNewTenant(Tenant tenant) {
        if (tenant == null || tenant.getFullName() == null || tenant.getFullName().isBlank()) {
            throw new IllegalArgumentException("Họ tên người thuê là bắt buộc.");
        }
        if (tenant.getIdNumber() == null || tenant.getIdNumber().isBlank()) {
            throw new IllegalArgumentException("Số CCCD/CMND là bắt buộc.");
        }
        if (tenant.getPhone() == null || tenant.getPhone().isBlank()) {
            throw new IllegalArgumentException("Số điện thoại là bắt buộc.");
        }
        if (tenant.getFullName().length() > 150 || tenant.getIdNumber().length() > 20
                || tenant.getPhone().length() > 20) {
            throw new IllegalArgumentException("Thông tin người thuê vượt quá độ dài cho phép.");
        }
        if (!tenant.getPhone().matches("^(0|\\+84)[0-9]{9,10}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (tenant.getHometown() != null && tenant.getHometown().length() > 255) {
            throw new IllegalArgumentException("Địa chỉ thường trú tối đa 255 ký tự.");
        }
        if (tenant.getDateOfBirth() != null && !tenant.getDateOfBirth().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh phải nằm trong quá khứ.");
        }
        if (tenant.getEmail() != null && !tenant.getEmail().isBlank()
                && !tenant.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        return ROOM_STATUSES.getOrDefault(status.trim().toUpperCase(Locale.ROOT), status.trim());
    }

    private static boolean isPaid(String status) {
        return "PAID".equalsIgnoreCase(status) || "ĐÃ THANH TOÁN".equalsIgnoreCase(status);
    }

    private static List<LandlordTenantView> buildTenantOccupancies(List<Contract> contracts) {
        LocalDate today = LocalDate.now();
        List<LandlordTenantView> occupancies = new ArrayList<>();
        for (Contract contract : contracts) {
            if (!isCurrentOccupancyContract(contract, today)) {
                continue;
            }

            Set<String> seen = new LinkedHashSet<>();
            Tenant representative = contract.getRepresentative();
            if (representative != null) {
                addTenantOccupancy(occupancies, seen, contract, representative.getUser(), representative,
                        null, null, null);
            }

            if (contract.getContractTenants() != null) {
                contract.getContractTenants().stream()
                        .filter(link -> link.getTenant() != null)
                        .filter(link -> link.getMoveInDate() == null || !link.getMoveInDate().isAfter(today))
                        .filter(link -> link.getMoveOutDate() == null || !link.getMoveOutDate().isBefore(today))
                        .forEach(link -> {
                            Tenant tenant = link.getTenant();
                            if (representative == null || !representative.getTenantId().equals(tenant.getTenantId())) {
                                addTenantOccupancy(occupancies, seen, contract, tenant.getUser(), tenant,
                                        null, null, null);
                            }
                        });
            }

            User tenantUser = contract.getTenantUser();
            if (tenantUser != null) {
                addTenantOccupancy(occupancies, seen, contract, tenantUser, null, null, null, null);
            }

            if (seen.isEmpty() && contract.getRentalRequest() != null) {
                addTenantOccupancy(occupancies, seen, contract, contract.getRentalRequest().getUser(), null,
                        contract.getRentalRequest().getFullName(),
                        contract.getRentalRequest().getPhone(),
                        contract.getRentalRequest().getIdNumber());
            }
        }
        return occupancies;
    }

    private static void addTenantOccupancy(List<LandlordTenantView> occupancies, Set<String> seen,
                                           Contract contract, User user, Tenant tenant,
                                           String fallbackName, String fallbackPhone, String fallbackIdNumber) {
        Integer userId = user != null ? user.getUserId() : null;
        String key = userId != null ? "user:" + userId
                : tenant != null ? "tenant:" + tenant.getTenantId()
                : "request:" + contract.getContractId();
        if (!seen.add(key)) {
            return;
        }

        String fullName = tenant != null && tenant.getFullName() != null
                ? tenant.getFullName()
                : user != null && user.getFullName() != null ? user.getFullName() : fallbackName;
        String phone = tenant != null && tenant.getPhone() != null
                ? tenant.getPhone()
                : user != null && user.getPhone() != null ? user.getPhone() : fallbackPhone;
        String email = tenant != null && tenant.getEmail() != null
                ? tenant.getEmail() : user != null ? user.getEmail() : null;
        String idNumber = tenant != null && tenant.getIdNumber() != null
                ? tenant.getIdNumber() : fallbackIdNumber;

        occupancies.add(new LandlordTenantView(userId, user != null ? user.getUsername() : null,
                fullName, phone, email, idNumber, contract.getRoom().getRoomCode(),
                contract.getContractCode(), contract.getStartDate(), contract.getEndDate(),
                contract.getStatus(), contract.getStatusLabel(), contract.getContractId()));
    }

    private static boolean isCurrentOccupancyContract(Contract contract, LocalDate today) {
        if (!isActiveContract(contract) || contract.getActualEndDate() != null
                || contract.getStartDate() == null || contract.getStartDate().isAfter(today)) {
            return false;
        }
        return contract.getEndDate() == null || !contract.getEndDate().isBefore(today);
    }

    private static boolean isActiveContract(Contract contract) {
        String status = contract.getStatus();
        return "ACTIVE".equalsIgnoreCase(status)
                || "HIỆU LỰC".equalsIgnoreCase(status)
                || "ĐANG HIỆU LỰC".equalsIgnoreCase(status)
                || "TENANT_ACCEPTED".equalsIgnoreCase(status)
                || "TERMINATION_REQUESTED".equalsIgnoreCase(status);
    }

}
