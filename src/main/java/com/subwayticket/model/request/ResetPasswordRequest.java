package com.subwayticket.model.request;

/**
 * Created by shengyun-zhou on 6/6/16.
 */
public class ResetPasswordRequest {
    private String phoneNumber;
    private String oldPassword;
    private String newPassword;
    private String captcha;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String phoneNumber, String newPassword, String captcha) {
        this.phoneNumber = phoneNumber;
        this.newPassword = newPassword;
        this.captcha = captcha;
    }

    public ResetPasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
}
