package com.qlphongtro.service;

import com.qlphongtro.dto.TenantDashboardView;
import com.qlphongtro.dto.IncidentReportRequest;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Incident;
import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Payment;
import com.qlphongtro.entity.UserNotification;

import java.util.List;

public interface TenantPortalService {

    TenantDashboardView getDashboard(String username);

    List<Contract> getContracts(String username);

    List<Contract> getActiveContracts(String username);

    List<Invoice> getInvoices(String username, String status, String month);

    Invoice getInvoice(String username, Integer invoiceId);

    List<Payment> getPayments(String username);

    List<UserNotification> getNotifications(String username);

    String markNotificationRead(String username, Integer notificationId);

    List<Incident> getIncidents(String username);

    Incident reportIncident(String username, IncidentReportRequest request);
}
