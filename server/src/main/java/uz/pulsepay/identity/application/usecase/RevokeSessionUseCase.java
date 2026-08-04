package uz.pulsepay.identity.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.identity.domain.model.Session;
import uz.pulsepay.identity.domain.port.in.RevokeSessionPort;
import uz.pulsepay.identity.domain.port.out.RefreshTokenRepository;
import uz.pulsepay.identity.domain.port.out.SessionRepository;
import uz.pulsepay.shared.exception.DomainException;
import uz.pulsepay.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.UUID;

@Service
public class RevokeSessionUseCase implements RevokeSessionPort {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RevokeSessionUseCase(SessionRepository sessionRepository,
                                RefreshTokenRepository refreshTokenRepository) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public void revoke(UUID sessionId, UUID requestingUserId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (!session.userId().equals(requestingUserId)) {
            throw new DomainException("Cannot revoke another user's session");
        }
        Session revoked = new Session(session.id(), session.userId(), session.deviceId(),
                session.ipAddress(), session.createdAt(), session.expiresAt(), Instant.now());
        sessionRepository.save(revoked);
        refreshTokenRepository.revokeAllForSession(sessionId);
    }
}
