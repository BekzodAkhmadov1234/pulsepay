package uz.pulsepay.transfer.domain.port.in;

import uz.pulsepay.transfer.domain.model.TransferSummary;

import java.util.List;
import java.util.UUID;

public interface ListTransferSummariesPort {
    List<TransferSummary> listSummariesByParticipant(UUID userId);
}
