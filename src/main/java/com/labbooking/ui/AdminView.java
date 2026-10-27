package com.labbooking.ui;

import com.labbooking.Main;
import com.labbooking.dao.BookingDao;
import com.labbooking.dao.InventoryDao;
import com.labbooking.model.BookingItem;
import com.labbooking.model.BookingRequest;
import com.labbooking.model.EquipmentUnit;
import com.labbooking.model.Material;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.labbooking.ui.StudentView.col;

// অ্যাডমিনের পুরো স্ক্রিন: রিকোয়েস্ট দেখা → approve/reject → issue → return,
// আর আলাদা ট্যাবে ইনভেন্টরি (নতুন যন্ত্র যোগ, সারানো যন্ত্র আবার চালু)।
// StudentView এর মতোই এক TabPane, FXML ছাড়া।
public class AdminView extends VBox {

    private final BookingDao bookings = new BookingDao();
    private final InventoryDao inventory = new InventoryDao();

    // তিন অবস্থা = কাজের তিন ধাপ; আলাদা তিন ট্যাব বানালে একই টেবিল তিনবার লিখতে হতো।
    private final ComboBox<String> status = new ComboBox<>();
    private final TableView<BookingRequest> requests = new TableView<>();
    private final TableView<BookingItem> items = new TableView<>();
    private final TableView<EquipmentUnit> heldUnits = new TableView<>();

    // এক টেবিলেই দুই রকম সারি: material (মোট কয়টা খালি) খুললে তার ইউনিটগুলো।
    // তাই টাইপ Object — প্রতিটা কলাম সারি দেখে ঠিক করে কী লিখবে।
    private final TreeTableView<Object> stock = new TreeTableView<>();
    private final ComboBox<Material> material = new ComboBox<>();
    private final TextField serialNo = new TextField();

    private final Label message = new Label();

    // SQLException ছুঁড়তে পারে এমন কাজ — তাই Runnable দিয়ে হয় না।
    private interface Action {
        void run() throws SQLException;
    }

