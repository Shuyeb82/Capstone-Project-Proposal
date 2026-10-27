package com.labbooking.ui;

import com.labbooking.Main;
import com.labbooking.dao.BookingDao;
import com.labbooking.dao.InventoryDao;
import com.labbooking.model.BookingItem;
import com.labbooking.model.BookingRequest;
import com.labbooking.model.Material;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.function.Function;

// ছাত্রের পুরো স্ক্রিন: স্টক দেখা → কার্টে যোগ → সাবমিট, আর My Bookings।
// দুইটা আলাদা ফাইল না করে এক TabPane — কার্ট আর তালিকা একই সেশনের দুই দিক।
public class StudentView extends VBox {

    private final InventoryDao inventory = new InventoryDao();
    private final BookingDao bookings = new BookingDao();

    private final ComboBox<String> subject = new ComboBox<>();
    private final TableView<Material> stock = new TableView<>();
    // Spinner নিজেই 1–99 এর বাইরে যেতে দেয় না, তাই সংখ্যা যাচাইয়ের কোড লাগে না।
    private final Spinner<Integer> quantity = new Spinner<>(1, 99, 1);

    // কার্ট মেমরিতে থাকা List<BookingItem> — observable হওয়ায় লিস্ট বদলালে টেবিল নিজেই আপডেট হয়।
    private final ObservableList<BookingItem> cart = FXCollections.observableArrayList();
    private final TableView<BookingItem> cartTable = new TableView<>();
    private final TextField purpose = new TextField();
    private final DatePicker returnDate = new DatePicker();

    private final TableView<BookingRequest> myRequests = new TableView<>();
    private final TableView<BookingItem> myItems = new TableView<>();

    private final Label message = new Label();

    public StudentView() {
        message.setWrapText(true);

        col(stock, "Subject", 140, Material::getSubject);
        col(stock, "Material", 200, Material::getName);
        col(stock, "Variant", 160, Material::getVariant);
        // ০ হলেও একই লেখা — README এর "0 available" শর্ত এখানেই পূরণ হয়।
        col(stock, "Availability", 110, m -> m.getAvailableCount() + " available");
        stock.setPlaceholder(new Label("No materials for this subject"));

        cartTable.setItems(cart);
        col(cartTable, "Material", 300, BookingItem::getMaterialName);
        col(cartTable, "Quantity", 90, i -> String.valueOf(i.getQuantity()));
        cartTable.setPlaceholder(new Label("Request list is empty"));

        col(myRequests, "Request code", 160, BookingRequest::getRequestCode);
        col(myRequests, "Purpose", 240, BookingRequest::getPurpose);
        col(myRequests, "Return date", 110, r -> String.valueOf(r.getReturnDate()));
        col(myRequests, "Status", 100, BookingRequest::getStatus);
        myRequests.setPlaceholder(new Label("No requests yet"));
        // উপরে কোন রিকোয়েস্ট বাছা হলো, নিচে তার আইটেমগুলো।
        myRequests.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> loadItems(selected));

        col(myItems, "Material", 300, BookingItem::getMaterialName);
        col(myItems, "Quantity", 90, i -> String.valueOf(i.getQuantity()));
        col(myItems, "Status", 110, BookingItem::getStatus);
        myItems.setPlaceholder(new Label("Select a request above"));

        // সাবজেক্ট বদলানো = নতুন কাজ শুরু, তাই কার্ট খালি হয় (README এর নিয়ম)।
        subject.setOnAction(e -> loadStock(subject.getValue(), false));

        Button refresh = new Button("Refresh stock");
        refresh.setOnAction(e -> loadStock(subject.getValue(), true));

        // সাবজেক্ট ফিল্টার বাদ দিয়ে পুরো ল্যাব — কার্ট ঠিক থাকে, তাই ড্রপডাউনের
        // ভেতরে একটা অপশন হিসেবে রাখা যায়নি (ওতে কার্ট মুছে যেত)।
        Button showAll = new Button("Show all lab stock");
        showAll.setOnAction(e -> loadStock(null, true));

        Button add = new Button("Add to request");
        add.setOnAction(e -> add());

        Button remove = new Button("Remove selected item");
        remove.setOnAction(e -> {
            BookingItem selected = cartTable.getSelectionModel().getSelectedItem();
            if (selected != null) cart.remove(selected);
        });

        Button submit = new Button("Submit request");
        submit.setOnAction(e -> submit());

        VBox browse = new VBox(8,
                new HBox(8, new Label("Subject"), subject, refresh, showAll),
                stock,
                new HBox(8, new Label("Quantity"), quantity, add),
                new Label("Request list"),
                cartTable,
                remove,
                new Label("Purpose"), purpose,
                new Label("Return date (must be in the future)"), returnDate,
                submit);
        browse.setPadding(new Insets(12));
        VBox.setVgrow(stock, Priority.ALWAYS);

        VBox mine = new VBox(8, new Label("My requests"), myRequests, new Label("Items"), myItems);
        mine.setPadding(new Insets(12));
        VBox.setVgrow(myRequests, Priority.ALWAYS);

