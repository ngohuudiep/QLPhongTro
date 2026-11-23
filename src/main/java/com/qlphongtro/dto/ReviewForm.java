package com.qlphongtro.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewForm {

    @NotNull(message = "Vui lòng chọn hợp đồng thuê.")
    private Integer contractId;

    @NotNull(message = "Vui lòng chọn số sao đánh giá.")
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao.")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao.")
    private Integer rating;

    @NotBlank(message = "Vui lòng nhập nhận xét chi tiết.")
    @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự.")
    private String comment;

    public ReviewForm() {}

    public ReviewForm(Integer contractId, Integer rating, String comment) {
        this.contractId = contractId;
        this.rating = rating;
        this.comment = comment;
    }

    public Integer getContractId() { return contractId; }
    public void setContractId(Integer contractId) { this.contractId = contractId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
