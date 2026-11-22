package com.qlphongtro.service;

import com.qlphongtro.entity.BookingRequest;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.DepositConfirmationRequest;
import com.qlphongtro.entity.Payment;
import java.math.BigDecimal;
import java.util.List;

public interface RentalWorkflowService {
    BookingRequest createRequest(String username, Integer listingId, String fullName, String phone,
                                 String idNumber, Short expectedOccupants, java.time.LocalDate expectedMoveInDate, String note);
    BookingRequest createRequest(String username, Integer listingId, Short expectedOccupants,
                                 java.time.LocalDate expectedMoveInDate, String note);
    List<BookingRequest> getTenantRequests(String username);
    List<BookingRequest> getLandlordRequests(String username);
    BookingRequest getLandlordRequest(Integer requestId, String username);
    Contract getLandlordContractForRequest(Integer requestId, String username);
    Contract getLandlordDraftForRequest(Integer requestId, String username);
    BookingRequest approveRequest(Integer requestId, String username);
    BookingRequest rejectRequest(Integer requestId, String reason, String username);
    Contract createAndSendContract(Integer requestId, Contract details, String username);
    Contract tenantAcceptContract(Integer contractId, String username);
    Contract tenantRejectContract(Integer contractId, String reason, String username);
    DepositConfirmationRequest createDepositConfirmationRequest(Integer contractId, String username);
    List<DepositConfirmationRequest> getTenantDepositRequests(Integer contractId, String username);
    DepositConfirmationRequest getLandlordLatestDepositRequest(Integer contractId, String username);
    List<DepositConfirmationRequest> getLandlordDepositRequests(String username, String status);
    DepositConfirmationRequest approveDepositConfirmationRequest(
            Integer requestId, String referenceNo, String username);
    DepositConfirmationRequest rejectDepositConfirmationRequest(
            Integer requestId, String reason, String username);
    Contract landlordApproveContract(Integer contractId, String username);
    Contract landlordRejectContract(Integer contractId, String reason, String username);
    Contract getTenantContract(Integer contractId, String username);
    List<Contract> getTenantContracts(String username);
    List<Payment> getContractPayments(Integer contractId, String username);
    Contract getLandlordContract(Integer contractId, String username);
    List<Payment> getLandlordContractPayments(Integer contractId, String username);
    List<com.qlphongtro.entity.UserNotification> getLandlordNotifications(String username);
    String openLandlordNotification(Integer notificationId, String username);
}