        Tab browseTab = new Tab("Browse & request", browse);
        Tab mineTab = new Tab("My bookings", mine);
        TabPane tabs = new TabPane(browseTab, mineTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        // ট্যাবে ফিরলে পুরোনো তালিকা নয়, নতুন করে পড়া হয় (অ্যাডমিন মাঝে approve করে থাকতে পারে)।
        mineTab.setOnSelectionChanged(e -> { if (mineTab.isSelected()) loadMyBookings(); });

        Button logout = new Button("Logout");
        logout.setOnAction(e -> Main.logout());
        HBox header = new HBox(12,
                new Label(Main.currentUser.getFullName() + "  (" + Main.currentUser.getRole() + ")"),
                logout);
        header.setPadding(new Insets(12, 12, 0, 12));

        setSpacing(8);
        // message হেডারের নিচে, tabs এর উপরে। সবার শেষে রাখলে দেখাই যেত না:
        // ট্যাবের ভেতরের কনটেন্ট (দুইটা টেবিল + ফিল্ড + বাটন) 600px উইন্ডোর চেয়ে
        // লম্বা, আর tabs এ Priority.ALWAYS — তাই শেষ child টা নিচের কিনারার বাইরে
        // পড়ে যেত। স্টক কম পড়ার মেসেজ সেট হতো, কিন্তু পর্দায় আসত না।
        getChildren().addAll(header, message, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        // USE_PREF_SIZE = জায়গা কম পড়লেও VBox এটাকে চেপে শূন্য উচ্চতা করতে পারবে না।
        message.setMinHeight(Region.USE_PREF_SIZE);
        VBox.setMargin(message, new Insets(0, 12, 0, 12));

        try {
            subject.getItems().setAll(inventory.listSubjects());
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
        loadStock(null, true);
    }

    // চার জায়গায় একই তিন লাইন লিখতে হয় না — কোন টেবিলে, কী শিরোনাম, কোন লেখা, ব্যস।
    // কলামটা নিজেই টেবিলে বসায়: addAll(col(...), col(...)) লিখলে জাভা varargs এর জন্য
    // জেনেরিক অ্যারে বানাতে বাধ্য হয় আর "type safety" ওয়ার্নিং দেয়।
    // private নয়, কারণ AdminView এর টেবিলগুলোও এটাই ব্যবহার করে (static import)।
    static <T> void col(TableView<T> table, String title, double width, Function<T, String> value) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue())));
        table.getColumns().add(c);
    }

    // subjectFilter null দিলে পুরো ল্যাবের স্টক।
    private void loadStock(String subjectFilter, boolean keepCart) {
        // সাবজেক্ট বদলানো মানে নতুন কাজ — কার্টের সাথে পুরোনো মেসেজও মুছে যায়।
        // keepCart=true তে মেসেজ মোছা হয় না: সাবমিটের পর স্টক আবার পড়তে গিয়ে
        // এইমাত্র দেখানো এরর/সফল লেখাটাই মুছে যেত, ছাত্র কিছুই বুঝত না।
        if (!keepCart) {
            cart.clear();
            message.setText("");
        }
        try {
            stock.getItems().setAll(inventory.listMaterials(subjectFilter));
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
    }

    // এক জায়গায় রং ঠিক করা: লাল মানে সমস্যা, সবুজ মানে কাজ হয়েছে।
    // শুধু কালো লেখা বাকি Label গুলোর মধ্যে চোখেই পড়ে না। রং একমাত্র সংকেত নয় —
    // লেখাটাই নিজে বলে দেয় কী হয়েছে, তাই রং আলাদা করতে না পারলেও বোঝা যায়।
    private void error(String text) {
        message.setStyle("-fx-text-fill: red;");
        message.setText(text);
    }

    private void info(String text) {
        message.setStyle("-fx-text-fill: green;");
        message.setText(text);
    }

    private void add() {
        Material selected = stock.getSelectionModel().getSelectedItem();
        if (selected == null) {
            error("Select a material from the table first");
            return;
        }
        // একটাও খালি নেই মানে চাওয়ার কিছু নেই — সাবমিট পর্যন্ত অপেক্ষা করার দরকার কী।
        if (selected.getAvailableCount() == 0) {
            error(selected + " has 0 available and cannot be added");
            return;
        }
        BookingDao.addToCart(cart, selected, quantity.getValue());
        info(selected + " x" + quantity.getValue() + " added to the request list");
    }

    private void submit() {
        try {
            String code = bookings.submit(Main.currentUser.getId(), purpose.getText(),
                    returnDate.getValue(), cart);
            cart.clear();
            purpose.clear();
            returnDate.setValue(null);
            // সাবমিটের পর ইউনিটগুলো BOOKED, তাই দেখানো সংখ্যাও বদলাবে।
            // মেসেজ সবার শেষে — তালিকা পড়ার কাজ আগে সেরে নেওয়া হয়।
            loadStock(subject.getValue(), true);
            info("Request " + code + " submitted. Check My bookings for its status.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // স্টক কম পড়লে DAO পুরোটা রোলব্যাক করে, তাই কার্ট অছোঁয়া রেখে দেওয়া হয় —
            // ছাত্র সংখ্যা কমিয়ে আবার চেষ্টা করতে পারে।
            loadStock(subject.getValue(), true);
            error(ex.getMessage());
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
    }

    private void loadMyBookings() {
        try {
            myRequests.getItems().setAll(bookings.listByStudent(Main.currentUser.getId()));
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
    }

    private void loadItems(BookingRequest request) {
        myItems.getItems().clear();
        if (request == null) return;
        try {
            myItems.getItems().setAll(bookings.listItems(request.getId()));
        } catch (SQLException ex) {
            error("Database error: " + ex.getMessage());
        }
    }
}
