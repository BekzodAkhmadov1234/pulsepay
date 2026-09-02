package uz.pulsepay.domain.identity;

public enum CooldownType {
    OTP_LOCKOUT,              // 3 failed OTP attempts → 15-minute lockout (REG-03)
    TRANSFER_RESTRICTION,     // security event → transfers blocked temporarily
    LOGIN_RESTRICTION,        // account-level login restriction
    CARD_REACTIVATION_PENDING // new-device/password-change → cards inactive, OTP needed to reactivate
}
