package uz.pulsepay.merchant.domain.port.in;

public interface MerchantAuthPort {
    String login(String email, String password);
}
