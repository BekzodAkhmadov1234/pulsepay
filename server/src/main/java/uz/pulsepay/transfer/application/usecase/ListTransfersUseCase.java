package uz.pulsepay.transfer.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pulsepay.transfer.domain.model.Transfer;
import uz.pulsepay.transfer.domain.port.in.ListTransfersPort;
import uz.pulsepay.transfer.domain.port.out.TransferRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ListTransfersUseCase implements ListTransfersPort {

    private final TransferRepository transferRepository;

    public ListTransfersUseCase(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transfer> listBySender(UUID senderId) {
        return transferRepository.findBySenderId(senderId);
    }
}