    public AdminView() {
        message.setWrapText(true);

        status.getItems().addAll("PENDING", "APPROVED", "ISSUED");
        status.setValue("PENDING");
        status.setOnAction(e -> refresh());

        col(requests, "Request code", 150, BookingRequest::getRequestCode);
        col(requests, "Student", 160, BookingRequest::getStudentName);
        col(requests, "Purpose", 220, BookingRequest::getPurpose);
        col(requests, "Return date", 110, r -> String.valueOf(r.getReturnDate()));
        col(requests, "Status", 100, BookingRequest::getStatus);
        requests.setPlaceholder(new Label("No requests in this state"));
        // উপরে রিকোয়েস্ট বাছা হলে নিচের দুই টেবিল নিজেই ভরে যায়।
        requests.getSelectionModel().selectedItemProperty().addListener((obs, old, r) -> loadDetails(r));

        col(items, "Material", 300, BookingItem::getMaterialName);
        col(items, "Quantity", 90, i -> String.valueOf(i.getQuantity()));
        col(items, "Status", 110, BookingItem::getStatus);
        items.setPlaceholder(new Label("Select a request above"));

        col(heldUnits, "Material", 260, EquipmentUnit::getMaterialName);
        col(heldUnits, "Serial", 140, EquipmentUnit::getSerialNo);
        col(heldUnits, "Status", 110, EquipmentUnit::getStatus);
        heldUnits.setPlaceholder(new Label("No units held by this request"));
        // নষ্ট ইউনিটগুলো শুধু বেছে নিলেই হয় — প্রতি লাইনে চেকবক্স বসাতে
        // আলাদা cell factory লিখতে হতো, ফল একই।
        heldUnits.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button approveItem = new Button("Approve item");
        approveItem.setOnAction(e -> decideItem(true));
        Button rejectItem = new Button("Reject item");
        rejectItem.setOnAction(e -> decideItem(false));

        Button approveAll = new Button("Approve whole request");
        approveAll.setOnAction(e -> decideRequest(true));
        Button rejectAll = new Button("Reject whole request");
        rejectAll.setOnAction(e -> decideRequest(false));

        Button issue = new Button("Record issue");
        issue.setOnAction(e -> {
            BookingRequest r = selectedRequest("APPROVED");
            if (r != null) act("Issued " + r.getRequestCode(), () -> bookings.recordIssue(r.getId()));
        });

        Button recordReturn = new Button("Record return");
        recordReturn.setOnAction(e -> {
            BookingRequest r = selectedRequest("ISSUED");
            if (r == null) return;
            Set<Integer> damaged = new HashSet<>();
            for (EquipmentUnit u : heldUnits.getSelectionModel().getSelectedItems()) damaged.add(u.getId());
            // বাছা না হওয়া বাকি ইউনিটগুলো DAO নিজেই AVAILABLE করে দেয়।
            act("Returned " + r.getRequestCode() + " (" + damaged.size() + " damaged)",
                    () -> bookings.recordReturn(r.getId(), damaged));
        });

        VBox requestTab = new VBox(8,
                new HBox(8, new Label("Show requests with status"), status),
                requests,
                new Label("Items in the selected request"),
                items,
                new HBox(8, approveItem, rejectItem, approveAll, rejectAll),
                new Label("Units held by this request — select the damaged ones before recording a return"),
                heldUnits,
                new HBox(8, issue, recordReturn));
        requestTab.setPadding(new Insets(12));
        VBox.setVgrow(requests, Priority.ALWAYS);

        // listMaterials(null) এমনিতেই AVAILABLE গোনে (ড্রপডাউন ভরতে পড়াই হয়),
        // তাই এই সারসংক্ষেপে নতুন কোনো কোয়েরি লাগছে না।
        treeCol("Material", 340,
                m -> m.getSubject() + " — " + m, EquipmentUnit::getMaterialName);
        treeCol("Serial", 140, m -> "", EquipmentUnit::getSerialNo);
        // material সারিতে "কয়টা খালি", ইউনিট সারিতে সেই যন্ত্রের নিজের অবস্থা।
        treeCol("Available / Status", 160,
                m -> m.getAvailableCount() + " available", EquipmentUnit::getStatus);
        stock.setShowRoot(false);
        stock.setRoot(new TreeItem<>());
        stock.setPlaceholder(new Label("No materials yet"));

        Button add = new Button("Add unit");
        add.setOnAction(e -> {
            Material m = material.getValue();
            if (m == null) {
                error("Choose a material first");
                return;
            }
            // একই সিরিয়াল আগে থাকলে DAO পড়ার মতো মেসেজ দিয়ে ঠেকায়।
            act("Added " + m + " / " + serialNo.getText().trim(), () -> {
                inventory.addUnit(m.getId(), serialNo.getText());
                serialNo.clear();
            });
        });

        Button repaired = new Button("Mark selected unit repaired");
        repaired.setOnAction(e -> {
            TreeItem<Object> row = stock.getSelectionModel().getSelectedItem();
            // material সারি বেছে নিলে row.getValue() EquipmentUnit নয় — তখন u null।
            EquipmentUnit u = row != null && row.getValue() instanceof EquipmentUnit unit ? unit : null;
            if (u == null || !u.getStatus().equals("DAMAGED")) {
                error("Select a DAMAGED unit to mark it repaired");
                return;
            }
            act("Unit " + u.getSerialNo() + " is available again",
                    () -> inventory.setUnitStatus(u.getId(), "AVAILABLE"));
        });

        VBox inventoryTab = new VBox(8,
                new Label("Available quantity per material — expand a row to see its units"),
                stock,
                repaired,
                new HBox(8, new Label("Material"), material, new Label("Serial number"), serialNo, add));
        inventoryTab.setPadding(new Insets(12));
        VBox.setVgrow(stock, Priority.ALWAYS);

        Tab requestsTabPage = new Tab("Requests", requestTab);
        Tab inventoryTabPage = new Tab("Inventory", inventoryTab);
        TabPane tabs = new TabPane(requestsTabPage, inventoryTabPage);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Button logout = new Button("Logout");
        logout.setOnAction(e -> Main.logout());
        HBox header = new HBox(12,
                new Label(Main.currentUser.getFullName() + "  (" + Main.currentUser.getRole() + ")"),
                logout);
        header.setPadding(new Insets(12, 12, 0, 12));

        setSpacing(8);
        // StudentView এর মতোই: message সবার শেষে থাকলে লম্বা ট্যাব-কনটেন্টের চাপে
        // উইন্ডোর নিচের কিনারার বাইরে পড়ে যায়, তাই হেডারের ঠিক নিচে।
        getChildren().addAll(header, message, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        message.setMinHeight(Region.USE_PREF_SIZE);
        VBox.setMargin(message, new Insets(0, 12, 0, 12));

        refresh();
    }

    // একটা লাইন approve/reject। লাইনের সিদ্ধান্ত থেকেই রিকোয়েস্টের স্ট্যাটাস হিসাব হয়।
    private void decideItem(boolean approve) {
        BookingRequest r = selectedRequest("PENDING");
        if (r == null) return;
        BookingItem item = items.getSelectionModel().getSelectedItem();
        if (item == null) {
            error("Select an item line first");
            return;
        }
        act((approve ? "Approved " : "Rejected ") + item.getMaterialName(),
                () -> bookings.decideItem(item.getId(), approve));
    }

    private void decideRequest(boolean approve) {
        BookingRequest r = selectedRequest("PENDING");
        if (r == null) return;
        act((approve ? "Approved " : "Rejected ") + r.getRequestCode(),
                () -> bookings.decideRequest(r.getId(), approve));
    }

    // DAO স্ট্যাটাস হাতে ধরে বসায়, তাই ভুল অবস্থার রিকোয়েস্টে বাটন চাপলে
    // ভুল স্ট্যাটাস পাকা হয়ে যেত — তার আগেই এখানে আটকানো হয়।
    private BookingRequest selectedRequest(String needStatus) {
        BookingRequest r = requests.getSelectionModel().getSelectedItem();
        if (r == null) {
            error("Select a request first");
            return null;
        }
        if (!r.getStatus().equals(needStatus)) {
            error("That request is " + r.getStatus() + "; this action needs " + needStatus);
            return null;
        }
        return r;
    }

    // সব বাটনের একই মোড়ক: চালাও, মেসেজ দেখাও, পর্দা নতুন করে পড়ো।
    private void act(String okMessage, Action action) {
        try {
            action.run();
            refresh();
            info(okMessage);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // ভুল ডাটার মেসেজ DAO থেকেই আসে (ফাঁকা সিরিয়াল, ডুপ্লিকেট সিরিয়াল ইত্যাদি),
            // তাই নিয়ম দুইবার লেখা হয় না — শুধু লাল করে দেখানো হয়।
            error(ex.getMessage());
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
    }

    // StudentView এর মতোই: লাল মানে সমস্যা, সবুজ মানে কাজ হয়েছে। রং একমাত্র সংকেত নয়,
    // লেখাটাই বলে দেয় কী হয়েছে।
    // দুই রকম সারির জন্য দুইটা আলাদা ফাংশন — কলাম নিজেই বেছে নেয় কোনটা চালাবে।
    private void treeCol(String title, double width,
                         Function<Material, String> ofMaterial, Function<EquipmentUnit, String> ofUnit) {
        TreeTableColumn<Object, String> c = new TreeTableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(row -> {
            Object v = row.getValue().getValue();
            return new SimpleStringProperty(v instanceof Material m ? ofMaterial.apply(m)
                    : ofUnit.apply((EquipmentUnit) v));
        });
        stock.getColumns().add(c);
    }

    // প্রতিটা material এর নিচে তার ইউনিটগুলো বসে। খোলা সারি আর বাছাই করা
    // ইউনিট ধরে রাখা হয়, নাহলে প্রতিবার refresh এ গাছ বন্ধ হয়ে যেত।
    private void loadStockTree(List<Material> materials, List<EquipmentUnit> units) {
        Set<Integer> expanded = new HashSet<>();
        for (TreeItem<Object> row : stock.getRoot().getChildren()) {
            if (row.isExpanded()) expanded.add(((Material) row.getValue()).getId());
        }
        TreeItem<Object> selected = stock.getSelectionModel().getSelectedItem();
        int selectedUnit = selected != null && selected.getValue() instanceof EquipmentUnit u ? u.getId() : -1;

        stock.getRoot().getChildren().clear();
        for (Material m : materials) {
            TreeItem<Object> parent = new TreeItem<>(m);
            parent.setExpanded(expanded.contains(m.getId()));
            for (EquipmentUnit u : units) {
                if (u.getMaterialId() == m.getId()) parent.getChildren().add(new TreeItem<>(u));
            }
            stock.getRoot().getChildren().add(parent);
            if (selectedUnit < 0) continue;
            for (TreeItem<Object> child : parent.getChildren()) {
                if (((EquipmentUnit) child.getValue()).getId() == selectedUnit) {
                    stock.getSelectionModel().select(child);
                }
            }
        }
    }

    private void error(String text) {
        message.setStyle("-fx-text-fill: red;");
        message.setText(text);
    }

    private void info(String text) {
        message.setStyle("-fx-text-fill: green;");
        message.setText(text);
    }

    // একটা কাজের পর রিকোয়েস্টের স্ট্যাটাস আর ইউনিটের অবস্থা দুটোই বদলায়,
    // তাই পুরো স্ক্রিনই আবার পড়া হয় — কোয়েরিগুলো ছোট, বাসি তথ্য থাকার ঝুঁকি নেই।
    private void refresh() {
        BookingRequest selected = requests.getSelectionModel().getSelectedItem();
        try {
            requests.getItems().setAll(bookings.listByStatus(status.getValue()));
            List<Material> materials = inventory.listMaterials(null);
            loadStockTree(materials, inventory.listUnits());
            material.getItems().setAll(materials);
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
            return;
        }
        // আগের রিকোয়েস্ট এখনো তালিকায় থাকলে আবার বেছে দেওয়া হয়, নাহলে
        // প্রতিটা আইটেম approve করার পর অ্যাডমিনকে আবার সেটা খুঁজতে হতো।
        if (selected == null) return;
        for (BookingRequest r : requests.getItems()) {
            if (r.getId() == selected.getId()) {
                requests.getSelectionModel().select(r);
                return;
            }
        }
    }

    private void loadDetails(BookingRequest request) {
        items.getItems().clear();
        heldUnits.getItems().clear();
        if (request == null) return;
        try {
            items.getItems().setAll(bookings.listItems(request.getId()));
            heldUnits.getItems().setAll(bookings.listUnitsForRequest(request.getId()));
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
    }
}
