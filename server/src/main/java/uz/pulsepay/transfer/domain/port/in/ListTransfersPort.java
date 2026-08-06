package uz.pulsepay.transfer.domain.port.in;

import uz.pulsepay.transfer.domain.model.Transfer;

import java.util.List;
import java.util.UUID;

public interface ListTransfersPort {
    List<Transfer> listBySender(UUID senderId);
}
