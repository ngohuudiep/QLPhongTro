package com.qlphongtro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewReplyForm {

    @NotBlank(message = "Vui lòng nhập nội dung phản hồi.")
    @Size(max = 2000, message = "Nội dung phản hồi tối đa 2000 ký tự.")
    private String replyComment;

    public ReviewReplyForm() {}

    public ReviewReplyForm(String replyComment) {
        this.replyComment = replyComment;
    }

    public String getReplyComment() { return replyComment; }
    public void setReplyComment(String replyComment) { this.replyComment = replyComment; }
}
