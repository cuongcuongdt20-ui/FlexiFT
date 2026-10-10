package vn.flexift.app.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import vn.flexift.app.model.Direction;
import vn.flexift.app.model.IncomingOffer;
import vn.flexift.app.model.PeerInfo;
import vn.flexift.app.model.TransferSnapshot;
import vn.flexift.app.model.TransferState;
import vn.flexift.app.service.FakeTransferService;
import vn.flexift.app.service.TransferListener;
import vn.flexift.app.service.TransferService;
import vn.flexift.app.util.Formatters;
import vn.flexift.app.util.HostPort;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cửa sổ chính (FR-16): chờ hoặc nối tới, danh sách máy đã nối, chọn file hoặc kéo thả,
 * chọn chế độ, thanh tiến độ, tốc độ, số kênh đang dùng, hộp thoại nhận/từ chối (FR-05),
 * tạm dừng/tiếp tục/hủy (FR-09).
 *
 * Mọi hàm onXxx của lớp này chạy trên luồng JavaFX (được UiBridge bảo đảm).
 */
public class MainWindow implements TransferListener {

    private final Stage stage;
    private final TransferService service;
    private final UiBridge bridge;

    // Kết nối
    private final TextField deviceNameField = new TextField(defaultDeviceName());
    private final TextField listenPortField = new TextField(String.valueOf(HostPort.DEFAULT_PORT));
    private final Button listenButton = new Button("Chờ kết nối");
    private final Label addressLabel = new Label("Chưa chờ kết nối");
    private final TextField connectField = new TextField();
    private final Button connectButton = new Button("Nối tới");

    // Máy đã nối
    private final ObservableList<PeerInfo> peers = FXCollections.observableArrayList();
    private final ListView<PeerInfo> peerList = new ListView<>(peers);
    private final Button disconnectButton = new Button("Ngắt kết nối");

    // Phiên truyền
    private final ObservableList<TransferSnapshot> transfers = FXCollections.observableArrayList();
    private final TableView<TransferSnapshot> table = new TableView<>(transfers);
    private final Button pauseButton = new Button("Tạm dừng");
    private final Button cancelButton = new Button("Hủy");
    private final Button clearButton = new Button("Xóa phiên đã xong");

    // Gửi file
    private final Label dropZone = new Label("Kéo thả file vào đây, hoặc bấm “Chọn file…”");
    private final Button chooseFilesButton = new Button("Chọn file…");
    private final ComboBox<vn.flexift.app.model.TransferMode> modeBox = new ComboBox<>();
    private final Spinner<Integer> channelSpinner = new Spinner<>(2, 8, 4);
    private final Spinner<Integer> limitSpinner = new Spinner<>(0, 1000, 0);
    private final Button sendButton = new Button("Gửi");
    private final List<File> chosenFiles = new ArrayList<>();

    // Nhận
    private Path saveDir = Path.of(System.getProperty("user.home"), "FlexiFT-Received");
    private final Button saveDirButton = new Button();

    private final Label statusLabel = new Label("Sẵn sàng.");

    private final ArrayDeque<IncomingOffer> offerQueue = new ArrayDeque<>();
    private boolean offerDialogOpen;

    public MainWindow(Stage stage, TransferService service) {
        this.stage = stage;
        this.service = service;
        this.bridge = new UiBridge(this);
        service.setListener(bridge);
        buildUi();
    }

    public void show() {
        bridge.start();
        stage.show();
    }

    // ------------------------------------------------------------------ dựng giao diện

