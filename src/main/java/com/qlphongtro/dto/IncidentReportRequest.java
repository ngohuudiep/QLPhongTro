package com.qlphongtro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IncidentReportRequest {

    @NotNull(message = "Vui lòng chọn phòng đang thuê.")
    private Integer contractId;

    @NotBlank(message = "Vui lòng nhập tiêu đề sự cố.")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự.")
    private String title;

    @NotBlank(message = "Vui lòng mô tả sự cố.")
    @Size(max = 2000, message = "Mô tả tối đa 2.000 ký tự.")
    private String description;

    private String priority = "MEDIUM";

    public Integer getContractId() { return contractId; }
    public void setContractId(Integer contractId) { this.contractId = contractId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
