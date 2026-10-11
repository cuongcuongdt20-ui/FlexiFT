package vn.flexift.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import vn.flexift.app.service.ServiceFactory;
import vn.flexift.app.service.TransferService;
import vn.flexift.app.ui.MainWindow;

public class MainApp extends Application {
    private TransferService service;

    @Override
    public void start(Stage stage) {
        service = ServiceFactory.create();
        new MainWindow(stage, service).show();
    }

    @Override
    public void stop() {
        if (service != null) {
            service.close();
        }
        Platform.exit();
    }
}