    private void buildUi() {
        BorderPane root = new BorderPane();
        root.setTop(new VBox(buildMenu(), buildConnectionBar()));
        root.setLeft(buildPeerPane());
        root.setCenter(buildTransferPane());
        root.setBottom(new VBox(buildSendPane(), buildStatusBar()));

        Scene scene = new Scene(root, 1100, 680);
        var css = getClass().getResource("/flexift.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setTitle("FlexiFT — truyền file song song qua TCP");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> {
            if (hasActiveTransfer() && !confirm("Còn phiên đang truyền. Thoát và hủy các phiên này?")) {
                e.consume();
                return;
            }
            bridge.stop();
            service.close();
        });
        refreshButtons();
    }

    private MenuBar buildMenu() {
        Menu tools = new Menu("Công cụ");
        MenuItem openDir = new MenuItem("Mở thư mục nhận");
        openDir.setOnAction(e -> openSaveDir());
        MenuItem exit = new MenuItem("Thoát");
        exit.setOnAction(e -> stage.fireEvent(new javafx.stage.WindowEvent(stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST)));
        tools.getItems().addAll(openDir);
        if (service instanceof FakeTransferService fake) {
            MenuItem sim = new MenuItem("Giả lập: có file gửi đến");
            sim.setOnAction(e -> fake.simulateIncomingOffer());
            tools.getItems().add(sim);
        }
        tools.getItems().add(exit);
        return new MenuBar(tools);
    }

    private VBox buildConnectionBar() {
        deviceNameField.setPrefColumnCount(10);
        deviceNameField.setPromptText("Tên máy");
        listenPortField.setPrefColumnCount(5);
        connectField.setPromptText("Địa chỉ máy kia, ví dụ 192.168.1.23:7777");
        connectField.setPrefColumnCount(26);
        HBox.setHgrow(addressLabel, Priority.ALWAYS);

        listenButton.setOnAction(e -> toggleListening());
        connectButton.setOnAction(e -> doConnect());
        connectField.setOnAction(e -> doConnect());
        deviceNameField.focusedProperty().addListener((o, was, now) -> {
            if (!now) {
                service.setDeviceName(deviceNameField.getText());
            }
        });
        service.setDeviceName(deviceNameField.getText());

        HBox listenRow = new HBox(8, new Label("Tên máy:"), deviceNameField, new Label("Cổng:"), listenPortField,
                listenButton, addressLabel);
        listenRow.setAlignment(Pos.CENTER_LEFT);
        HBox connectRow = new HBox(8, new Label("Nối tới:"), connectField, connectButton);
        connectRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, listenRow, connectRow);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("panel");
        return box;
    }

    private VBox buildPeerPane() {
        peerList.setPlaceholder(new Label("Chưa có máy nào nối.\nBấm “Chờ kết nối” hoặc nhập địa chỉ."));
        peerList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshButtons());
        disconnectButton.setOnAction(e -> {
            PeerInfo p = peerList.getSelectionModel().getSelectedItem();
            if (p != null) {
                service.disconnect(p.id());
            }
        });
        VBox.setVgrow(peerList, Priority.ALWAYS);
        Label title = new Label("Máy đã nối");
        title.getStyleClass().add("section-title");
        VBox box = new VBox(6, title, peerList, disconnectButton);
        box.setPadding(new Insets(10));
        box.setPrefWidth(250);
        return box;
    }

    private VBox buildTransferPane() {
        table.setPlaceholder(new Label("Chưa có phiên truyền nào."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TransferSnapshot, String> dirCol = col("Hướng", 60, s -> s.direction().label());
        TableColumn<TransferSnapshot, String> nameCol = col("File", 220, s -> Formatters.displayName(s.fileName()));
        TableColumn<TransferSnapshot, String> peerCol = col("Máy", 110, TransferSnapshot::peerName);
        TableColumn<TransferSnapshot, String> modeCol = col("Chế độ", 90, s -> s.mode().label());
        TableColumn<TransferSnapshot, String> sizeCol = col("Dung lượng", 90, s -> Formatters.bytes(s.size()));

        TableColumn<TransferSnapshot, TransferSnapshot> progCol = new TableColumn<>("Tiến độ");
        progCol.setPrefWidth(180);
        progCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        progCol.setCellFactory(c -> new ProgressCell());

        TableColumn<TransferSnapshot, String> speedCol = col("Tốc độ", 90, s -> s.state() == TransferState.RUNNING ? Formatters.speed(s.speedBps()) : "—");
        TableColumn<TransferSnapshot, String> etaCol = col("Còn lại", 70,
                s -> s.state() == TransferState.RUNNING ? Formatters.eta(s.size() - s.bytesDone(), s.speedBps()) : "—");
        TableColumn<TransferSnapshot, String> chCol = col("Kênh", 50, s -> s.state() == TransferState.RUNNING ? String.valueOf(s.channels()) : "—");
        TableColumn<TransferSnapshot, String> stCol = col("Trạng thái", 130,
                s -> s.message() == null ? s.state().label() : s.state().label() + ": " + s.message());

        table.getColumns().addAll(List.of(dirCol, nameCol, peerCol, modeCol, sizeCol, progCol, speedCol, etaCol, chCol, stCol));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshButtons());

        pauseButton.setOnAction(e -> togglePause());
        cancelButton.setOnAction(e -> {
            TransferSnapshot s = table.getSelectionModel().getSelectedItem();
            if (s != null && confirm("Hủy phiên truyền “" + Formatters.displayName(s.fileName()) + "”?")) {
                service.cancel(s.transferId());
            }
        });
        clearButton.setOnAction(e -> transfers.removeIf(s -> s.state().isFinished()));

        HBox actions = new HBox(8, pauseButton, cancelButton, clearButton);
        Label title = new Label("Phiên truyền");
        title.getStyleClass().add("section-title");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox box = new VBox(6, title, table, actions);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildSendPane() {
        dropZone.setMaxWidth(Double.MAX_VALUE);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setMinHeight(64);
        dropZone.getStyleClass().add("drop-zone");
        HBox.setHgrow(dropZone, Priority.ALWAYS);
        dropZone.setOnDragOver(this::onDragOver);
        dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("drop-active"));
        dropZone.setOnDragDropped(this::onDragDropped);

        chooseFilesButton.setOnAction(e -> chooseFiles());

        modeBox.getItems().setAll(vn.flexift.app.model.TransferMode.values());
        modeBox.getSelectionModel().select(vn.flexift.app.model.TransferMode.PARALLEL);
        modeBox.valueProperty().addListener((o, a, b) -> refreshButtons());
        channelSpinner.setEditable(true);
        channelSpinner.setPrefWidth(70);
        limitSpinner.setEditable(true);
        limitSpinner.setPrefWidth(80);
        limitSpinner.valueProperty().addListener((o, a, b) -> service.setSpeedLimit((b == null ? 0 : b) * 1024L * 1024L));

        sendButton.setDefaultButton(true);
        sendButton.setOnAction(e -> doSend());

        saveDirButton.setOnAction(e -> chooseSaveDir());
        updateSaveDirButton();

        HBox top = new HBox(10, dropZone, chooseFilesButton);
        top.setAlignment(Pos.CENTER_LEFT);
        HBox opts = new HBox(8, new Label("Chế độ:"), modeBox, new Label("Số kênh:"), channelSpinner,
                new Label("Giới hạn (MB/s, 0 = không):"), limitSpinner, sendButton);
        opts.setAlignment(Pos.CENTER_LEFT);
        HBox recv = new HBox(8, new Label("Lưu file nhận vào:"), saveDirButton);
        recv.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, top, opts, recv);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("panel");
        return box;
    }

    private HBox buildStatusBar() {
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(4, 10, 4, 10));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private static TableColumn<TransferSnapshot, String> col(String title, double width,
                                                              java.util.function.Function<TransferSnapshot, String> f) {
        TableColumn<TransferSnapshot, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(f.apply(cell.getValue())));
        c.setSortable(false);
        return c;
    }

    /** Ô tiến độ: thanh + phần trăm. */
    private static final class ProgressCell extends TableCell<TransferSnapshot, TransferSnapshot> {
        private final ProgressBar bar = new ProgressBar(0);
        private final Label text = new Label();
        private final HBox box = new HBox(6, bar, text);

        ProgressCell() {
            bar.setPrefWidth(110);
            box.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(TransferSnapshot s, boolean empty) {
            super.updateItem(s, empty);
            if (empty || s == null) {
                setGraphic(null);
                return;
            }
            bar.setProgress(s.state() == TransferState.WAITING_ACCEPT ? ProgressBar.INDETERMINATE_PROGRESS : s.fraction());
            text.setText(s.state() == TransferState.WAITING_ACCEPT ? "" : Formatters.percent(s.fraction()));
            setGraphic(box);
        }
    }

    // ------------------------------------------------------------------ hành động của người dùng

    private void toggleListening() {
        if (listenButton.getText().startsWith("Dừng")) {
            service.stopListening();
            return;
        }
        try {
            int port = HostPort.parsePort(listenPortField.getText());
            service.setDeviceName(deviceNameField.getText());
            listenButton.setDisable(true);
            setStatus("Đang mở cổng " + port + "…");
            service.startListening(port);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void doConnect() {
        try {
            HostPort hp = HostPort.parse(connectField.getText());
            service.setDeviceName(deviceNameField.getText());
            connectButton.setDisable(true);
            setStatus("Đang nối tới " + hp.host() + ":" + hp.port() + "…");
            service.connectTo(hp.host(), hp.port());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void chooseFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn file cần gửi");
        List<File> files = fc.showOpenMultipleDialog(stage);
        if (files != null && !files.isEmpty()) {
            setChosenFiles(files);
        }
    }

    private void setChosenFiles(List<File> files) {
        chosenFiles.clear();
        for (File f : files) {
            if (f.isFile()) {
                chosenFiles.add(f);
            }
        }
        if (chosenFiles.isEmpty()) {
            dropZone.setText("Chỉ gửi được file, không gửi được thư mục.");
        } else if (chosenFiles.size() == 1) {
            File f = chosenFiles.get(0);
            dropZone.setText(f.getName() + "  (" + Formatters.bytes(f.length()) + ")");
        } else {
            long total = chosenFiles.stream().mapToLong(File::length).sum();
            dropZone.setText(chosenFiles.size() + " file, tổng " + Formatters.bytes(total));
        }
        refreshButtons();
    }

    private void onDragOver(DragEvent e) {
        if (e.getDragboard().hasFiles()) {
            e.acceptTransferModes(TransferMode.COPY);
            if (!dropZone.getStyleClass().contains("drop-active")) {
                dropZone.getStyleClass().add("drop-active");
            }
        }
        e.consume();
    }

    private void onDragDropped(DragEvent e) {
        boolean ok = e.getDragboard().hasFiles();
        if (ok) {
            setChosenFiles(e.getDragboard().getFiles());
        }
        dropZone.getStyleClass().remove("drop-active");
        e.setDropCompleted(ok);
        e.consume();
    }

    private void doSend() {
        PeerInfo peer = peerList.getSelectionModel().getSelectedItem();
        if (peer == null) {
            showError("Chọn một máy trong danh sách “Máy đã nối” trước khi gửi.");
            return;
        }
        if (chosenFiles.isEmpty()) {
            showError("Chưa chọn file nào để gửi.");
            return;
        }
        var mode = modeBox.getValue();
        int channels = commitSpinner(channelSpinner, 2, 8);
        for (File f : chosenFiles) {
            service.sendFile(peer.id(), f.toPath(), mode, channels);
        }
        setStatus("Đã gửi đề nghị " + chosenFiles.size() + " file tới " + peer.name() + ", chờ bên kia bấm Nhận.");
        chosenFiles.clear();
        dropZone.setText("Kéo thả file vào đây, hoặc bấm “Chọn file…”");
        refreshButtons();
    }

    /** Spinner cho gõ tay: đọc lại chữ đã gõ, kẹp vào khoảng cho phép. */
    private static int commitSpinner(Spinner<Integer> sp, int min, int max) {
        try {
            int v = Integer.parseInt(sp.getEditor().getText().strip());
            v = Math.max(min, Math.min(max, v));
            sp.getValueFactory().setValue(v);
            return v;
        } catch (NumberFormatException e) {
            sp.getValueFactory().setValue(sp.getValue() == null ? min : sp.getValue());
            return sp.getValue();
        }
    }

    private void togglePause() {
        TransferSnapshot s = table.getSelectionModel().getSelectedItem();
        if (s == null) {
            return;
        }
        if (s.state() == TransferState.RUNNING) {
            service.pause(s.transferId());
        } else if (s.state() == TransferState.PAUSED) {
            service.resume(s.transferId());
        }
    }

    private void chooseSaveDir() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Chọn thư mục lưu file nhận");
        File cur = saveDir.toFile();
        if (cur.isDirectory()) {
            dc.setInitialDirectory(cur);
        }
        File d = dc.showDialog(stage);
        if (d != null) {
            saveDir = d.toPath();
            updateSaveDirButton();
        }
    }

    private void updateSaveDirButton() {
        saveDirButton.setText(saveDir.toString());
    }

    private void openSaveDir() {
        File d = saveDir.toFile();
        if (!d.isDirectory() && !d.mkdirs()) {
            showError("Không tạo được thư mục " + d);
            return;
        }
        new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().open(d);
            } catch (Exception | Error ex) {
                Platform.runLater(() -> showError("Không mở được thư mục. Đường dẫn: " + d));
            }
        }, "open-dir").start();
    }

    private void refreshButtons() {
        boolean hasPeer = peerList.getSelectionModel().getSelectedItem() != null;
        disconnectButton.setDisable(!hasPeer);
        sendButton.setDisable(!hasPeer || chosenFiles.isEmpty());
        channelSpinner.setDisable(modeBox.getValue() != vn.flexift.app.model.TransferMode.PARALLEL);

        TransferSnapshot s = table.getSelectionModel().getSelectedItem();
        boolean canPause = s != null && (s.state() == TransferState.RUNNING || s.state() == TransferState.PAUSED);
        pauseButton.setDisable(!canPause);
        pauseButton.setText(s != null && s.state() == TransferState.PAUSED ? "Tiếp tục" : "Tạm dừng");
        cancelButton.setDisable(s == null || s.state().isFinished());
    }

    private boolean hasActiveTransfer() {
        return transfers.stream().anyMatch(t -> !t.state().isFinished());
    }

    // ------------------------------------------------------------------ sự kiện từ lõi (đã ở luồng JavaFX)

    @Override
    public void onListening(String address) {
        listenButton.setDisable(false);
        listenButton.setText("Dừng chờ");
        listenPortField.setDisable(true);
        addressLabel.setText("Đang chờ tại  " + address + "  — đọc địa chỉ này cho máy kia");
        setStatus("Đang chờ kết nối tại " + address);
    }

    @Override
    public void onListeningStopped() {
        listenButton.setDisable(false);
        listenButton.setText("Chờ kết nối");
        listenPortField.setDisable(false);
        addressLabel.setText("Chưa chờ kết nối");
        setStatus("Đã dừng chờ kết nối.");
    }

    @Override
    public void onPeerConnected(PeerInfo peer) {
        connectButton.setDisable(false);
        peers.add(peer);
        if (peerList.getSelectionModel().isEmpty()) {
            peerList.getSelectionModel().select(peer);
        }
        connectField.clear();
        setStatus("Đã nối với " + peer.name() + " (" + peer.address() + ").");
    }

    @Override
    public void onPeerDisconnected(String peerId, String reason) {
        peers.removeIf(p -> p.id().equals(peerId));
        setStatus("Một máy đã ngắt kết nối: " + reason);
        refreshButtons();
    }

    @Override
    public void onTransferUpdate(TransferSnapshot snap) {
        for (int i = 0; i < transfers.size(); i++) {
            if (transfers.get(i).transferId().equals(snap.transferId())) {
                transfers.set(i, snap);
                refreshButtons();
                notifyFinished(snap);
                return;
            }
        }
        transfers.add(snap);
        refreshButtons();
    }

    private void notifyFinished(TransferSnapshot s) {
        if (s.state() == TransferState.COMPLETED) {
            setStatus((s.direction() == Direction.SEND ? "Đã gửi xong " : "Đã nhận xong ") + Formatters.displayName(s.fileName()) + ".");
        } else if (s.state() == TransferState.DECLINED) {
            setStatus(s.peerName() + " đã từ chối " + Formatters.displayName(s.fileName()) + ".");
        } else if (s.state() == TransferState.FAILED) {
            setStatus("Lỗi khi truyền " + Formatters.displayName(s.fileName()) + (s.message() == null ? "" : ": " + s.message()));
        }
    }

    @Override
    public void onOffer(IncomingOffer offer) {
        offerQueue.add(offer);
        showNextOffer();
    }

    @Override
    public void onError(String message) {
        listenButton.setDisable(false);
        connectButton.setDisable(false);
        setStatus(message);
        showError(message);
    }

    // ------------------------------------------------------------------ hộp thoại

    /** Mỗi lần chỉ hiện một lời đề nghị; các lời đề nghị khác xếp hàng. Không tự nhận (NFR-05). */
    private void showNextOffer() {
        if (offerDialogOpen || offerQueue.isEmpty()) {
            return;
        }
        IncomingOffer o = offerQueue.poll();
        offerDialogOpen = true;

        ButtonType accept = new ButtonType("Nhận", ButtonBar.ButtonData.OK_DONE);
        ButtonType decline = new ButtonType("Từ chối", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "", accept, decline);
        a.initOwner(stage);
        a.setTitle("Có file gửi đến");
        a.setHeaderText(o.peerName() + " muốn gửi cho bạn một file");
        a.setContentText("Tên file:  " + Formatters.displayName(o.fileName())
                + "\nDung lượng:  " + Formatters.bytes(o.size())
                + "\nChế độ:  " + o.mode().label()
                + (o.resumable() ? "\nPhiên này sẽ được tiếp tục từ phần đã nhận." : "")
                + "\n\nLưu vào:  " + saveDir);
        a.setOnHidden(ev -> {
            offerDialogOpen = false;
            Optional<ButtonType> r = Optional.ofNullable(a.getResult());
            if (r.isPresent() && r.get() == accept) {
                service.accept(o.transferId(), saveDir);
            } else {
                service.decline(o.transferId());
                setStatus("Đã từ chối " + Formatters.displayName(o.fileName()) + ".");
            }
            showNextOffer();
        });
        a.show();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.initOwner(stage);
        a.setTitle("Lỗi");
        a.setHeaderText(null);
        a.show();
    }

    private boolean confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.initOwner(stage);
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    private void setStatus(String s) {
        statusLabel.setText(s);
    }

    private static String defaultDeviceName() {
        String u = System.getProperty("user.name", "");
        return "May-" + (u.isBlank() ? "FlexiFT" : u);
    }
}
