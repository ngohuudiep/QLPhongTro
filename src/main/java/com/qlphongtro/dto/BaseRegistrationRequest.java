package com.qlphongtro.dto;

public class BaseRegistrationRequest {

    private String roleChoice = "TENANT";

    public String getRoleChoice() {
        return roleChoice;
    }

    public void setRoleChoice(String roleChoice) {
        this.roleChoice = roleChoice;
    }
}
