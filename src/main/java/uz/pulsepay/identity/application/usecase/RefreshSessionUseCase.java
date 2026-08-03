package uz.pulsepay.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.identity.domain.model.RefreshToken;
import uz.pulsepay.identity.domain.model.Session;
import uz.pulsepay.identity.domain.port.in.RefreshSessionPort;
import uz.pulsepay.identity.domain.port.out.RefreshTokenRepository;
import uz.pulsepay.identity.domain.port.out.SessionRepository;
import uz.pulsepay.shared.exception.DomainException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class RefreshSessionUseCase implements RefreshSessionPort {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;

    public RefreshSessionUseCase(RefreshTokenRepository refreshTokenRepository,
                                 SessionRepository sessionRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional
    public Session refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .filter(RefreshToken::isValid)
                .orElseThrow(() -> new DomainException("Invalid or expired refresh token"));
        return sessionRepository.findById(token.sessionId())
                .filter(Session::isValid)
                .orElseThrow(() -> new DomainException("Associated session is revoked or expired"));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
