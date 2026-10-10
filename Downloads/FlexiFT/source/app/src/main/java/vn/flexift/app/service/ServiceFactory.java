package vn.flexift.app.service;

/**
 * Chỗ DUY NHẤT quyết định giao diện dùng lõi thật hay lõi giả.
 * Khi lõi sẵn sàng: return new CoreTransferService();
 */
public final class ServiceFactory {
    private ServiceFactory() {
    }

    public static TransferService create() {
        return new FakeTransferService();
    }
}
