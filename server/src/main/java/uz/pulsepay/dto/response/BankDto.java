package uz.pulsepay.dto.response;

import java.util.UUID;

public record BankDto(UUID id, String mfoCode, String name) {}
