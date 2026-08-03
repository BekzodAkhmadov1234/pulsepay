package uz.pulsepay.identity.adapter.out.jpa;

import org.springframework.stereotype.Repository;
import uz.pulsepay.identity.adapter.out.jpa.entity.OtpCodeEntity;
import uz.pulsepay.identity.domain.model.OtpCode;
import uz.pulsepay.identity.domain.model.OtpPurpose;
import uz.pulsepay.identity.domain.port.out.OtpCodeRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class OtpCodeJpaAdapter implements OtpCodeRepository {

    private final OtpCodeJpaRepository jpa;

    OtpCodeJpaAdapter(OtpCodeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OtpCode save(OtpCode otpCode) {
        return jpa.save(OtpCodeEntity.fromDomain(otpCode)).toDomain();
    }

    @Override
    public Optional<OtpCode> findLatestByUserIdAndPurpose(UUID userId, OtpPurpose purpose) {
        return jpa.findLatestByUserIdAndPurpose(userId, purpose).map(OtpCodeEntity::toDomain);
    }

    @Override
    public void markConsumed(UUID id) {
        jpa.markConsumed(id, Instant.now());
    }

    @Override
    public int incrementAttemptCount(UUID id) {
        jpa.incrementAttemptCount(id);
        return jpa.findAttemptCount(id);
    }
}
