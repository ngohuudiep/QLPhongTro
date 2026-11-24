package com.qlphongtro.service;

import com.qlphongtro.dto.TerminationRequestForm;
import com.qlphongtro.dto.TerminationSettlementForm;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.ContractTerminationHistory;
import com.qlphongtro.entity.ContractTerminationRequest;
import com.qlphongtro.entity.ContractSettlement;

import java.util.List;

public interface ContractTerminationService {

    List<ContractTerminationRequest> getLandlordRequests(String username);

    List<ContractTerminationRequest> getTenantRequests(String username);

    ContractTerminationRequest getLandlordRequest(Integer requestId, String username);

    ContractTerminationRequest getTenantRequest(Integer requestId, String username);

    List<ContractTerminationHistory> getHistory(Integer requestId, String username, boolean landlord);

    ContractTerminationRequest requestFromTenant(Integer contractId, TerminationRequestForm form, String username);

    ContractTerminationRequest requestFromLandlord(Integer contractId, TerminationRequestForm form, String username);

    ContractTerminationRequest respond(Integer requestId, boolean accept, String reason, String username, boolean landlord);

    ContractSettlement submitSettlement(Integer requestId, TerminationSettlementForm form, String username);

    ContractSettlement confirmSettlement(Integer requestId, String username);

    ContractSettlement disputeSettlement(Integer requestId, String reason, String username);

    ContractSettlement confirmFinancialTransfers(Integer requestId, String tenantPaymentReference,
                                                  String depositRefundReference, String username);

    ContractTerminationRequest completeTermination(Integer requestId, String username);

    Contract cancelRequest(Integer requestId, String username);

    List<Contract> getTenantContracts(String username);
}
