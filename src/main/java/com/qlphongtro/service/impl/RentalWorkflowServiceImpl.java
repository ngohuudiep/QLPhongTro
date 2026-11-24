package com.qlphongtro.service.impl;

import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.service.RentalWorkflowService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RentalWorkflowServiceImpl implements RentalWorkflowService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoomListingRepository roomListingRepository;
    private final RoomRepository roomRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final ContractRepository contractRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final PaymentRepository paymentRepository;
    private final DepositConfirmationRequestRepository depositConfirmationRequestRepository;
    private final UserNotificationRepository notificationRepository;

    public RentalWorkflowServiceImpl(UserRepository userRepository,
                                     TenantRepository tenantRepository,
                                     RoomListingRepository roomListingRepository,
                                     RoomRepository roomRepository,
                                     BookingRequestRepository bookingRequestRepository,
                                     ContractRepository contractRepository,
                                     ContractTenantRepository contractTenantRepository,
                                     PaymentRepository paymentRepository,
                                     DepositConfirmationRequestRepository depositConfirmationRequestRepository,
                                     UserNotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roomListingRepository = roomListingRepository;
        this.roomRepository = roomRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.contractRepository = contractRepository;
        this.contractTenantRepository = contractTenantRepository;
        this.paymentRepository = paymentRepository;
        this.depositConfirmationRequestRepository = depositConfirmationRequestRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public BookingRequest createRequest(String username, Integer listingId, String fullName, String phone,
                                        String idNumber, Short expectedOccupants,
                                        LocalDate expectedMoveInDate, String note) {
        User tenantUser = getEnabledTenant(username);
        if (bookingRequestRepository.existsByUser_UserIdAndRoom_ListingIdAndStatusIn(
                tenantUser.getUserId(), listingId,
                List.of("PENDING_LANDLORD_REVIEW", "APPROVED_BY_LANDLORD", "CONTRACT_SENT"))) {
            throw new IllegalStateException("Bạn đã gửi yêu cầu thuê phòng này và yêu cầu vẫn đang được xử lý.");
        }
        RoomListing listingReference = roomListingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));
        if (listingReference.getManagedRoom() == null) {
            throw new IllegalStateException("Phòng chưa được liên kết chủ trọ hợp lệ.");
        }
        Room managedRoom = roomRepository.findByIdForUpdate(listingReference.getManagedRoom().getRoomId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phòng được liên kết."));
        RoomListing listing = roomListingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));
        if (!"TRONG".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalStateException("Phòng không còn ở trạng thái có thể đăng ký.");
        }
        if (listing.getManagedRoom() == null || !listing.getManagedRoom().getRoomId().equals(managedRoom.getRoomId())
                || managedRoom.getBuilding() == null || managedRoom.getBuilding().getOwner() == null) {
            throw new IllegalStateException("Phòng chưa được liên kết chủ trọ hợp lệ.");
        }
        if (!"TRONG".equalsIgnoreCase(managedRoom.getStatus())) {
            throw new IllegalStateException("Phòng không còn trống.");
        }
        Short maxOccupants = managedRoom.getMaxOccupants();
        if (expectedOccupants == null || expectedOccupants < 1
                || (maxOccupants != null && expectedOccupants > maxOccupants)) {
            throw new IllegalArgumentException("Số người dự kiến không hợp lệ.");
        }
        if (expectedMoveInDate == null || expectedMoveInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày dự kiến nhận phòng không hợp lệ.");
        }
        if (fullName == null || fullName.isBlank() || phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Họ tên và số điện thoại là bắt buộc.");
        }
        BookingRequest request = new BookingRequest(tenantUser, listing);
        request.setStatus("PENDING_LANDLORD_REVIEW");
        request.setFullName(fullName.trim());
        request.setPhone(phone.trim());
        request.setIdNumber(idNumber != null ? idNumber.trim() : null);
        request.setExpectedOccupants(expectedOccupants);
        request.setExpectedMoveInDate(expectedMoveInDate);
        request.setNote(note != null ? note.trim() : null);
        request.setExpiresAt(LocalDateTime.now().plusDays(7));
        BookingRequest saved = bookingRequestRepository.save(request);

        listing.setStatus("RESERVED");
        roomListingRepository.save(listing);
        managedRoom.setStatus("RESERVED");
        roomRepository.save(managedRoom);

        User owner = managedRoom.getBuilding().getOwner();
        notify(owner, UserNotification.TYPE_RENTAL_REQUEST,
                "Yêu cầu thuê phòng mới",
                "Người thuê " + fullName.trim() + " đã gửi yêu cầu thuê phòng " + listing.getRoomCode()
                        + ". Ngày nhận phòng dự kiến: " + expectedMoveInDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + "; số người ở: " + expectedOccupants + ".",
                null, saved);
        return saved;
    }

    @Override
    public BookingRequest createRequest(String username, Integer listingId, Short expectedOccupants,
                                        LocalDate expectedMoveInDate, String note) {
        User tenantUser = getEnabledTenant(username);
        Tenant tenantProfile = tenantRepository.findByUser_UserId(tenantUser.getUserId()).orElse(null);
        String fullName = tenantProfile != null && tenantProfile.getFullName() != null
                ? tenantProfile.getFullName() : tenantUser.getFullName();
        String phone = tenantProfile != null && tenantProfile.getPhone() != null
                ? tenantProfile.getPhone() : tenantUser.getPhone();
        String idNumber = tenantProfile != null ? tenantProfile.getIdNumber() : null;
        if (fullName == null || fullName.isBlank()) {
            fullName = tenantUser.getUsername();
        }
        return createRequest(username, listingId, fullName, phone, idNumber,
                expectedOccupants, expectedMoveInDate, note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingRequest> getTenantRequests(String username) {
        User tenant = getEnabledTenant(username);
        return bookingRequestRepository.findByUser_UserIdOrderByRequestDateDesc(tenant.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingRequest> getLandlordRequests(String username) {
        User landlord = getLandlord(username);
        return bookingRequestRepository.findByRoom_ManagedRoom_Building_Owner_UsernameOrderByRequestDateDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingRequest getLandlordRequest(Integer requestId, String username) {
        return getOwnedRequest(requestId, username);
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getLandlordContractForRequest(Integer requestId, String username) {
        getOwnedRequest(requestId, username);
        Contract contract = contractRepository.findByRentalRequest_RequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu này chưa có hợp đồng."));
        return getOwnedContract(contract.getContractId(), username);
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getLandlordDraftForRequest(Integer requestId, String username) {
        Contract owned = getLandlordContractForRequest(requestId, username);
        if (!List.of("DRAFT", "TENANT_REJECTED").contains(owned.getStatus())) {
            throw new IllegalStateException("Hợp đồng này không còn ở trạng thái có thể chỉnh sửa.");
        }
        return owned;
    }

    @Override
    public BookingRequest approveRequest(Integer requestId, String username) {
        User landlord = getLandlord(username);
        BookingRequest request = bookingRequestRepository.findByIdForUpdate(requestId)
                .filter(item -> item.getRoom() != null
                        && item.getRoom().getManagedRoom() != null
                        && item.getRoom().getManagedRoom().getBuilding() != null
                        && item.getRoom().getManagedRoom().getBuilding().getOwner() != null
                        && item.getRoom().getManagedRoom().getBuilding().getOwner().getUserId()
                        .equals(landlord.getUserId()))
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Bạn không có quyền xử lý yêu cầu thuê này."));
        requireStatus(request, "PENDING_LANDLORD_REVIEW");

        Room room = roomRepository.findByIdForUpdate(request.getRoom().getManagedRoom().getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng được liên kết."));
        RoomListing listing = roomListingRepository.findByIdForUpdate(request.getRoom().getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng của yêu cầu."));
        if (listing.getManagedRoom() == null
                || !listing.getManagedRoom().getRoomId().equals(room.getRoomId())
                || room.getBuilding() == null || room.getBuilding().getOwner() == null
                || !landlord.getUserId().equals(room.getBuilding().getOwner().getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền xử lý phòng này.");
        }
        if (!List.of("TRONG", "RESERVED").contains(listing.getStatus())
                || !List.of("TRONG", "RESERVED").contains(room.getStatus())) {
            throw new IllegalStateException("Phòng không còn trống để duyệt yêu cầu.");
        }
        if (bookingRequestRepository.existsByRoom_ListingIdAndStatusInAndRequestIdNot(
                listing.getListingId(),
                List.of("APPROVED_BY_LANDLORD", "CONTRACT_SENT"),
                requestId)) {
            throw new IllegalStateException("Phòng đã được giữ cho một yêu cầu khác.");
        }
        if (contractRepository.existsByRoom_RoomIdAndStatusInAndContractIdNot(
                room.getRoomId(), activeContractStatuses(), -1)) {
            throw new IllegalStateException("Phòng đã có hợp đồng đang được xử lý hoặc có hiệu lực.");
        }

        request.setStatus("APPROVED_BY_LANDLORD");
        request.setProcessedBy(landlord);
        request.setProcessedDate(LocalDateTime.now());
        BookingRequest saved = bookingRequestRepository.save(request);

        Contract draft = new Contract();
        draft.setContractCode("HD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + request.getRequestId());
        draft.setRoom(room);
        draft.setRepresentative(tenantRepository.findByUser_UserId(request.getUser().getUserId()).orElse(null));
        draft.setTenantUser(request.getUser());
        draft.setRentalRequest(saved);
        LocalDate moveIn = request.getExpectedMoveInDate() != null ? request.getExpectedMoveInDate() : LocalDate.now();
        draft.setStartDate(moveIn);
        draft.setContractDate(LocalDate.now());
        draft.setMonthlyRent(room.getPriceOverride() != null ? room.getPriceOverride() : listing.getPrice());
        draft.setContractType("ONLINE_RENTAL");
        draft.setStatus("DRAFT");
        contractRepository.save(draft);

        notify(request.getUser(), UserNotification.TYPE_RENTAL_REQUEST,
                "Yêu cầu thuê đã được duyệt",
                "Chủ trọ đã duyệt yêu cầu thuê phòng " + listing.getRoomCode()
                        + " và đang chuẩn bị hợp đồng cho bạn.",
                null, saved);
        return saved;
    }

    @Override
    public BookingRequest rejectRequest(Integer requestId, String reason, String username) {
        User landlord = getLandlord(username);
        BookingRequest request = bookingRequestRepository.findByIdForUpdate(requestId)
                .filter(item -> item.getRoom() != null
                        && item.getRoom().getManagedRoom() != null
                        && item.getRoom().getManagedRoom().getBuilding() != null
                        && item.getRoom().getManagedRoom().getBuilding().getOwner() != null
                        && item.getRoom().getManagedRoom().getBuilding().getOwner().getUserId()
                        .equals(landlord.getUserId()))
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Bạn không có quyền xử lý yêu cầu thuê này."));
        requireStatus(request, "PENDING_LANDLORD_REVIEW");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        }
        RoomListing listing = roomListingRepository.findByIdForUpdate(request.getRoom().getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng của yêu cầu."));
        if (!"RESERVED".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalStateException("Phòng không còn được giữ cho yêu cầu này.");
        }
        request.setStatus("REJECTED_BY_LANDLORD");
        request.setNote(reason.trim());
        request.setProcessedBy(landlord);
        request.setProcessedDate(LocalDateTime.now());
        releaseReservation(request);
        BookingRequest saved = bookingRequestRepository.save(request);
        notify(request.getUser(), UserNotification.TYPE_RENTAL_REQUEST,
                "Yêu cầu thuê bị từ chối",
                "Chủ trọ đã từ chối yêu cầu thuê phòng " + request.getRoom().getRoomCode()
                        + ". Lý do: " + reason.trim(), null, saved);
        return saved;
    }

    @Override
    public Contract createAndSendContract(Integer requestId, Contract details, String username) {
        BookingRequest request = getOwnedRequest(requestId, username);
        if (!List.of("APPROVED_BY_LANDLORD", "CONTRACT_SENT").contains(request.getStatus())) {
            throw new IllegalStateException("Yêu cầu chưa được duyệt hoặc không thể gửi hợp đồng.");
        }
        Contract contract = contractRepository.findByRentalRequest_RequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy bản nháp hợp đồng."));
        getOwnedContract(contract.getContractId(), username);
        if (!List.of("DRAFT", "TENANT_REJECTED").contains(contract.getStatus())) {
            throw new IllegalStateException("Hợp đồng này không ở trạng thái có thể gửi.");
        }
        RoomListing listing = roomListingRepository.findByIdForUpdate(request.getRoom().getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng của yêu cầu."));
        if (!"RESERVED".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalStateException("Phòng không còn được giữ cho yêu cầu này.");
        }
        if (details.getStartDate() == null || details.getEndDate() == null
                || details.getEndDate().isBefore(details.getStartDate())
                || (request.getExpectedMoveInDate() != null
                    && details.getStartDate().isBefore(request.getExpectedMoveInDate()))) {
            throw new IllegalArgumentException("Thời hạn hợp đồng không hợp lệ.");
        }
        BigDecimal monthlyRent = details.getMonthlyRent() != null
                ? details.getMonthlyRent() : contract.getMonthlyRent();
        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tiền thuê tháng phải lớn hơn 0.");
        }
        if (details.getDepositAmount() != null
                && details.getDepositAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền đặt cọc không hợp lệ.");
        }
        if (contractRepository.existsByRoom_RoomIdAndStatusInAndContractIdNot(
                contract.getRoom().getRoomId(), activeContractStatuses(), contract.getContractId())) {
            throw new IllegalStateException("Phòng đã có hợp đồng đang được xử lý hoặc có hiệu lực.");
        }

        contract.setStartDate(details.getStartDate());
        contract.setEndDate(details.getEndDate());
        contract.setMonthlyRent(monthlyRent);
        contract.setDepositAmount(details.getDepositAmount());
        contract.setBillingCycleMonths(details.getBillingCycleMonths());
        contract.setPaymentDay(details.getPaymentDay());
        contract.setMonthlyElectricPrice(details.getMonthlyElectricPrice());
        contract.setMonthlyWaterPrice(details.getMonthlyWaterPrice());
        contract.setInternetFee(details.getInternetFee());
        contract.setServiceFee(details.getServiceFee());
        contract.setSanitationFee(details.getSanitationFee());
        contract.setOtherServiceFee(details.getOtherServiceFee());
        contract.setPaymentMethod(details.getPaymentMethod());
        contract.setPaymentTerms(details.getPaymentTerms());
        contract.setDepositTerms(details.getDepositTerms());
        contract.setUtilityTerms(details.getUtilityTerms());
        contract.setAssetTerms(details.getAssetTerms());
        contract.setRepairTerms(details.getRepairTerms());
        contract.setTerminationTerms(details.getTerminationTerms());
        contract.setOccupantTerms(details.getOccupantTerms());
        contract.setRenewalTerms(details.getRenewalTerms());
        contract.setLandlordResponsibilities(details.getLandlordResponsibilities());
        contract.setTenantResponsibilities(details.getTenantResponsibilities());
        contract.setAdditionalTerms(details.getAdditionalTerms());
        contract.setStatus("SENT_TO_TENANT");
        contract.setTenantAcceptedAt(null);
        contract.setTenantRejectionReason(null);
        Contract saved = contractRepository.save(contract);

        request.setStatus("CONTRACT_SENT");
        bookingRequestRepository.save(request);
        notify(request.getUser(), UserNotification.TYPE_CONTRACT,
                "Hợp đồng thuê phòng",
                "Chủ trọ đã gửi hợp đồng " + saved.getContractCode()
                        + " để bạn xem và xác nhận.", saved, request);
        return saved;
    }

    @Override
    public Contract tenantAcceptContract(Integer contractId, String username) {
        Contract contract = getTenantContract(contractId, username);
        if (!"SENT_TO_TENANT".equalsIgnoreCase(contract.getStatus())
                && !"PENDING_TENANT_CONFIRMATION".equalsIgnoreCase(contract.getStatus())) {
            throw new IllegalStateException("Hợp đồng không còn ở trạng thái chờ người thuê xác nhận.");
        }
        BookingRequest request = contract.getRentalRequest();
        if (request == null || !"CONTRACT_SENT".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Yêu cầu thuê không còn liên kết với hợp đồng đang gửi.");
        }
        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng của hợp đồng."));
        RoomListing listing = roomListingRepository.findByIdForUpdate(request.getRoom().getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin đăng của phòng."));
        if (!"RESERVED".equalsIgnoreCase(room.getStatus())
                || !"RESERVED".equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalStateException("Phòng không còn được giữ cho đúng yêu cầu thuê này.");
        }
        if (contractRepository.existsByRoom_RoomIdAndStatusInAndContractIdNot(
                room.getRoomId(), activeContractStatuses(), contractId)) {
            throw new IllegalStateException("Phòng đã có hợp đồng khác được duyệt hoặc đang hiệu lực.");
        }

        contract.setStatus("TENANT_ACCEPTED");
        contract.setTenantAcceptedAt(LocalDateTime.now());
        contract.setTenantRejectionReason(null);
        Contract saved = contractRepository.save(contract);

        room.setStatus("OCCUPIED");
        roomRepository.save(room);
        listing.setStatus("DA_THUE");
        listing.setCurrentTenant(request.getFullName() != null
                ? request.getFullName() : contract.getTenantUser().getFullName());
        listing.setTenantPhone(request.getPhone() != null
                ? request.getPhone() : contract.getTenantUser().getPhone());
        listing.setLeaseStartDate(contract.getStartDate());
        listing.setLeaseEndDate(contract.getEndDate());
        roomListingRepository.save(listing);

        request.setStatus("COMPLETED");
        bookingRequestRepository.save(request);
        if (contract.getRepresentative() != null
                && !contractTenantRepository.existsById_ContractId(contractId)) {
            ContractTenant relation = new ContractTenant();
            relation.setId(new ContractTenantId(contractId, contract.getRepresentative().getTenantId()));
            relation.setContract(contract);
            relation.setTenant(contract.getRepresentative());
            relation.setMoveInDate(contract.getStartDate());
            contractTenantRepository.save(relation);
        }
        notify(contract.getRoom().getBuilding().getOwner(), UserNotification.TYPE_CONTRACT,
                "Người thuê đã đồng ý hợp đồng",
                "Người thuê đã đồng ý hợp đồng " + contract.getContractCode()
                        + ". Phòng " + room.getRoomCode() + " đã được cập nhật là đang thuê.",
                saved, request);
        return saved;
    }

    @Override
    public Contract tenantRejectContract(Integer contractId, String reason, String username) {
        Contract contract = getTenantContract(contractId, username);
        if (!"SENT_TO_TENANT".equalsIgnoreCase(contract.getStatus())
                && !"PENDING_TENANT_CONFIRMATION".equalsIgnoreCase(contract.getStatus())) {
            throw new IllegalStateException("Hợp đồng không còn ở trạng thái chờ người thuê xác nhận.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối hợp đồng.");
        }
        contract.setStatus("TENANT_REJECTED");
        contract.setTenantRejectionReason(reason.trim());
        if (contract.getRentalRequest() != null) {
            contract.getRentalRequest().setStatus("APPROVED_BY_LANDLORD");
            bookingRequestRepository.save(contract.getRentalRequest());
        }
        Contract saved = contractRepository.save(contract);
        notify(contract.getRoom().getBuilding().getOwner(), UserNotification.TYPE_CONTRACT,
                "Người thuê từ chối hợp đồng",
                "Người thuê đã từ chối hợp đồng " + contract.getContractCode()
                        + ". Lý do: " + reason.trim(), saved, contract.getRentalRequest());
        return saved;
    }

    @Override
    public DepositConfirmationRequest createDepositConfirmationRequest(Integer contractId, String username) {
        getTenantContract(contractId, username);
        Contract contract = contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hợp đồng."));
        getTenantContract(contractId, username);
        if (!List.of("TENANT_ACCEPTED", "ACTIVE").contains(
                contract.getStatus() == null ? "" : contract.getStatus().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalStateException("Hợp đồng chưa được người thuê chấp thuận hoặc không thể xác nhận tiền cọc.");
        }
        if (contract.getDepositAmount() == null || contract.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Hợp đồng không yêu cầu tiền cọc hoặc chưa cấu hình số tiền.");
        }
        if (paymentRepository.existsDepositPaymentByContractIdAndStatus(contractId, "SUCCESS")) {
            throw new IllegalStateException("Tiền cọc của hợp đồng này đã được xác nhận.");
        }
        if (paymentRepository.existsUnresolvedDepositPaymentByContractId(contractId)) {
            throw new IllegalStateException("Đã có giao dịch tiền cọc đang được xử lý.");
        }
        if (depositConfirmationRequestRepository.existsByContract_ContractIdAndStatusIgnoreCase(
                contractId, DepositConfirmationRequest.STATUS_PENDING)) {
            throw new IllegalStateException("Đã có yêu cầu xác nhận tiền cọc đang được xử lý.");
        }
        User tenant = getEnabledTenant(username);
        User landlord = contract.getRoom() != null && contract.getRoom().getBuilding() != null
                ? contract.getRoom().getBuilding().getOwner() : null;
        if (landlord == null || !landlord.isApproved() || !landlord.isEnabled()
                || landlord.getRole() == null || !Role.LANDLORD.equalsIgnoreCase(landlord.getRole().getRoleName())) {
            throw new IllegalStateException("Không xác định được chủ trọ hợp lệ của hợp đồng.");
        }

        DepositConfirmationRequest request = new DepositConfirmationRequest();
        request.setContract(contract);
        request.setTenantUser(tenant);
        request.setLandlordUser(landlord);
        request.setAmount(contract.getDepositAmount());
        request.setStatus(DepositConfirmationRequest.STATUS_PENDING);
        request.setRequestedAt(LocalDateTime.now());
        DepositConfirmationRequest saved = depositConfirmationRequestRepository.saveAndFlush(request);
        notify(landlord, UserNotification.TYPE_DEPOSIT,
                "Người thuê gửi yêu cầu xác nhận tiền cọc",
                "Người thuê đã gửi yêu cầu xác nhận tiền cọc cho hợp đồng "
                        + contract.getContractCode() + " (phòng " + contract.getRoom().getRoomCode()
                        + "), số tiền " + request.getAmount().toPlainString()
                        + " ₫. Hãy đối chiếu giao dịch thực tế trước khi xác nhận.",
                contract, contract.getRentalRequest());
        return saved;
    }

    @Override
    public List<DepositConfirmationRequest> getTenantDepositRequests(Integer contractId, String username) {
        Contract contract = getTenantContract(contractId, username);
        User tenant = getEnabledTenant(username);
        return depositConfirmationRequestRepository
                .findByContract_ContractIdAndTenantUser_UserIdOrderByRequestedAtDesc(
                        contract.getContractId(), tenant.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public DepositConfirmationRequest getLandlordLatestDepositRequest(Integer contractId, String username) {
        getOwnedContract(contractId, username);
        User landlord = getLandlord(username);
        return depositConfirmationRequestRepository
                .findTopByContract_ContractIdAndLandlordUser_UserIdOrderByRequestedAtDesc(
                        contractId, landlord.getUserId())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositConfirmationRequest> getLandlordDepositRequests(String username, String status) {
        User landlord = getLandlord(username);
        String normalizedStatus = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null : status.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedStatus != null && !List.of(
                DepositConfirmationRequest.STATUS_PENDING,
                DepositConfirmationRequest.STATUS_APPROVED,
                DepositConfirmationRequest.STATUS_REJECTED).contains(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái yêu cầu xác nhận tiền cọc không hợp lệ.");
        }
        return normalizedStatus == null
                ? depositConfirmationRequestRepository.findByLandlordUser_UserIdOrderByRequestedAtDesc(
                        landlord.getUserId())
                : depositConfirmationRequestRepository
                        .findByLandlordUser_UserIdAndStatusIgnoreCaseOrderByRequestedAtDesc(
                                landlord.getUserId(), normalizedStatus);
    }

    @Override
    public DepositConfirmationRequest approveDepositConfirmationRequest(
            Integer requestId, String referenceNo, String username) {
        DepositConfirmationRequest request = getLandlordDepositRequest(requestId, username);
        Integer contractId = request.getContract().getContractId();
        Contract contract = contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hợp đồng."));
        request = getLandlordDepositRequest(requestId, username);
        String currentStatus = depositConfirmationRequestRepository.findStatusByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu xác nhận tiền cọc."));
        if (DepositConfirmationRequest.STATUS_APPROVED.equalsIgnoreCase(currentStatus)) {
            return request;
        }
        if (!DepositConfirmationRequest.STATUS_PENDING.equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Yêu cầu tiền cọc không còn ở trạng thái chờ xác nhận.");
        }
        if (!List.of("TENANT_ACCEPTED", "ACTIVE").contains(
                contract.getStatus() == null ? "" : contract.getStatus().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalStateException("Hợp đồng không còn ở trạng thái có thể xác nhận tiền cọc.");
        }
        if (contract.getDepositAmount() == null
                || contract.getDepositAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalStateException("Số tiền cọc trong hợp đồng đã thay đổi; không thể xác nhận yêu cầu cũ.");
        }
        if (referenceNo != null && referenceNo.trim().length() > 50) {
            throw new IllegalArgumentException("Mã tham chiếu không được vượt quá 50 ký tự.");
        }
        if (paymentRepository.existsDepositPaymentByContractIdAndStatus(contractId, "SUCCESS")) {
            throw new IllegalStateException("Tiền cọc của hợp đồng này đã được xác nhận.");
        }

        LocalDateTime confirmedAt = LocalDateTime.now();
        Payment payment = new Payment();
        payment.setContract(contract);
        payment.setInvoice(null);
        payment.setAmount(request.getAmount());
        payment.setMethod("MANUAL_TRANSFER");
        payment.setProvider("MANUAL");
        payment.setStatus("SUCCESS");
        payment.setTransactionCode("DEP-" + UUID.randomUUID());
        payment.setReferenceNo(referenceNo == null || referenceNo.isBlank() ? null : referenceNo.trim());
        payment.setCreatedAt(request.getRequestedAt());
        payment.setProcessedAt(confirmedAt);
        payment.setPaidAt(confirmedAt);
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        User landlord = getLandlord(username);
        request.setPayment(savedPayment);
        request.setStatus(DepositConfirmationRequest.STATUS_APPROVED);
        request.setRespondedAt(confirmedAt);
        request.setRespondedBy(landlord);
        request.setResponseReference(payment.getReferenceNo());
        contract.setStatus("ACTIVE");
        contract.setLandlordApprovedAt(confirmedAt);
        contractRepository.save(contract);
        DepositConfirmationRequest savedRequest = depositConfirmationRequestRepository.save(request);
        notify(request.getTenantUser(), UserNotification.TYPE_DEPOSIT,
                "Tiền cọc đã được xác nhận",
                "Chủ trọ đã xác nhận nhận tiền cọc cho hợp đồng " + contract.getContractCode()
                        + ". Trạng thái tiền cọc hiện là đã thanh toán.",
                contract, contract.getRentalRequest());
        return savedRequest;
    }

    @Override
    public DepositConfirmationRequest rejectDepositConfirmationRequest(
            Integer requestId, String reason, String username) {
        DepositConfirmationRequest request = getLandlordDepositRequest(requestId, username);
        Integer contractId = request.getContract().getContractId();
        Contract contract = contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hợp đồng."));
        request = getLandlordDepositRequest(requestId, username);
        String currentStatus = depositConfirmationRequestRepository.findStatusByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu xác nhận tiền cọc."));
        if (!DepositConfirmationRequest.STATUS_PENDING.equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Yêu cầu tiền cọc không còn ở trạng thái chờ xác nhận.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối yêu cầu tiền cọc.");
        }
        if (reason.trim().length() > 4000) {
            throw new IllegalArgumentException("Lý do từ chối không được vượt quá 4000 ký tự.");
        }
        LocalDateTime respondedAt = LocalDateTime.now();
        request.setStatus(DepositConfirmationRequest.STATUS_REJECTED);
        request.setRejectionReason(reason.trim());
        request.setRespondedAt(respondedAt);
        request.setRespondedBy(getLandlord(username));
        DepositConfirmationRequest saved = depositConfirmationRequestRepository.save(request);
        notify(request.getTenantUser(), UserNotification.TYPE_DEPOSIT,
                "Yêu cầu xác nhận tiền cọc bị từ chối",
                "Chủ trọ đã từ chối yêu cầu xác nhận tiền cọc cho hợp đồng " + contract.getContractCode()
                        + ". Lý do: " + reason.trim()
                        + ". Tiền cọc vẫn chưa được ghi nhận là đã thanh toán.",
                contract, contract.getRentalRequest());
        return saved;
    }

    @Override
    public Contract landlordApproveContract(Integer contractId, String username) {
        Contract contract = getOwnedContract(contractId, username);
        requireStatus(contract, "PENDING_FINAL_LANDLORD_APPROVAL");
        if (contract.getTenantAcceptedAt() == null) {
            throw new IllegalStateException("Người thuê chưa đồng ý hợp đồng.");
        }
        if (contract.getDepositAmount() != null && contract.getDepositAmount().compareTo(BigDecimal.ZERO) > 0
                && !paymentRepository.existsDepositPaymentByContractIdAndStatus(contractId, "SUCCESS")) {
            throw new IllegalStateException("Chưa xác nhận thanh toán tiền cọc thành công.");
        }
        contract.setStatus("ACTIVE");
        contract.setLandlordApprovedAt(LocalDateTime.now());
        Contract saved = contractRepository.save(contract);
        if (contract.getRentalRequest() != null) {
            contract.getRentalRequest().setStatus("COMPLETED");
            bookingRequestRepository.save(contract.getRentalRequest());
        }
        Room room = contract.getRoom();
        room.setStatus("OCCUPIED");
        roomRepository.save(room);
        RoomListing listing = roomListingRepository.findByManagedRoom_RoomId(room.getRoomId())
                .orElse(null);
        if (listing != null) {
            listing.setStatus("DA_THUE");
            listing.setCurrentTenant(contract.getRepresentative() != null
                    ? contract.getRepresentative().getFullName() : null);
            listing.setTenantPhone(contract.getRepresentative() != null
                    ? contract.getRepresentative().getPhone() : null);
            listing.setLeaseStartDate(contract.getStartDate());
            listing.setLeaseEndDate(contract.getEndDate());
            roomListingRepository.save(listing);
        }
        notify(contract.getTenantUser(), UserNotification.TYPE_CONTRACT,
                "Hợp đồng đã được kích hoạt",
                "Chủ trọ đã phê duyệt hợp đồng " + contract.getContractCode()
                        + ". Phòng " + room.getRoomCode() + " đã chính thức được ghi nhận cho bạn.",
                saved, contract.getRentalRequest());
        return saved;
    }

    @Override
    public Contract landlordRejectContract(Integer contractId, String reason, String username) {
        Contract contract = getOwnedContract(contractId, username);
        requireStatus(contract, "PENDING_FINAL_LANDLORD_APPROVAL");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối phê duyệt.");
        }
        contract.setStatus("REJECTED_BY_LANDLORD");
        contract.setLandlordRejectionReason(reason.trim());
        if (contract.getRentalRequest() != null) {
            contract.getRentalRequest().setStatus("CANCELLED");
            bookingRequestRepository.save(contract.getRentalRequest());
            releaseReservation(contract.getRentalRequest());
        }
        Contract saved = contractRepository.save(contract);
        notify(contract.getTenantUser(), UserNotification.TYPE_CONTRACT,
                "Hợp đồng chưa được phê duyệt",
                "Chủ trọ đã từ chối phê duyệt hợp đồng " + contract.getContractCode()
                        + ". Lý do: " + reason.trim(), saved, contract.getRentalRequest());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getTenantContract(Integer contractId, String username) {
        User tenant = getEnabledTenant(username);
        Tenant tenantProfile = tenantRepository.findByUser_UserId(tenant.getUserId()).orElse(null);
        return contractRepository.findById(contractId)
                .filter(contract -> !"DRAFT".equalsIgnoreCase(contract.getStatus()))
                .filter(contract -> (contract.getTenantUser() != null
                                && contract.getTenantUser().getUserId().equals(tenant.getUserId()))
                        || (tenantProfile != null && contract.getRepresentative() != null
                                && contract.getRepresentative().getTenantId().equals(tenantProfile.getTenantId())))
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem hợp đồng này."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contract> getTenantContracts(String username) {
        User tenant = getEnabledTenant(username);
        java.util.Map<Integer, Contract> contracts = new java.util.LinkedHashMap<>();
        contractRepository.findByTenantUser_UserIdOrderByContractIdDesc(tenant.getUserId())
                .forEach(contract -> contracts.put(contract.getContractId(), contract));
        tenantRepository.findByUser_UserId(tenant.getUserId()).ifPresent(profile -> {
            contractRepository.findByRepresentative_TenantId(profile.getTenantId())
                    .forEach(contract -> contracts.put(contract.getContractId(), contract));
            contractTenantRepository.findById_TenantId(profile.getTenantId()).stream()
                    .map(ContractTenant::getContract)
                    .filter(java.util.Objects::nonNull)
                    .forEach(contract -> contracts.put(contract.getContractId(), contract));
        });
        return contracts.values().stream()
                .filter(contract -> !"DRAFT".equalsIgnoreCase(contract.getStatus()))
                .sorted(java.util.Comparator.comparing(Contract::getContractId).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getContractPayments(Integer contractId, String username) {
        getTenantContract(contractId, username);
        return paymentRepository.findDepositPaymentsByContractId(contractId);
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getLandlordContract(Integer contractId, String username) {
        return getOwnedContract(contractId, username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getLandlordContractPayments(Integer contractId, String username) {
        getOwnedContract(contractId, username);
        return paymentRepository.findDepositPaymentsByContractId(contractId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserNotification> getLandlordNotifications(String username) {
        User landlord = getLandlord(username);
        return notificationRepository.findByRecipient_UserIdOrderByCreatedAtDesc(landlord.getUserId());
    }

    @Override
    public String openLandlordNotification(Integer notificationId, String username) {
        User landlord = getLandlord(username);
        UserNotification notification = notificationRepository
                .findByNotificationIdAndRecipient_UserId(notificationId, landlord.getUserId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Không tìm thấy thông báo thuộc tài khoản này."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        if (notification.getTerminationRequest() != null) {
            ContractTerminationRequest terminationRequest = notification.getTerminationRequest();
            getOwnedContract(terminationRequest.getContract().getContractId(), username);
            return "/landlord/terminations/" + terminationRequest.getTerminationRequestId();
        }
        if (notification.getRentalRequest() != null) {
            BookingRequest request = getOwnedRequest(
                    notification.getRentalRequest().getRequestId(), username);
            return "/landlord/rental/requests/" + request.getRequestId();
        }
        if (notification.getContract() != null) {
            Contract contract = getOwnedContract(
                    notification.getContract().getContractId(), username);
            return "/landlord/rental/contracts/" + contract.getContractId();
        }
        if (UserNotification.TYPE_ACCOUNT_REVIEW.equals(notification.getNotificationType())) {
            return "/landlord/dashboard";
        }
        return "/landlord/notifications";
    }

    private User getEnabledTenant(String username) {
        return userRepository.findByUsername(username)
                .filter(User::isActiveTenant)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thuê không hợp lệ."));
    }

    private User getLandlord(String username) {
        return userRepository.findByUsername(username)
                .filter(User::isEnabled)
                .filter(User::isApproved)
                .filter(user -> user.getRole() != null
                        && Role.LANDLORD.equalsIgnoreCase(user.getRole().getRoleName()))
                .orElseThrow(() -> new AccessDeniedException("Tài khoản chủ trọ không hợp lệ."));
    }

    private BookingRequest getOwnedRequest(Integer requestId, String username) {
        User landlord = getLandlord(username);
        return bookingRequestRepository.findById(requestId)
                .filter(request -> request.getRoom() != null
                        && request.getRoom().getManagedRoom() != null
                        && request.getRoom().getManagedRoom().getBuilding() != null
                        && request.getRoom().getManagedRoom().getBuilding().getOwner() != null
                        && request.getRoom().getManagedRoom().getBuilding().getOwner().getUserId()
                        .equals(landlord.getUserId()))
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xử lý yêu cầu thuê này."));
    }

    private Contract getOwnedContract(Integer contractId, String username) {
        User landlord = getLandlord(username);
        return contractRepository.findById(contractId)
                .filter(contract -> contract.getRoom() != null
                        && contract.getRoom().getBuilding() != null
                        && contract.getRoom().getBuilding().getOwner() != null
                        && contract.getRoom().getBuilding().getOwner().getUserId().equals(landlord.getUserId()))
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xử lý hợp đồng này."));
    }

    private DepositConfirmationRequest getLandlordDepositRequest(Integer requestId, String username) {
        User landlord = getLandlord(username);
        DepositConfirmationRequest request = depositConfirmationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu xác nhận tiền cọc."));
        Contract ownedContract = getOwnedContract(request.getContract().getContractId(), username);
        if (request.getLandlordUser() == null
                || !request.getLandlordUser().getUserId().equals(landlord.getUserId())
                || !ownedContract.getContractId().equals(request.getContract().getContractId())) {
            throw new AccessDeniedException("Bạn không có quyền xử lý yêu cầu xác nhận tiền cọc này.");
        }
        return request;
    }

    private void releaseReservation(BookingRequest request) {
        if (request.getRoom() != null) {
            request.getRoom().setStatus("TRONG");
            roomListingRepository.save(request.getRoom());
            if (request.getRoom().getManagedRoom() != null) {
                request.getRoom().getManagedRoom().setStatus("TRONG");
                roomRepository.save(request.getRoom().getManagedRoom());
            }
        }
    }

    private void requireStatus(BookingRequest request, String status) {
        if (!status.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Yêu cầu không ở trạng thái có thể thực hiện thao tác.");
        }
    }

    private void requireStatus(Contract contract, String status) {
        if (!status.equalsIgnoreCase(contract.getStatus())) {
            throw new IllegalStateException("Hợp đồng không ở trạng thái có thể thực hiện thao tác.");
        }
    }

    private void notify(User recipient, String type, String title, String message,
                        Contract contract, BookingRequest request) {
        if (recipient == null) {
            throw new IllegalStateException("Không xác định được người nhận thông báo.");
        }
        UserNotification notification = new UserNotification();
        notification.setRecipient(recipient);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setContract(contract);
        notification.setRentalRequest(request);
        notificationRepository.save(notification);
    }

    private List<String> activeContractStatuses() {
        return List.of("DRAFT", "SENT_TO_TENANT", "PENDING_TENANT_CONFIRMATION",
                "PENDING_DEPOSIT", "PENDING_FINAL_LANDLORD_APPROVAL", "TENANT_ACCEPTED",
                "ACTIVE", "Hiệu lực");
    }
}
