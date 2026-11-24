package com.qlphongtro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceCreateRequest {

    @NotNull(message = "Vui lòng chọn hợp đồng/phòng.")
    private Integer contractId;

    @NotBlank(message = "Vui lòng chọn tháng thanh toán.")
    private String month;

    @NotNull(message = "Vui lòng chọn hạn thanh toán.")
    private LocalDate dueDate;

    @NotNull(message = "Tiền phòng là bắt buộc.")
    @PositiveOrZero(message = "Tiền phòng không được âm.")
    private BigDecimal roomAmount;

    @NotNull(message = "Chỉ số điện cũ là bắt buộc.")
    @PositiveOrZero(message = "Chỉ số điện không được âm.")
    private BigDecimal electricityPreviousReading;

    @NotNull(message = "Chỉ số điện mới là bắt buộc.")
    @PositiveOrZero(message = "Chỉ số điện không được âm.")
    private BigDecimal electricityCurrentReading;

    @NotNull(message = "Đơn giá điện là bắt buộc.")
    @PositiveOrZero(message = "Đơn giá điện không được âm.")
    private BigDecimal electricityUnitPrice;

    @NotNull(message = "Chỉ số nước cũ là bắt buộc.")
    @PositiveOrZero(message = "Chỉ số nước không được âm.")
    private BigDecimal waterPreviousReading;

    @NotNull(message = "Chỉ số nước mới là bắt buộc.")
    @PositiveOrZero(message = "Chỉ số nước không được âm.")
    private BigDecimal waterCurrentReading;

    @NotNull(message = "Đơn giá nước là bắt buộc.")
    @PositiveOrZero(message = "Đơn giá nước không được âm.")
    private BigDecimal waterUnitPrice;

    private String notes;

    @Valid
    private List<InvoiceServiceChargeRequest> serviceCharges = new ArrayList<>();

    public Integer getContractId() { return contractId; }
    public void setContractId(Integer contractId) { this.contractId = contractId; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getRoomAmount() { return roomAmount; }
    public void setRoomAmount(BigDecimal roomAmount) { this.roomAmount = roomAmount; }
    public BigDecimal getElectricityPreviousReading() { return electricityPreviousReading; }
    public void setElectricityPreviousReading(BigDecimal electricityPreviousReading) { this.electricityPreviousReading = electricityPreviousReading; }
    public BigDecimal getElectricityCurrentReading() { return electricityCurrentReading; }
    public void setElectricityCurrentReading(BigDecimal electricityCurrentReading) { this.electricityCurrentReading = electricityCurrentReading; }
    public BigDecimal getElectricityUnitPrice() { return electricityUnitPrice; }
    public void setElectricityUnitPrice(BigDecimal electricityUnitPrice) { this.electricityUnitPrice = electricityUnitPrice; }
    public BigDecimal getWaterPreviousReading() { return waterPreviousReading; }
    public void setWaterPreviousReading(BigDecimal waterPreviousReading) { this.waterPreviousReading = waterPreviousReading; }
    public BigDecimal getWaterCurrentReading() { return waterCurrentReading; }
    public void setWaterCurrentReading(BigDecimal waterCurrentReading) { this.waterCurrentReading = waterCurrentReading; }
    public BigDecimal getWaterUnitPrice() { return waterUnitPrice; }
    public void setWaterUnitPrice(BigDecimal waterUnitPrice) { this.waterUnitPrice = waterUnitPrice; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<InvoiceServiceChargeRequest> getServiceCharges() { return serviceCharges; }
    public void setServiceCharges(List<InvoiceServiceChargeRequest> serviceCharges) {
        this.serviceCharges = serviceCharges == null ? new ArrayList<>() : serviceCharges;
    }
}
