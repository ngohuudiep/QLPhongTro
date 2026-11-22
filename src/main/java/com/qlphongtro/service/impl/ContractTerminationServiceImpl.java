package com.qlphongtro.service.impl;

import com.qlphongtro.dto.TerminationRequestForm;
import com.qlphongtro.dto.TerminationSettlementForm;
import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.service.ContractTerminationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class ContractTerminationServiceImpl implements ContractTerminationService {

    private static final List<String> ROOM_OCCUPYING_STATUSES =
            List.of("ACTIVE", "HIỆU LỰC", "ĐANG HIỆU LỰC", "TENANT_ACCEPTED", "TERMINATION_REQUESTED");
    private static final List<String> OPEN_REQUEST_STATUSES =
            List.of(ContractTerminationRequest.PENDING_RESPONSE,
                    ContractTerminationRequest.PENDING_SETTLEMENT,
                    ContractTerminationRequest.DISPUTED);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final ContractRepository contractRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final RoomRepository roomRepository;
    private final RoomListingRepository roomListingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final UserNotificationRepository notificationRepository;
    private final ContractTerminationRequestRepository requestRepository;
    private final ContractSettlementRepository settlementRepository;
    private final ContractTerminationHistoryRepository historyRepository;

    public ContractTerminationServiceImpl(UserRepository userRepository,
                                          TenantRepository tenantRepository,
                                          ContractRepository contractRepository,
                                          ContractTenantRepository contractTenantRepository,
                                          RoomRepository roomRepository,
                                          RoomListingRepository roomListingRepository,
                                          InvoiceRepository invoiceRepository,
                                          PaymentRepository paymentRepository,
                                          MeterReadingRepository meterReadingRepository,
                                          UserNotificationRepository notificationRepository,
                                          ContractTerminationRequestRepository requestRepository,
                                          ContractSettlementRepository settlementRepository,
                                          ContractTerminationHistoryRepository historyRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.contractRepository = contractRepository;
        this.contractTenantRepository = contractTenantRepository;
        this.roomRepository = roomRepository;
        this.roomListingRepository = roomListingRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.notificationRepository = notificationRepository;
        this.requestRepository = requestRepository;
        this.settlementRepository = settlementRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTerminationRequest> getLandlordRequests(String username) {
        requireLandlord(currentUser(username));
        return requestRepository.findForLandlord(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTerminationRequest> getTenantRequests(String username) {
        User user = requireTenant(currentUser(username));
        return requestRepository.findForParticipant(user.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTerminationRequest getLandlordRequest(Integer requestId, String username) {
        User user = requireLandlord(currentUser(username));
        ContractTerminationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu chấm dứt."));
        requireOwner(request.getContract(), user);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTerminationRequest getTenantRequest(Integer requestId, String username) {
        User user = requireTenant(currentUser(username));
        ContractTerminationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu chấm dứt."));
        requireTenantAccess(request.getContract(), user);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTerminationHistory> getHistory(Integer requestId, String username, boolean landlord) {
        if (landlord) {
            getLandlordRequest(requestId, username);
        } else {
            getTenantRequest(requestId, username);
        }
        return historyRepository.findByTerminationRequest_TerminationRequestIdOrderByHappenedAtAsc(requestId);
    }

    @Override
    public ContractTerminationRequest requestFromTenant(Integer contractId, TerminationRequestForm form, String username) {
        User user = requireTenant(currentUser(username));
        Contract contract = lockContract(contractId);
        requireTenantAccess(contract, user);
        return createRequest(contract, user, "TENANT", form);
    }

    @Override
    public ContractTerminationRequest requestFromLandlord(Integer contractId, TerminationRequestForm form, String username) {
        User user = requireLandlord(currentUser(username));
        Contract contract = lockContract(contractId);
        requireOwner(contract, user);
        return createRequest(contract, user, "LANDLORD", form);
    }

    @Override
    public ContractTerminationRequest respond(Integer requestId, boolean accept, String reason,
                                              String username, boolean landlord) {
        User actor = currentUser(username);
        if (landlord) {
            actor = requireLandlord(actor);
        } else {
            actor = requireTenant(actor);
        }
        ContractTerminationRequest request = lockRequest(requestId);
        Contract contract = lockContract(request.getContract().getContractId());
        authorizeRequestParticipant(request, contract, actor, landlord);
        if (!ContractTerminationRequest.PENDING_RESPONSE.equals(request.getStatus())) {
            throw new IllegalStateException("Yêu cầu này không còn chờ phản hồi.");
        }
        if (!request.getResponder().getUserId().equals(actor.getUserId())) {
            throw new AccessDeniedException("Chỉ bên nhận yêu cầu mới được phản hồi.");
        }
        if (!"TERMINATION_REQUESTED".equalsIgnoreCase(contract.getStatus())) {
            throw new IllegalStateException("Trạng thái hợp đồng đã thay đổi; không thể phản hồi yêu cầu này.");
        }
        LocalDateTime now = LocalDateTime.now();
        request.setRespondedAt(now);
        if (!accept) {
            if (reason == null || reason.isBlank() || reason.trim().length() > 1500) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối, tối đa 1500 ký tự.");
            }
            request.setStatus(ContractTerminationRequest.REJECTED);
            request.setResponseReason(reason.trim());
            contract.setStatus(request.getOriginalContractStatus());
            contractRepository.save(contract);
            requestRepository.save(request);
            record(request, actor, "REQUEST_REJECTED", ContractTerminationRequest.PENDING_RESPONSE,
                    ContractTerminationRequest.REJECTED, request.getResponseReason());
            notify(request.getRequester(), contract, request, "Yêu cầu chấm dứt bị từ chối",
                    actor.getFullName() + " đã từ chối yêu cầu chấm dứt hợp đồng " + contract.getContractCode()
                            + (request.getResponseReason() == null ? "." : ": " + request.getResponseReason()));
            return request;
        }

        request.setStatus(ContractTerminationRequest.PENDING_SETTLEMENT);
        request.setResponseReason(cleanOptional(reason));
        requestRepository.save(request);
        ContractSettlement settlement = initializeSettlement(request);
        settlementRepository.save(settlement);
        request.setSettlement(settlement);
        record(request, actor, "REQUEST_ACCEPTED", ContractTerminationRequest.PENDING_RESPONSE,
                ContractTerminationRequest.PENDING_SETTLEMENT, cleanOptional(reason));
        notify(request.getRequester(), contract, request, "Yêu cầu chấm dứt đã được chấp nhận",
                "Hai bên cần thống nhất và xác nhận bảng thanh lý hợp đồng " + contract.getContractCode() + ".");
        return request;
    }

    @Override
    public ContractSettlement submitSettlement(Integer requestId, TerminationSettlementForm form, String username) {
        User landlord = requireLandlord(currentUser(username));
        ContractTerminationRequest request = lockRequest(requestId);
        Contract contract = lockContract(request.getContract().getContractId());
        requireOwner(contract, landlord);
        if (!OPEN_REQUEST_STATUSES.contains(request.getStatus())
                || ContractTerminationRequest.PENDING_RESPONSE.equals(request.getStatus())) {
            throw new IllegalStateException("Yêu cầu chưa được chấp nhận hoặc đã hoàn tất.");
        }
        if (form == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu bảng thanh lý.");
        }

        ContractSettlement settlement = settlementRepository
                .findByTerminationRequest_TerminationRequestId(requestId)
                .orElseGet(() -> initializeSettlement(request));
        if (settlement.getStatus() == null || ContractSettlement.DRAFT.equals(settlement.getStatus())
                || ContractSettlement.DISPUTED.equals(settlement.getStatus())
                || ContractSettlement.PENDING_CONFIRMATION.equals(settlement.getStatus())) {
            applySettlementInputs(settlement, contract, request, form);
        } else {
            throw new IllegalStateException("Bảng thanh lý đã được hai bên xác nhận.");
        }

        settlement.setLandlordConfirmedAt(LocalDateTime.now());
        settlement.setTenantConfirmedAt(null);
        settlement.setTenantPaymentConfirmedAt(null);
        settlement.setTenantPaymentReference(null);
        settlement.setDepositRefundedAt(null);
        settlement.setDepositRefundReference(null);
        settlement.setDisputedAt(null);
        settlement.setDisputeReason(null);
        settlement.setStatus(ContractSettlement.PENDING_CONFIRMATION);
        settlementRepository.save(settlement);
        request.setStatus(ContractTerminationRequest.PENDING_SETTLEMENT);
        requestRepository.save(request);
        record(request, landlord, "SETTLEMENT_PROPOSED", ContractSettlement.DRAFT,
                ContractSettlement.PENDING_CONFIRMATION, "Chủ trọ đã lập và xác nhận bảng thanh lý.");
        notify(canonicalTenantUser(contract),
                contract, request, "Bảng thanh lý chờ xác nhận",
                "Chủ trọ đã cập nhật bảng thanh lý hợp đồng " + contract.getContractCode()
                        + ". Vui lòng kiểm tra và xác nhận hoặc ghi nhận tranh chấp.");
        return settlement;
    }

    @Override
    public ContractSettlement confirmSettlement(Integer requestId, String username) {
        User tenant = requireTenant(currentUser(username));
        ContractTerminationRequest request = lockRequest(requestId);
        Contract contract = lockContract(request.getContract().getContractId());
        requireTenantAccess(contract, tenant);
        ContractSettlement settlement = requirePendingSettlement(request);
        if (!ContractSettlement.PENDING_CONFIRMATION.equals(settlement.getStatus())
                || settlement.getLandlordConfirmedAt() == null) {
            throw new IllegalStateException("Chủ trọ chưa gửi bảng thanh lý để xác nhận.");
        }
        verifyInvoiceSnapshot(contract, settlement, request.getExpectedTerminationDate());
        settlement.setTenantConfirmedAt(LocalDateTime.now());
        settlement.setStatus(ContractSettlement.CONFIRMED);
        settlementRepository.save(settlement);
        record(request, tenant, "SETTLEMENT_CONFIRMED", ContractSettlement.PENDING_CONFIRMATION,
                ContractSettlement.CONFIRMED, "Người thuê đã xác nhận số liệu thanh lý.");
        notify(contract.getRoom().getBuilding().getOwner(), contract, request,
                "Người thuê đã xác nhận thanh lý",
                "Người thuê đã xác nhận bảng thanh lý hợp đồng " + contract.getContractCode()
                        + ". Hãy ghi nhận nghĩa vụ tài chính thực tế trước khi hoàn tất.");
        return settlement;
    }

    @Override
    public ContractSettlement disputeSettlement(Integer requestId, String reason, String username) {
        User actor = currentUser(username);
        ContractTerminationRequest request = lockRequest(requestId);
        Contract contract = lockContract(request.getContract().getContractId());
        authorizeAnyParticipant(request, contract, actor);
        ContractSettlement settlement = requirePendingSettlement(request);
        if (!ContractSettlement.PENDING_CONFIRMATION.equals(settlement.getStatus())) {
            throw new IllegalStateException("Bảng thanh lý hiện không chờ xác nhận/tranh chấp.");
        }
        if (reason == null || reason.isBlank() || reason.trim().length() > 1500) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung tranh chấp, tối đa 1500 ký tự.");
        }
        settlement.setStatus(ContractSettlement.DISPUTED);
        settlement.setDisputedAt(LocalDateTime.now());
        settlement.setDisputeReason(reason.trim());
        settlement.setTenantConfirmedAt(null);
        settlementRepository.save(settlement);
        request.setStatus(ContractTerminationRequest.DISPUTED);
        requestRepository.save(request);
        record(request, actor, "SETTLEMENT_DISPUTED", ContractSettlement.PENDING_CONFIRMATION,
                ContractSettlement.DISPUTED, reason.trim());
        User other = actor.getUserId().equals(request.getRequester().getUserId())
                ? request.getResponder() : request.getRequester();
        notify(other, contract, request, "Có tranh chấp bảng thanh lý",
                actor.getFullName() + " đã ghi nhận tranh chấp: " + reason.trim()
                        + ". Phòng vẫn được giữ nguyên trạng thái đang thuê.");
        return settlement;
    }

    @Override
    public ContractSettlement confirmFinancialTransfers(Integer requestId, String tenantPaymentReference,
                                                        String depositRefundReference, String username) {
        User landlord = requireLandlord(currentUser(username));
        ContractTerminationRequest request = lockRequest(requestId);
        Contract contract = lockContract(request.getContract().getContractId());
        requireOwner(contract, landlord);
        ContractSettlement settlement = requirePendingSettlement(request);
        if (!ContractSettlement.CONFIRMED.equals(settlement.getStatus())
                || settlement.getTenantConfirmedAt() == null) {
            throw new IllegalStateException("Hai bên chưa xác nhận bảng thanh lý.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (positive(settlement.getTotalDueAmount())) {
            String reference = normalizeReference(tenantPaymentReference, "mã giao dịch thanh toán");
            settlement.setTenantPaymentReference(reference);
            settlement.setTenantPaymentConfirmedAt(now);
            record(request, landlord, "TENANT_PAYMENT_CONFIRMED", null, null,
                    "Chủ trọ xác nhận đã đối chiếu giao dịch thanh toán: " + reference);
        }
        if (positive(settlement.getDepositRefundAmount())) {
            String reference = normalizeReference(depositRefundReference, "mã giao dịch hoàn cọc");
            settlement.setDepositRefundReference(reference);
            settlement.setDepositRefundedAt(now);
            record(request, landlord, "DEPOSIT_REFUND_CONFIRMED", null, null,
                    "Chủ trọ xác nhận đã đối chiếu giao dịch hoàn cọc: " + reference);
        }
        settlementRepository.save(settlement);
        return settlement;
    }

    @Override
    public ContractTerminationRequest completeTermination(Integer requestId, String username) {
        User landlord = requireLandlord(currentUser(username));
        ContractTerminationRequest request = lockRequest(requestId);
        Contract contract = lockContract(request.getContract().getContractId());
        requireOwner(contract, landlord);
        ContractSettlement settlement = requirePendingSettlement(request);
        if (!ContractTerminationRequest.PENDING_SETTLEMENT.equals(request.getStatus())
                || !ContractSettlement.CONFIRMED.equals(settlement.getStatus())
                || settlement.getLandlordConfirmedAt() == null
                || settlement.getTenantConfirmedAt() == null) {
            throw new IllegalStateException("Cần có xác nhận thanh lý của cả hai bên trước khi kết thúc hợp đồng.");
        }
        if (positive(settlement.getTotalDueAmount()) && settlement.getTenantPaymentConfirmedAt() == null) {
            throw new IllegalStateException("Chưa có xác nhận giao dịch thanh toán nghĩa vụ còn nợ.");
        }
        if (positive(settlement.getDepositRefundAmount()) && settlement.getDepositRefundedAt() == null) {
            throw new IllegalStateException("Chưa có xác nhận giao dịch hoàn tiền cọc.");
        }
        verifyInvoiceSnapshot(contract, settlement, request.getExpectedTerminationDate());

        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getRoomId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phòng của hợp đồng."));
        contract.setStatus("TERMINATED");
        contract.setActualEndDate(LocalDate.now());
        contractRepository.save(contract);
        contractTenantRepository.findById_ContractId(contract.getContractId()).forEach(relation -> {
            if (relation.getMoveOutDate() == null) {
                relation.setMoveOutDate(contract.getActualEndDate());
                contractTenantRepository.save(relation);
            }
        });

        boolean anotherActiveContract = contractRepository.findByRoom_RoomId(room.getRoomId()).stream()
                .filter(other -> !contract.getContractId().equals(other.getContractId()))
                .anyMatch(other -> ROOM_OCCUPYING_STATUSES.stream()
                        .anyMatch(status -> status.equalsIgnoreCase(other.getStatus())));
        if (!anotherActiveContract && !"BAO_TRI".equalsIgnoreCase(room.getStatus())) {
            room.setStatus("TRONG");
            roomRepository.save(room);
            roomListingRepository.findByManagedRoom_RoomIdForUpdate(room.getRoomId()).ifPresent(listing -> {
                if (!"BAO_TRI".equalsIgnoreCase(listing.getStatus())) {
                    listing.setStatus("TRONG");
                    listing.setCurrentTenant(null);
                    listing.setTenantPhone(null);
                    listing.setLeaseStartDate(null);
                    listing.setLeaseEndDate(null);
                    roomListingRepository.save(listing);
                }
            });
        }
        request.setStatus(ContractTerminationRequest.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        requestRepository.save(request);
        record(request, landlord, "TERMINATION_COMPLETED", ContractTerminationRequest.PENDING_SETTLEMENT,
                ContractTerminationRequest.COMPLETED,
                "Đã hoàn tất thanh lý; ngày chấm dứt thực tế " + contract.getActualEndDate() + ".");
        notify(request.getRequester(), contract, request, "Hợp đồng đã được thanh lý",
                "Hợp đồng " + contract.getContractCode() + " đã kết thúc sau khi hoàn tất các xác nhận.");
        notify(request.getResponder(), contract, request, "Hợp đồng đã được thanh lý",
                "Hợp đồng " + contract.getContractCode() + " đã kết thúc sau khi hoàn tất các xác nhận.");
        return request;
    }

    @Override
    public Contract cancelRequest(Integer requestId, String username) {
        User actor = currentUser(username);
        ContractTerminationRequest request = lockRequest(requestId);
        Contract contract = lockContract(request.getContract().getContractId());
        authorizeAnyParticipant(request, contract, actor);
        if (!ContractTerminationRequest.PENDING_RESPONSE.equals(request.getStatus())
                || !request.getRequester().getUserId().equals(actor.getUserId())) {
            throw new IllegalStateException("Chỉ người gửi mới được hủy yêu cầu đang chờ phản hồi.");
        }
        request.setStatus(ContractTerminationRequest.CANCELLED);
        contract.setStatus(request.getOriginalContractStatus());
        contractRepository.save(contract);
        requestRepository.save(request);
        record(request, actor, "REQUEST_CANCELLED", ContractTerminationRequest.PENDING_RESPONSE,
                ContractTerminationRequest.CANCELLED, "Người gửi đã hủy yêu cầu.");
        notify(request.getResponder(), contract, request, "Yêu cầu chấm dứt đã bị hủy",
                "Yêu cầu chấm dứt hợp đồng " + contract.getContractCode() + " đã bị người gửi hủy.");
        return contract;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contract> getTenantContracts(String username) {
        User user = requireTenant(currentUser(username));
        List<Contract> contracts = new ArrayList<>(contractRepository
                .findByTenantUser_UserIdOrderByContractIdDesc(user.getUserId()));
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        if (tenant != null) {
            contractRepository.findByRepresentative_TenantId(tenant.getTenantId()).forEach(contracts::add);
            contractTenantRepository.findById_TenantId(tenant.getTenantId()).stream()
                    .map(ContractTenant::getContract).forEach(contracts::add);
        }
        return contracts.stream()
                .filter(contract -> !"DRAFT".equalsIgnoreCase(contract.getStatus()))
                .distinct()
                .sorted(Comparator.comparing(Contract::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private ContractTerminationRequest createRequest(Contract contract, User requester, String side,
                                                     TerminationRequestForm form) {
        validateRequestForm(form, contract);
        if (!isTerminableContractStatus(contract.getStatus())) {
            throw new IllegalStateException("Chỉ hợp đồng đang có hiệu lực mới được yêu cầu chấm dứt.");
        }
        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getRoomId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phòng của hợp đồng."));
        if (contractRepository.existsByRoom_RoomIdAndStatusInAndContractIdNot(
                room.getRoomId(), ROOM_OCCUPYING_STATUSES, contract.getContractId())) {
            throw new IllegalStateException("Phòng đang có hợp đồng hiệu lực khác; không thể tạo yêu cầu chấm dứt.");
        }
        if (requestRepository.existsByContract_ContractIdAndStatusIn(contract.getContractId(), OPEN_REQUEST_STATUSES)) {
            throw new IllegalStateException("Hợp đồng đã có yêu cầu chấm dứt đang được xử lý.");
        }
        User owner = contract.getRoom().getBuilding().getOwner();
        User tenant = canonicalTenantUser(contract);
        if (tenant == null) {
            throw new IllegalStateException("Hợp đồng chưa liên kết tài khoản người thuê để gửi/nhận yêu cầu.");
        }
        User responder = "LANDLORD".equals(side) ? tenant : owner;
        if (responder == null || responder.getUserId().equals(requester.getUserId())) {
            throw new IllegalStateException("Không xác định được bên nhận yêu cầu chấm dứt.");
        }

        ContractTerminationRequest request = new ContractTerminationRequest();
        request.setContract(contract);
        request.setRequester(requester);
        request.setResponder(responder);
        request.setRequesterRole(side);
        request.setOriginalContractStatus(contract.getStatus());
        request.setReasonCode(form.getReasonCode().trim());
        request.setReasonDetails(form.getReasonDetails().trim());
        request.setNotes(cleanOptional(form.getNotes()));
        request.setExpectedTerminationDate(form.getExpectedTerminationDate());
        request.setStatus(ContractTerminationRequest.PENDING_RESPONSE);
        request.setCreatedAt(LocalDateTime.now());
        requestRepository.save(request);
        contract.setStatus("TERMINATION_REQUESTED");
        contractRepository.save(contract);
        record(request, requester, "REQUEST_SUBMITTED", request.getOriginalContractStatus(),
                ContractTerminationRequest.PENDING_RESPONSE, request.getReasonDetails());
        notify(responder, contract, request, "Yêu cầu chấm dứt hợp đồng mới",
                requester.getFullName() + " đã gửi yêu cầu chấm dứt hợp đồng "
                        + contract.getContractCode() + " của phòng " + room.getRoomCode()
                        + ". Ngày dự kiến: " + request.getExpectedTerminationDate() + ".");
        return request;
    }

    private ContractSettlement initializeSettlement(ContractTerminationRequest request) {
        Contract contract = request.getContract();
        ContractSettlement settlement = new ContractSettlement();
        settlement.setTerminationRequest(request);
        settlement.setStatus(ContractSettlement.DRAFT);
        settlement.setDepositAmount(contract.getDepositAmount());
        settlement.setDepositDeductionAmount(null);
        settlement.setRentThroughMoveOutAmount(unbilledRentThrough(contract, request.getExpectedTerminationDate()));
        settlement.setElectricityUnitPrice(contract.getMonthlyElectricPrice());
        settlement.setWaterUnitPrice(contract.getMonthlyWaterPrice());
        List<Invoice> invoices = invoiceRepository.findByContract_ContractId(contract.getContractId());
        InvoiceSnapshot snapshot = invoiceSnapshot(invoices, request.getExpectedTerminationDate());
        settlement.setUnpaidInvoiceAmount(snapshot.unpaidAmount());
        settlement.setPaidInvoiceAmount(snapshot.paidAmount());
        meterReadingRepository.findFirstByRoom_RoomIdOrderByReadDateDesc(contract.getRoom().getRoomId())
                .ifPresent(reading -> settlement.setElectricityOldReading(reading.getEndIndex()));
        return settlement;
    }

    private void applySettlementInputs(ContractSettlement settlement, Contract contract,
                                       ContractTerminationRequest request, TerminationSettlementForm form) {
        BigDecimal deposit = contract.getDepositAmount() != null
                ? contract.getDepositAmount() : form.getDepositAmount();
        requireAmount(deposit, "Tiền cọc ban đầu");
        settlement.setDepositAmount(deposit);

        BigDecimal rentThrough = unbilledRentThrough(contract, request.getExpectedTerminationDate());
        if (rentThrough == null) {
            rentThrough = form.getRentThroughMoveOutAmount();
        }
        requireAmount(rentThrough, "Tiền thuê đến ngày trả phòng");
        settlement.setRentThroughMoveOutAmount(rentThrough);

        BigDecimal electricityOld = form.getElectricityOldReading() != null
                ? form.getElectricityOldReading() : settlement.getElectricityOldReading();
        BigDecimal electricityNew = form.getElectricityNewReading();
        BigDecimal electricityPrice = contract.getMonthlyElectricPrice() != null
                ? contract.getMonthlyElectricPrice() : form.getElectricityUnitPrice();
        BigDecimal waterOld = form.getWaterOldReading() != null
                ? form.getWaterOldReading() : settlement.getWaterOldReading();
        BigDecimal waterNew = form.getWaterNewReading();
        BigDecimal waterPrice = contract.getMonthlyWaterPrice() != null
                ? contract.getMonthlyWaterPrice() : form.getWaterUnitPrice();
        requireMeter(electricityOld, electricityNew, electricityPrice, "điện");
        requireMeter(waterOld, waterNew, waterPrice, "nước");
        settlement.setElectricityOldReading(electricityOld);
        settlement.setElectricityNewReading(electricityNew);
        settlement.setElectricityUnitPrice(electricityPrice);
        settlement.setElectricityAmount(ContractSettlement.calculateAmount(
                electricityNew.subtract(electricityOld), electricityPrice));
        settlement.setWaterOldReading(waterOld);
        settlement.setWaterNewReading(waterNew);
        settlement.setWaterUnitPrice(waterPrice);
        settlement.setWaterAmount(ContractSettlement.calculateAmount(
                waterNew.subtract(waterOld), waterPrice));

        BigDecimal service = requireAmount(form.getServiceFeeAmount(), "Phí dịch vụ");
        BigDecimal compensation = requireAmount(form.getCompensationAmount(), "Khoản bồi thường");
        BigDecimal deduction = requireAmount(form.getDepositDeductionAmount(), "Khoản khấu trừ tiền cọc");
        if (compensation.compareTo(BigDecimal.ZERO) > 0
                && (form.getCompensationReason() == null || form.getCompensationReason().isBlank())) {
            throw new IllegalArgumentException("Cần nêu lý do cho khoản bồi thường.");
        }
        if (deduction.compareTo(BigDecimal.ZERO) > 0
                && (form.getDepositDeductionReason() == null || form.getDepositDeductionReason().isBlank())) {
            throw new IllegalArgumentException("Cần nêu lý do khấu trừ tiền cọc.");
        }
        if (form.getRoomHandoverCondition() == null || form.getRoomHandoverCondition().isBlank()) {
            throw new IllegalArgumentException("Cần ghi nhận tình trạng bàn giao phòng.");
        }
        settlement.setServiceFeeAmount(service);
        settlement.setCompensationAmount(compensation);
        settlement.setCompensationReason(cleanOptional(form.getCompensationReason()));
        settlement.setDepositDeductionAmount(deduction);
        settlement.setDepositDeductionReason(cleanOptional(form.getDepositDeductionReason()));
        settlement.setRoomHandoverCondition(form.getRoomHandoverCondition().trim());
        settlement.setDamageNotes(cleanOptional(form.getDamageNotes()));
        settlement.setNotes(cleanOptional(form.getNotes()));

        InvoiceSnapshot snapshot = invoiceSnapshot(invoiceRepository.findByContract_ContractId(contract.getContractId()),
                request.getExpectedTerminationDate());
        settlement.setUnpaidInvoiceAmount(snapshot.unpaidAmount());
        settlement.setPaidInvoiceAmount(snapshot.paidAmount());
        BigDecimal gross = snapshot.unpaidAmount().add(rentThrough)
                .add(settlement.getElectricityAmount()).add(settlement.getWaterAmount())
                .add(service).add(compensation);
        if (deduction.compareTo(deposit) > 0 || deduction.compareTo(gross) > 0) {
            throw new IllegalArgumentException("Khoản khấu trừ không được vượt tiền cọc hoặc tổng nghĩa vụ hợp lệ.");
        }
        settlement.setDepositRefundAmount(deposit.subtract(deduction).setScale(2, RoundingMode.HALF_UP));
        settlement.setTotalDueAmount(gross.subtract(deduction).setScale(2, RoundingMode.HALF_UP));
    }

    private void verifyInvoiceSnapshot(Contract contract, ContractSettlement settlement, LocalDate throughDate) {
        InvoiceSnapshot current = invoiceSnapshot(
                invoiceRepository.findByContract_ContractId(contract.getContractId()), throughDate);
        if (!sameMoney(current.unpaidAmount(), settlement.getUnpaidInvoiceAmount())
                || !sameMoney(current.paidAmount(), settlement.getPaidInvoiceAmount())) {
            throw new IllegalStateException("Hóa đơn/thanh toán đã thay đổi sau khi lập bảng thanh lý. "
                    + "Chủ trọ cần cập nhật và gửi lại bảng thanh lý.");
        }
    }

    private InvoiceSnapshot invoiceSnapshot(List<Invoice> invoices, LocalDate throughDate) {
        BigDecimal unpaid = ZERO;
        BigDecimal paid = ZERO;
        for (Invoice invoice : invoices) {
            if (invoice.getPeriodStart() == null || invoice.getPeriodStart().isAfter(throughDate)
                    || "CANCELLED".equalsIgnoreCase(invoice.getStatus())
                    || "CANCELED".equalsIgnoreCase(invoice.getStatus())
                    || "ĐÃ HỦY".equalsIgnoreCase(invoice.getStatus())) {
                continue;
            }
            BigDecimal total = amountOrZero(invoice.getTotalAmount());
            List<Payment> payments = paymentRepository.findByInvoice_InvoiceId(invoice.getInvoiceId());
            BigDecimal confirmedPayments = payments.stream()
                    .filter(payment -> "SUCCESS".equalsIgnoreCase(payment.getStatus()))
                    .map(Payment::getAmount).filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if ("PAID".equalsIgnoreCase(invoice.getStatus())
                    || "ĐÃ THANH TOÁN".equalsIgnoreCase(invoice.getStatus())) {
                paid = paid.add(confirmedPayments.signum() > 0 ? confirmedPayments : total);
            } else {
                paid = paid.add(confirmedPayments);
                unpaid = unpaid.add(total.subtract(confirmedPayments).max(BigDecimal.ZERO));
            }
        }
        return new InvoiceSnapshot(money(unpaid), money(paid));
    }

    private BigDecimal unbilledRentThrough(Contract contract, LocalDate throughDate) {
        BigDecimal monthlyRent = contract.getMonthlyRent() != null
                ? contract.getMonthlyRent()
                : contract.getRoom() == null ? null : contract.getRoom().getPriceOverride();
        if (monthlyRent == null || contract.getStartDate() == null || throughDate == null) {
            return null;
        }
        if (throughDate.isBefore(contract.getStartDate())) {
            throw new IllegalArgumentException("Ngày trả phòng không được trước ngày bắt đầu hợp đồng.");
        }
        List<Invoice> invoices = invoiceRepository.findByContract_ContractId(contract.getContractId()).stream()
                .filter(invoice -> invoice.getPeriodStart() != null && invoice.getPeriodEnd() != null)
                .filter(invoice -> !invoice.getPeriodStart().isAfter(throughDate))
                .toList();
        BigDecimal result = BigDecimal.ZERO;
        YearMonth month = YearMonth.from(contract.getStartDate());
        YearMonth lastMonth = YearMonth.from(throughDate);
        while (!month.isAfter(lastMonth)) {
            LocalDate segmentStart = month.atDay(1).isBefore(contract.getStartDate())
                    ? contract.getStartDate() : month.atDay(1);
            LocalDate segmentEnd = month.atEndOfMonth().isAfter(throughDate)
                    ? throughDate : month.atEndOfMonth();
            int totalDays = (int) (segmentEnd.toEpochDay() - segmentStart.toEpochDay() + 1);
            List<DateRange> ranges = invoices.stream()
                    .filter(invoice -> !invoice.getPeriodEnd().isBefore(segmentStart)
                            && !invoice.getPeriodStart().isAfter(segmentEnd))
                    .map(invoice -> new DateRange(
                            invoice.getPeriodStart().isBefore(segmentStart) ? segmentStart : invoice.getPeriodStart(),
                            invoice.getPeriodEnd().isAfter(segmentEnd) ? segmentEnd : invoice.getPeriodEnd()))
                    .sorted(Comparator.comparing(DateRange::start))
                    .toList();
            int coveredDays = coveredDays(ranges);
            int unbilledDays = totalDays - coveredDays;
            if (unbilledDays > 0) {
                result = result.add(monthlyRent
                        .divide(BigDecimal.valueOf(month.lengthOfMonth()), 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(unbilledDays)));
            }
            month = month.plusMonths(1);
        }
        return money(result);
    }

    private static int coveredDays(List<DateRange> ranges) {
        if (ranges.isEmpty()) {
            return 0;
        }
        LocalDate start = ranges.get(0).start();
        LocalDate end = ranges.get(0).end();
        int days = 0;
        for (int i = 1; i < ranges.size(); i++) {
            DateRange next = ranges.get(i);
            if (!next.start().isAfter(end.plusDays(1))) {
                if (next.end().isAfter(end)) {
                    end = next.end();
                }
            } else {
                days += (int) (end.toEpochDay() - start.toEpochDay() + 1);
                start = next.start();
                end = next.end();
            }
        }
        return days + (int) (end.toEpochDay() - start.toEpochDay() + 1);
    }

    private void validateRequestForm(TerminationRequestForm form, Contract contract) {
        if (form == null || form.getExpectedTerminationDate() == null
                || form.getReasonCode() == null || form.getReasonCode().isBlank()
                || form.getReasonDetails() == null || form.getReasonDetails().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ lý do và ngày dự kiến chấm dứt.");
        }
        if (form.getReasonCode().trim().length() > 60 || form.getReasonDetails().trim().length() > 1500
                || (form.getNotes() != null && form.getNotes().length() > 2000)) {
            throw new IllegalArgumentException("Thông tin yêu cầu vượt quá độ dài cho phép.");
        }
        if (!List.of("Hết nhu cầu thuê", "Chuyển nơi ở", "Điều kiện phòng không còn phù hợp",
                "Lý do cá nhân", "Lý do khác").contains(form.getReasonCode().trim())) {
            throw new IllegalArgumentException("Lý do chấm dứt không hợp lệ.");
        }
        if (form.getExpectedTerminationDate().isBefore(LocalDate.now())
                || (contract.getStartDate() != null
                && form.getExpectedTerminationDate().isBefore(contract.getStartDate()))) {
            throw new IllegalArgumentException("Ngày dự kiến chấm dứt phải từ hôm nay trở đi và không trước ngày bắt đầu thuê.");
        }
    }

    private static void requireMeter(BigDecimal oldValue, BigDecimal newValue, BigDecimal unitPrice, String label) {
        requireAmount(oldValue, "Chỉ số " + label + " cũ");
        requireAmount(newValue, "Chỉ số " + label + " mới");
        requireAmount(unitPrice, "Đơn giá " + label);
        if (newValue.compareTo(oldValue) < 0) {
            throw new IllegalArgumentException("Chỉ số " + label + " mới không được nhỏ hơn chỉ số cũ.");
        }
    }

    private static BigDecimal requireAmount(BigDecimal amount, String label) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " cần được nhập và không được âm.");
        }
        return money(amount);
    }

    private static BigDecimal optionalAmount(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền không được âm.");
        }
        return money(amount);
    }

    private User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản đang đăng nhập."));
    }

    private User requireLandlord(User user) {
        if (!user.isEnabled() || !user.isApproved() || user.getRole() == null
                || !Role.LANDLORD.equalsIgnoreCase(Role.normalizeRoleName(user.getRole().getRoleName()))) {
            throw new AccessDeniedException("Chức năng này chỉ dành cho chủ trọ đã được duyệt.");
        }
        return user;
    }

    private User requireTenant(User user) {
        if (!user.isActiveTenant()) {
            throw new AccessDeniedException("Chức năng này chỉ dành cho người thuê đang hoạt động.");
        }
        return user;
    }

    private Contract lockContract(Integer contractId) {
        return contractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hợp đồng."));
    }

    private ContractTerminationRequest lockRequest(Integer requestId) {
        return requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu chấm dứt."));
    }

    private void requireOwner(Contract contract, User user) {
        if (contract.getRoom() == null || contract.getRoom().getBuilding() == null
                || contract.getRoom().getBuilding().getOwner() == null
                || !user.getUserId().equals(contract.getRoom().getBuilding().getOwner().getUserId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập hợp đồng của chủ trọ khác.");
        }
    }

    private void requireTenantAccess(Contract contract, User user) {
        if (contract.getTenantUser() != null && user.getUserId().equals(contract.getTenantUser().getUserId())) {
            return;
        }
        if (contract.getRentalRequest() != null && contract.getRentalRequest().getUser() != null
                && user.getUserId().equals(contract.getRentalRequest().getUser().getUserId())) {
            return;
        }
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        if (tenant != null) {
            if (contract.getRepresentative() != null
                    && tenant.getTenantId().equals(contract.getRepresentative().getTenantId())) {
                return;
            }
            if (contractTenantRepository.existsById_ContractIdAndId_TenantId(
                    contract.getContractId(), tenant.getTenantId())) {
                return;
            }
        }
        throw new AccessDeniedException("Hợp đồng này không thuộc tài khoản người thuê hiện tại.");
    }

    private void authorizeRequestParticipant(ContractTerminationRequest request, Contract contract,
                                             User actor, boolean landlord) {
        if (landlord) {
            requireOwner(contract, actor);
        } else {
            requireTenantAccess(contract, actor);
        }
        authorizeAnyParticipant(request, contract, actor);
    }

    private void authorizeAnyParticipant(ContractTerminationRequest request, Contract contract, User actor) {
        boolean owner = contract.getRoom() != null && contract.getRoom().getBuilding() != null
                && contract.getRoom().getBuilding().getOwner() != null
                && actor.getUserId().equals(contract.getRoom().getBuilding().getOwner().getUserId());
        User canonicalTenant = canonicalTenantUser(contract);
        boolean tenant = request.getRequester().getUserId().equals(actor.getUserId())
                || request.getResponder().getUserId().equals(actor.getUserId())
                || (canonicalTenant != null && canonicalTenant.getUserId().equals(actor.getUserId()));
        if (!owner && !tenant) {
            throw new AccessDeniedException("Bạn không phải bên tham gia yêu cầu chấm dứt này.");
        }
    }

    private User canonicalTenantUser(Contract contract) {
        if (contract.getTenantUser() != null) {
            return contract.getTenantUser();
        }
        if (contract.getRepresentative() != null && contract.getRepresentative().getUser() != null) {
            return contract.getRepresentative().getUser();
        }
        return contract.getRentalRequest() == null ? null : contract.getRentalRequest().getUser();
    }

    private ContractSettlement requirePendingSettlement(ContractTerminationRequest request) {
        if (request.getSettlement() != null) {
            return request.getSettlement();
        }
        return settlementRepository.findByTerminationRequest_TerminationRequestId(
                        request.getTerminationRequestId())
                .orElseThrow(() -> new IllegalStateException("Chưa có bảng thanh lý cho yêu cầu này."));
    }

    private void notify(User recipient, Contract contract, ContractTerminationRequest request,
                        String title, String message) {
        if (recipient == null) {
            throw new IllegalStateException("Không xác định được người nhận thông báo.");
        }
        UserNotification notification = new UserNotification();
        notification.setRecipient(recipient);
        notification.setContract(contract);
        notification.setTerminationRequest(request);
        notification.setNotificationType(UserNotification.TYPE_CONTRACT_TERMINATION);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private void record(ContractTerminationRequest request, User actor, String action,
                        String previousStatus, String newStatus, String details) {
        ContractTerminationHistory history = new ContractTerminationHistory();
        history.setTerminationRequest(request);
        history.setContract(request.getContract());
        history.setActor(actor);
        history.setAction(action);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setDetails(cleanOptional(details));
        history.setHappenedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    private static boolean isTerminableContractStatus(String status) {
        return status != null && List.of("ACTIVE", "HIỆU LỰC", "ĐANG HIỆU LỰC")
                .stream().anyMatch(status::equalsIgnoreCase);
    }

    private static boolean positive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String normalizeReference(String reference, String label) {
        if (reference == null || reference.isBlank() || reference.trim().length() > 100) {
            throw new IllegalArgumentException("Cần nhập " + label + " đã đối chiếu thực tế (tối đa 100 ký tự).");
        }
        return reference.trim();
    }

    private static String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal amountOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean sameMoney(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private record InvoiceSnapshot(BigDecimal unpaidAmount, BigDecimal paidAmount) { }
    private record DateRange(LocalDate start, LocalDate end) { }
}
