package uz.pulsepay.transfer.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.transfer.domain.model.TransferSummary;
import uz.pulsepay.transfer.domain.port.in.ListTransferSummariesPort;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ListTransferSummariesUseCase implements ListTransferSummariesPort {

    private final TransferRepository transferRepository;

    public ListTransferSummariesUseCase(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferSummary> listSummariesByParticipant(UUID userId) {
        return transferRepository.findSummariesByParticipantId(userId);
    }
}
