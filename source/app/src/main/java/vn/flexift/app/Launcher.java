package vn.flexift.app;

import javafx.application.Application;

/**
 * Lớp main của flexift.jar. Phải là lớp riêng, KHÔNG kế thừa Application,
 * nếu không JavaFX đòi module khi chạy từ file jar gộp (shade).
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
