package com.qlphongtro.dto;

import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Tenant;
import com.qlphongtro.entity.User;

import java.math.BigDecimal;
import java.util.List;

public class TenantDashboardView {

    private final User user;
    private final Tenant tenant;
    private final List<Contract> contracts;
    private final Contract activeContract;
    private final List<Invoice> invoices;
    private final Invoice currentInvoice;
    private final long unpaidInvoiceCount;
    private final long unreadNotificationCount;
    private final BigDecimal outstandingAmount;

    public TenantDashboardView(User user, Tenant tenant, List<Contract> contracts, Contract activeContract,
                               List<Invoice> invoices, Invoice currentInvoice, long unpaidInvoiceCount,
                               long unreadNotificationCount, BigDecimal outstandingAmount) {
        this.user = user;
        this.tenant = tenant;
        this.contracts = contracts;
        this.activeContract = activeContract;
        this.invoices = invoices;
        this.currentInvoice = currentInvoice;
        this.unpaidInvoiceCount = unpaidInvoiceCount;
        this.unreadNotificationCount = unreadNotificationCount;
        this.outstandingAmount = outstandingAmount;
    }

    public User getUser() { return user; }
    public Tenant getTenant() { return tenant; }
    public List<Contract> getContracts() { return contracts; }
    public Contract getActiveContract() { return activeContract; }
    public List<Invoice> getInvoices() { return invoices; }
    public Invoice getCurrentInvoice() { return currentInvoice; }
    public long getUnpaidInvoiceCount() { return unpaidInvoiceCount; }
    public long getUnreadNotificationCount() { return unreadNotificationCount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
}
