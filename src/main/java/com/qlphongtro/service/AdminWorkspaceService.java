package com.qlphongtro.service;

import com.qlphongtro.dto.AdminAccountForm;
import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.User;
import com.qlphongtro.entity.UserNotification;
import com.qlphongtro.repository.BookingRequestRepository;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.MeterReadingRepository;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.RoomServiceRepository;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.repository.UserNotificationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class AdminWorkspaceService {

    private static final List<String> MANAGED_ROLES = List.of(Role.LANDLORD, Role.TENANT);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoomListingRepository roomListingRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final ContractRepository contractRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomListingService roomListingService;
    private final MeterReadingRepository meterReadingRepository;
    private final RoomServiceRepository roomServiceRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final SessionRegistry sessionRegistry;

    public AdminWorkspaceService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 RoomListingRepository roomListingRepository,
                                 RoomRepository roomRepository,
                                 BuildingRepository buildingRepository,
                                 ContractRepository contractRepository,
                                 BookingRequestRepository bookingRequestRepository,
                                 PasswordEncoder passwordEncoder,
                                 RoomListingService roomListingService,
                                 MeterReadingRepository meterReadingRepository,
                                 RoomServiceRepository roomServiceRepository,
                                 UserNotificationRepository userNotificationRepository,
                                 SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.roomListingRepository = roomListingRepository;
        this.roomRepository = roomRepository;
        this.buildingRepository = buildingRepository;
        this.contractRepository = contractRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.roomListingService = roomListingService;
        this.meterReadingRepository = meterReadingRepository;
        this.roomServiceRepository = roomServiceRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("accountCount", userRepository.count());
        stats.put("adminCount", userRepository.countByRole_RoleNameIgnoreCase(Role.ADMIN));
        stats.put("landlordCount", userRepository.countByRole_RoleNameIgnoreCase(Role.LANDLORD));
        stats.put("tenantCount", userRepository.countByRole_RoleNameIgnoreCase(Role.TENANT));
        stats.put("pendingLandlordCount", countPendingLandlords());
        stats.put("approvedLandlordCount",
                userRepository.countByRole_RoleNameIgnoreCaseAndAccountStatusIgnoreCase(Role.LANDLORD, "APPROVED"));
        stats.put("listingCount", roomListingRepository.count());
        stats.put("listingOccupiedCount", roomListingRepository.countByStatus("DA_THUE"));
        stats.put("listingAvailableCount", roomListingRepository.countByStatus("TRONG"));
        stats.put("listingMaintenanceCount", roomListingRepository.countByStatus("BAO_TRI"));
        stats.put("managedRoomCount", roomRepository.count());
        stats.put("managedOccupiedCount", roomRepository.countByStatus("DA_THUE")
                + roomRepository.countByStatus("Đang thuê"));
        stats.put("managedAvailableCount", roomRepository.countByStatus("TRONG")
                + roomRepository.countByStatus("Trống"));
        stats.put("managedMaintenanceCount", roomRepository.countByStatus("BAO_TRI")
                + roomRepository.countByStatus("Bảo trì"));
        stats.put("buildingCount", buildingRepository.count());
        stats.put("activeContractCount", contractRepository.countByStatus("Hiệu lực")
                + contractRepository.countByStatus("ACTIVE"));
        return stats;
    }

    @Transactional(readOnly = true)
    public List<User> getPendingLandlords() {
        return getLandlordRegistrations("PENDING");
    }

    @Transactional(readOnly = true)
    public List<User> getLandlordRegistrations(String status) {
        String filter = status == null || status.isBlank() ? "PENDING" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "PENDING", "APPROVED", "REJECTED").contains(filter)) {
            filter = "PENDING";
        }
        String selectedFilter = filter;
        return userRepository.searchAccountsByRole(Role.LANDLORD, null).stream()
                .filter(user -> switch (selectedFilter) {
                    case "PENDING" -> isPendingApproval(user.getAccountStatus());
                    case "APPROVED" -> "APPROVED".equalsIgnoreCase(user.getAccountStatus());
                    case "REJECTED" -> "REJECTED".equalsIgnoreCase(user.getAccountStatus());
                    default -> true;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public long countPendingLandlords() {
        return userRepository.countByRole_RoleNameIgnoreCaseAndAccountStatusIgnoreCase(Role.LANDLORD, "PENDING_APPROVAL")
                + userRepository.countByRole_RoleNameIgnoreCaseAndAccountStatusIgnoreCase(Role.LANDLORD, "PENDING");
    }

    @Transactional(readOnly = true)
    public long countApprovedLandlords() {
        return userRepository.countByRole_RoleNameIgnoreCaseAndAccountStatusIgnoreCase(Role.LANDLORD, "APPROVED");
    }

    public void reviewLandlord(Integer userId, boolean approve, String rejectionReason, String adminUsername) {
        User user = getManagedUser(userId, Role.LANDLORD);
        if (!isPendingApproval(user.getAccountStatus())) {
            throw new IllegalStateException("Chỉ yêu cầu đang chờ mới có thể được xử lý.");
        }
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new AccessDeniedException("Không xác định được tài khoản Admin thực hiện."));
        if (!Role.ADMIN.equals(Role.normalizeRoleName(
                admin.getRole() == null ? null : admin.getRole().getRoleName()))) {
            throw new AccessDeniedException("Chỉ Admin mới được xét duyệt tài khoản chủ trọ.");
        }

        LocalDateTime reviewedAt = LocalDateTime.now();
        if (approve) {
            user.setAccountStatus("APPROVED");
            user.setEnabled(true);
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            user.setApprovedAt(reviewedAt);
            user.setApprovedBy(admin);
            user.setRejectedAt(null);
            user.setRejectionReason(null);
        } else {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
            }
            String reason = rejectionReason.trim();
            if (reason.length() > 1000) {
                throw new IllegalArgumentException("Lý do từ chối tối đa 1000 ký tự.");
            }
            user.setAccountStatus("REJECTED");
            user.setRejectedAt(reviewedAt);
            user.setRejectionReason(reason);
        }
        userRepository.save(user);
        expireSessions(user.getUsername());

        UserNotification notification = new UserNotification();
        notification.setRecipient(user);
        notification.setNotificationType(UserNotification.TYPE_ACCOUNT_REVIEW);
        notification.setTitle(approve ? "Tài khoản chủ trọ đã được duyệt" : "Tài khoản chủ trọ bị từ chối");
        notification.setMessage(approve
                ? "Tài khoản của bạn đã được Admin phê duyệt. Bạn có thể đăng nhập và sử dụng hệ thống."
                : "Tài khoản đăng ký chủ trọ bị từ chối. Lý do: " + rejectionReason.trim());
        userNotificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<User> getAccounts(String roleName, String keyword) {
        String role = normalizeManagedRole(roleName);
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return userRepository.searchAccountsByRole(role, query);
    }

    @Transactional(readOnly = true)
    public User getAccount(Integer userId, String roleName) {
        return getManagedUser(userId, normalizeManagedRole(roleName));
    }

    public User saveAccount(AdminAccountForm form, String roleName) {
        String role = normalizeManagedRole(roleName);
        validateAccount(form);

        User user;
        boolean isNew = form.getUserId() == null;
        if (isNew) {
            if (userRepository.existsByUsername(form.getUsername().trim())) {
                throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng.");
            }
            if (userRepository.existsByEmail(form.getEmail().trim())) {
                throw new IllegalArgumentException("Email đã được sử dụng.");
            }
            if (userRepository.existsByPhone(form.getPhone().trim())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
            }
            user = new User();
            user.setUsername(form.getUsername().trim());
            user.setPassword(passwordEncoder.encode(form.getPassword()));
            user.setRole(roleRepository.findByRoleNameIgnoreCase(role)
                    .orElseThrow(() -> new IllegalStateException("Chưa cấu hình vai trò " + role + ".")));
            user.setAccountStatus("APPROVED");
            user.setEnabled(true);
            user.setRegisteredAt(LocalDateTime.now());
        } else {
            user = getManagedUser(form.getUserId(), role);
            if (userRepository.existsByEmailAndUserIdNot(form.getEmail().trim(), user.getUserId())) {
                throw new IllegalArgumentException("Email đã được sử dụng.");
            }
            if (userRepository.existsByPhoneAndUserIdNot(form.getPhone().trim(), user.getUserId())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
            }
            if (form.getPassword() != null && !form.getPassword().isBlank()) {
                if (form.getPassword().length() < 8) {
                    throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự.");
                }
                user.setPassword(passwordEncoder.encode(form.getPassword()));
            }
        }

        user.setFullName(form.getFullName().trim());
        user.setEmail(form.getEmail().trim());
        user.setPhone(form.getPhone().trim());
        return userRepository.save(user);
    }

    public void setAccountEnabled(Integer userId, String roleName, boolean enabled) {
        User user = getManagedUser(userId, normalizeManagedRole(roleName));
        user.setEnabled(enabled);
        if (!enabled) {
            expireSessions(user.getUsername());
        }
        if (enabled) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
        }
        userRepository.save(user);
    }

    public void deleteAccount(Integer userId, String roleName, String currentUsername) {
        User user = getManagedUser(userId, normalizeManagedRole(roleName));
        if (user.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new IllegalStateException("Không thể xóa tài khoản đang đăng nhập.");
        }
        expireSessions(user.getUsername());
        boolean referenced = buildingRepository.existsByOwner_UserId(userId)
                || roomListingRepository.existsByCreatedBy_UserId(userId)
                || bookingRequestRepository.existsByUser_UserId(userId)
                || bookingRequestRepository.existsByProcessedBy_UserId(userId);
        if (referenced) {
            throw new IllegalStateException("Không thể xóa tài khoản còn liên kết với khu trọ, bài đăng hoặc yêu cầu thuê. Hãy khóa tài khoản để bảo toàn lịch sử.");
        }
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<User> getLandlords() {
        return userRepository.searchAccountsByRole(Role.LANDLORD, null).stream()
                .filter(User::isApproved)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Building> getManagedBuildings() {
        return buildingRepository.findAllByOrderByBuildingNameAsc();
    }

    @Transactional(readOnly = true)
    public Building getManagedBuilding(Integer id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khu trọ."));
    }

    public Building saveManagedBuilding(Building input, Integer ownerId) {
        if (input.getBuildingCode() == null || input.getBuildingCode().isBlank()
                || input.getBuildingCode().trim().length() > 20
                || input.getBuildingName() == null || input.getBuildingName().isBlank()
                || input.getBuildingName().trim().length() > 150
                || input.getAddress() == null || input.getAddress().isBlank()
                || input.getAddress().trim().length() > 255) {
            throw new IllegalArgumentException("Mã, tên khu trọ và địa chỉ là bắt buộc; vui lòng kiểm tra độ dài.");
        }
        Building building = input.getBuildingId() == null
                ? new Building()
                : getManagedBuilding(input.getBuildingId());
        String code = input.getBuildingCode().trim();
        if (buildingRepository.existsByBuildingCodeAndBuildingIdNot(code, building.getBuildingId() == null ? -1 : building.getBuildingId())) {
            throw new IllegalArgumentException("Mã khu trọ đã tồn tại.");
        }
        building.setBuildingCode(code);
        building.setBuildingName(input.getBuildingName().trim());
        building.setAddress(input.getAddress().trim());
        building.setCity(input.getCity() == null ? null : input.getCity().trim());
        building.setTotalFloors(input.getTotalFloors());
        if (input.getTotalFloors() != null && input.getTotalFloors() < 1) {
            throw new IllegalArgumentException("Số tầng phải lớn hơn 0.");
        }
        building.setOwner(ownerId == null ? null : approvedLandlord(ownerId));
        return buildingRepository.save(building);
    }

    public void deleteManagedBuilding(Integer id) {
        Building building = getManagedBuilding(id);
        if (roomRepository.countByBuilding_BuildingId(id) > 0) {
            throw new IllegalStateException("Không thể xóa khu trọ đang có phòng.");
        }
        buildingRepository.delete(building);
    }

    @Transactional(readOnly = true)
    public List<Room> getManagedRooms(String keyword, String status, Integer ownerId) {
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String selectedStatus = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null : normalizeManagedRoomStatus(status);
        List<Room> rooms = roomRepository.searchAllForAdmin(query, null, ownerId);
        return selectedStatus == null ? rooms : rooms.stream()
                .filter(room -> selectedStatus.equals(normalizeManagedRoomStatus(room.getStatus())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Room getManagedRoom(Integer id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));
    }

    public Room saveManagedRoom(Room input, Integer buildingId) {
        if (input.getRoomCode() == null || input.getRoomCode().isBlank() || input.getRoomCode().trim().length() > 20) {
            throw new IllegalArgumentException("Mã phòng là bắt buộc và tối đa 20 ký tự.");
        }
        if (input.getFloorNo() != null && input.getFloorNo() < 0) {
            throw new IllegalArgumentException("Tầng không được âm.");
        }
        if (input.getMaxOccupants() != null && input.getMaxOccupants() < 1) {
            throw new IllegalArgumentException("Sức chứa phải lớn hơn 0.");
        }
        if (input.getPriceOverride() != null && input.getPriceOverride().signum() < 0) {
            throw new IllegalArgumentException("Giá thuê không được âm.");
        }
        String status = normalizeManagedRoomStatus(input.getStatus());
        if (!List.of("TRONG", "DA_THUE", "BAO_TRI").contains(status)) {
            throw new IllegalArgumentException("Trạng thái phòng không hợp lệ.");
        }
        Building building = getManagedBuilding(buildingId);
        Room room;
        if (input.getRoomId() == null) {
            if (roomRepository.findByRoomCode(input.getRoomCode().trim()).isPresent()) {
                throw new IllegalArgumentException("Mã phòng đã tồn tại.");
            }
            if ("DA_THUE".equals(status)) {
                throw new IllegalArgumentException("Phòng đang thuê phải được tạo thông qua hợp đồng.");
            }
            room = new Room();
        } else {
            room = getManagedRoom(input.getRoomId());
            List<com.qlphongtro.entity.Contract> contracts = contractRepository.findByRoom_RoomId(room.getRoomId());
            boolean hasActiveContract = contracts.stream().anyMatch(contract ->
                    "ACTIVE".equalsIgnoreCase(contract.getStatus()) || "HIỆU LỰC".equalsIgnoreCase(contract.getStatus()));
            if (hasActiveContract && !"DA_THUE".equals(status)) {
                throw new IllegalStateException("Phòng có hợp đồng còn hiệu lực; hãy kết thúc hợp đồng trước.");
            }
            if (!hasActiveContract && "DA_THUE".equals(status)) {
                throw new IllegalStateException("Hãy tạo hợp đồng để đánh dấu phòng đang thuê.");
            }
            if (!contracts.isEmpty() && !room.getBuilding().getBuildingId().equals(buildingId)) {
                throw new IllegalStateException("Không thể chuyển phòng có lịch sử hợp đồng sang khu trọ khác.");
            }
            roomRepository.findByRoomCode(input.getRoomCode().trim()).ifPresent(existing -> {
                if (!existing.getRoomId().equals(room.getRoomId())) {
                    throw new IllegalArgumentException("Mã phòng đã tồn tại.");
                }
            });
        }
        room.setRoomCode(input.getRoomCode().trim());
        room.setBuilding(building);
        room.setFloorNo(input.getFloorNo());
        room.setMaxOccupants(input.getMaxOccupants());
        room.setPriceOverride(input.getPriceOverride());
        room.setStatus(status);
        Room savedRoom = roomRepository.save(room);
        roomListingRepository.findByManagedRoom_RoomId(savedRoom.getRoomId()).ifPresent(listing -> {
            listing.setRoomCode(savedRoom.getRoomCode());
            listing.setFloorNo(savedRoom.getFloorNo() == null ? null : savedRoom.getFloorNo().intValue());
            listing.setPrice(savedRoom.getPriceOverride());
            listing.setStatus(savedRoom.getStatus());
            if (savedRoom.getBuilding().getOwner() != null) {
                listing.setCreatedBy(savedRoom.getBuilding().getOwner());
            }
            roomListingRepository.save(listing);
        });
        return savedRoom;
    }

    public void deleteManagedRoom(Integer id) {
        Room room = getManagedRoom(id);
        if (roomListingRepository.findByManagedRoom_RoomId(id).isPresent()) {
            throw new IllegalStateException("Phòng này còn bài đăng đã liên kết; hãy xử lý bài đăng trước khi xóa.");
        }
        if (!contractRepository.findByRoom_RoomId(id).isEmpty()) {
            throw new IllegalStateException("Không thể xóa phòng đã có hợp đồng; cần giữ nguyên lịch sử thuê.");
        }
        if (meterReadingRepository.existsByRoom_RoomId(id) || roomServiceRepository.existsByRoom_RoomId(id)) {
            throw new IllegalStateException("Không thể xóa phòng có lịch sử chỉ số hoặc dịch vụ.");
        }
        if ("DA_THUE".equals(normalizeManagedRoomStatus(room.getStatus()))) {
            throw new IllegalStateException("Không thể xóa phòng đang có người thuê.");
        }
        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<RoomListing> getRoomListings(String keyword, String status, Integer ownerId) {
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String selectedStatus = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null : status.trim().toUpperCase(Locale.ROOT);
        List<RoomListing> rooms = roomListingRepository.searchAndFilter(
                query, selectedStatus, null, null, null, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ownerId == null ? rooms : rooms.stream()
                .filter(room -> room.getCreatedBy() != null && ownerId.equals(room.getCreatedBy().getUserId()))
                .toList();
    }

    public RoomListing saveRoomListing(RoomListing input, Integer ownerId) {
        if (input.getStatus() == null || !List.of("TRONG", "DA_THUE", "BAO_TRI")
                .contains(input.getStatus().trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Trạng thái phòng không hợp lệ.");
        }
        if (input.getFloorNo() != null && input.getFloorNo() < 0) {
            throw new IllegalArgumentException("Tầng không được âm.");
        }
        RoomListing room;
        if (input.getListingId() == null) {
            room = input;
            room.setCreatedAt(LocalDateTime.now());
        } else {
            room = roomListingRepository.findById(input.getListingId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài đăng phòng."));
            if (room.getManagedRoom() != null) {
                Room managedRoom = room.getManagedRoom();
                if (managedRoom.getBuilding().getOwner() == null || ownerId == null
                        || !ownerId.equals(managedRoom.getBuilding().getOwner().getUserId())) {
                    throw new IllegalStateException("Không thể đổi chủ sở hữu riêng của bài đăng đã liên kết; hãy chuyển cả khu trọ qua quy trình riêng.");
                }
                String mappedStatus = normalizeManagedRoomStatus(input.getStatus());
                List<com.qlphongtro.entity.Contract> roomContracts = contractRepository.findByRoom_RoomId(managedRoom.getRoomId());
                boolean hasActiveContract = roomContracts.stream().anyMatch(contract ->
                        "ACTIVE".equalsIgnoreCase(contract.getStatus()) || "HIỆU LỰC".equalsIgnoreCase(contract.getStatus()));
                if ((hasActiveContract && !"DA_THUE".equals(mappedStatus))
                        || (!hasActiveContract && "DA_THUE".equals(mappedStatus))) {
                    throw new IllegalStateException("Trạng thái bài đăng không thể mâu thuẫn với hợp đồng của phòng.");
                }
                if (input.getFloorNo() != null && input.getFloorNo() > Short.MAX_VALUE) {
                    throw new IllegalArgumentException("Số tầng vượt quá giới hạn.");
                }
                roomRepository.findByRoomCode(input.getRoomCode().trim()).ifPresent(existing -> {
                    if (!existing.getRoomId().equals(managedRoom.getRoomId())) {
                        throw new IllegalArgumentException("Mã phòng đã tồn tại trong quản lý phòng.");
                    }
                });
                managedRoom.setRoomCode(input.getRoomCode().trim());
                managedRoom.setFloorNo(input.getFloorNo() == null ? null : input.getFloorNo().shortValue());
                managedRoom.setPriceOverride(input.getPrice());
                managedRoom.setStatus(mappedStatus);
                roomRepository.save(managedRoom);
            }
            room.setRoomCode(input.getRoomCode());
            room.setTitle(input.getTitle());
            room.setDescription(input.getDescription());
            room.setFloorNo(input.getFloorNo());
            room.setArea(input.getArea());
            room.setPrice(input.getPrice());
            room.setStatus(input.getStatus());
            room.setAmenities(input.getAmenities());
            room.setImageUrl(input.getImageUrl());
            room.setImageGallery(input.getImageGallery());
            room.setCurrentTenant(input.getCurrentTenant());
            room.setTenantPhone(input.getTenantPhone());
            room.setLeaseStartDate(input.getLeaseStartDate());
            room.setLeaseEndDate(input.getLeaseEndDate());
        }
        if (ownerId == null) {
            room.setCreatedBy(null);
        } else {
            User landlord = userRepository.findById(ownerId)
                    .filter(candidate -> candidate.getRole() != null
                            && Role.LANDLORD.equalsIgnoreCase(candidate.getRole().getRoleName())
                            && candidate.isApproved())
                    .orElseThrow(() -> new IllegalArgumentException("Chủ trọ được chọn không tồn tại hoặc chưa được duyệt."));
            room.setCreatedBy(landlord);
        }
        return roomListingService.save(room);
    }

    public void deleteRoomListing(Integer listingId) {
        RoomListing room = roomListingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));
        if (room.getManagedRoom() != null) {
            throw new IllegalStateException("Bài đăng này đã liên kết với phòng quản lý; hãy xóa phòng qua mục Quản lý phòng trọ.");
        }
        if ("DA_THUE".equalsIgnoreCase(room.getStatus())) {
            throw new IllegalStateException("Không thể xóa phòng đang được thuê; hãy giữ lại lịch sử.");
        }
        if (bookingRequestRepository.existsByRoom_ListingId(listingId)) {
            throw new IllegalStateException("Không thể xóa phòng đã có yêu cầu thuê liên quan; hãy giữ lại lịch sử.");
        }
        roomListingService.delete(listingId);
    }

    private User getManagedUser(Integer userId, String expectedRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (user.getRole() == null || !expectedRole.equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Tài khoản không thuộc danh mục quản lý đã chọn.");
        }
        return user;
    }

    private void expireSessions(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String registeredUsername = principal instanceof UserDetails details
                    ? details.getUsername()
                    : principal instanceof String name ? name : null;
            if (registeredUsername != null && registeredUsername.equalsIgnoreCase(username)) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }

    private static boolean isPendingApproval(String status) {
        return "PENDING_APPROVAL".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status);
    }

    private User approvedLandlord(Integer userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getRole() != null
                        && Role.LANDLORD.equalsIgnoreCase(user.getRole().getRoleName())
                        && user.isApproved())
                .orElseThrow(() -> new IllegalArgumentException("Chủ trọ không tồn tại hoặc chưa được duyệt."));
    }

    private static String normalizeManagedRoomStatus(String status) {
        if (status == null) return "";
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TRỐNG" -> "TRONG";
            case "ĐANG THUÊ" -> "DA_THUE";
            case "BẢO TRÌ" -> "BAO_TRI";
            default -> normalized;
        };
    }

    private static String normalizeManagedRole(String roleName) {
        if (roleName == null) {
            throw new IllegalArgumentException("Danh mục tài khoản không hợp lệ.");
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        if (!MANAGED_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("Chỉ được quản lý tài khoản Chủ trọ hoặc Người thuê.");
        }
        return normalized;
    }

    private static void validateAccount(AdminAccountForm form) {
        if (form.getFullName() == null || form.getFullName().isBlank() || form.getFullName().trim().length() > 150) {
            throw new IllegalArgumentException("Họ tên là bắt buộc và tối đa 150 ký tự.");
        }
        if (form.getEmail() == null || !form.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || form.getEmail().trim().length() > 100) {
            throw new IllegalArgumentException("Email không hợp lệ (tối đa 100 ký tự).");
        }
        if (form.getPhone() == null || !form.getPhone().matches("^(0|\\+84)[0-9]{9,10}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (form.getUserId() == null) {
            if (form.getUsername() == null || !form.getUsername().matches("^[a-zA-Z0-9_.]{4,50}$")) {
                throw new IllegalArgumentException("Tên đăng nhập phải từ 4 đến 50 ký tự hợp lệ.");
            }
            if (form.getPassword() == null || form.getPassword().length() < 8) {
                throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự.");
            }
        }
    }
}
