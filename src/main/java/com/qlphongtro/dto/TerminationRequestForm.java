package com.qlphongtro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TerminationRequestForm {

    @NotBlank(message = "Vui lòng chọn lý do chấm dứt.")
    @Size(max = 60, message = "Lý do không hợp lệ.")
    private String reasonCode;

    @NotBlank(message = "Vui lòng nhập lý do chi tiết.")
    @Size(max = 1500, message = "Lý do chi tiết tối đa 1500 ký tự.")
    private String reasonDetails;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự.")
    private String notes;

    @NotNull(message = "Vui lòng chọn ngày dự kiến chấm dứt.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedTerminationDate;

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonDetails() { return reasonDetails; }
    public void setReasonDetails(String reasonDetails) { this.reasonDetails = reasonDetails; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getExpectedTerminationDate() { return expectedTerminationDate; }
    public void setExpectedTerminationDate(LocalDate expectedTerminationDate) { this.expectedTerminationDate = expectedTerminationDate; }
}
