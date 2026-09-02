package uz.pulsepay.mapper;

import org.springframework.stereotype.Component;
import uz.pulsepay.dto.response.BankDto;
import uz.pulsepay.domain.reference.Bank;

@Component
public class BankMapper {

    public BankDto toDto(Bank bank) {
        return new BankDto(bank.id(), bank.mfoCode(), bank.name());
    }
}
