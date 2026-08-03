package uz.pulsepay.shared.audit;

import java.util.UUID;

public final class AuditContext {

    private static final ThreadLocal<UUID> CURRENT_ADMIN = new ThreadLocal<>();

    private AuditContext() {}

    public static void setAdminId(UUID adminId) {
        CURRENT_ADMIN.set(adminId);
    }

    public static UUID getAdminId() {
        return CURRENT_ADMIN.get();
    }

    public static void clear() {
        CURRENT_ADMIN.remove();
    }
}
