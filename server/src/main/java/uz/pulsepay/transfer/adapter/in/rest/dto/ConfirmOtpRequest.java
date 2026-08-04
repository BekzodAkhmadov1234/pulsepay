package uz.pulsepay.transfer.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmOtpRequest(@NotBlank @Size(min = 6, max = 6) String code) {}
