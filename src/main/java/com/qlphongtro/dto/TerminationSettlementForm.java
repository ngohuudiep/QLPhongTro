package com.qlphongtro.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class TerminationSettlementForm {

    private BigDecimal depositAmount;
    private BigDecimal rentThroughMoveOutAmount;
    private BigDecimal electricityOldReading;
    private BigDecimal electricityNewReading;
    private BigDecimal electricityUnitPrice;
    private BigDecimal waterOldReading;
    private BigDecimal waterNewReading;
    private BigDecimal waterUnitPrice;
    private BigDecimal serviceFeeAmount;
    private BigDecimal compensationAmount;
    private BigDecimal depositDeductionAmount;

    @Size(max = 1000, message = "Mô tả khoản bồi thường tối đa 1000 ký tự.")
    private String compensationReason;

    @Size(max = 1000, message = "Lý do khấu trừ tối đa 1000 ký tự.")
    private String depositDeductionReason;

    @Size(max = 1500, message = "Mô tả bàn giao tối đa 1500 ký tự.")
    private String roomHandoverCondition;

    @Size(max = 1500, message = "Ghi chú hư hỏng tối đa 1500 ký tự.")
    private String damageNotes;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự.")
    private String notes;

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getRentThroughMoveOutAmount() { return rentThroughMoveOutAmount; }
    public void setRentThroughMoveOutAmount(BigDecimal rentThroughMoveOutAmount) { this.rentThroughMoveOutAmount = rentThroughMoveOutAmount; }
    public BigDecimal getElectricityOldReading() { return electricityOldReading; }
    public void setElectricityOldReading(BigDecimal electricityOldReading) { this.electricityOldReading = electricityOldReading; }
    public BigDecimal getElectricityNewReading() { return electricityNewReading; }
    public void setElectricityNewReading(BigDecimal electricityNewReading) { this.electricityNewReading = electricityNewReading; }
    public BigDecimal getElectricityUnitPrice() { return electricityUnitPrice; }
    public void setElectricityUnitPrice(BigDecimal electricityUnitPrice) { this.electricityUnitPrice = electricityUnitPrice; }
    public BigDecimal getWaterOldReading() { return waterOldReading; }
    public void setWaterOldReading(BigDecimal waterOldReading) { this.waterOldReading = waterOldReading; }
    public BigDecimal getWaterNewReading() { return waterNewReading; }
    public void setWaterNewReading(BigDecimal waterNewReading) { this.waterNewReading = waterNewReading; }
    public BigDecimal getWaterUnitPrice() { return waterUnitPrice; }
    public void setWaterUnitPrice(BigDecimal waterUnitPrice) { this.waterUnitPrice = waterUnitPrice; }
    public BigDecimal getServiceFeeAmount() { return serviceFeeAmount; }
    public void setServiceFeeAmount(BigDecimal serviceFeeAmount) { this.serviceFeeAmount = serviceFeeAmount; }
    public BigDecimal getCompensationAmount() { return compensationAmount; }
    public void setCompensationAmount(BigDecimal compensationAmount) { this.compensationAmount = compensationAmount; }
    public BigDecimal getDepositDeductionAmount() { return depositDeductionAmount; }
    public void setDepositDeductionAmount(BigDecimal depositDeductionAmount) { this.depositDeductionAmount = depositDeductionAmount; }
    public String getCompensationReason() { return compensationReason; }
    public void setCompensationReason(String compensationReason) { this.compensationReason = compensationReason; }
    public String getDepositDeductionReason() { return depositDeductionReason; }
    public void setDepositDeductionReason(String depositDeductionReason) { this.depositDeductionReason = depositDeductionReason; }
    public String getRoomHandoverCondition() { return roomHandoverCondition; }
    public void setRoomHandoverCondition(String roomHandoverCondition) { this.roomHandoverCondition = roomHandoverCondition; }
    public String getDamageNotes() { return damageNotes; }
    public void setDamageNotes(String damageNotes) { this.damageNotes = damageNotes; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
