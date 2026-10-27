import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ExpenseManager extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/expense_manager";
    private static final String DB_USER = System.getenv().getOrDefault("EXPENSE_DB_USER", "root");
    private static final String DB_PASSWORD = System.getenv("EXPENSE_DB_PASSWORD");

    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField categoryField = new JTextField(12);
    private final JTextField descriptionField = new JTextField(18);
    private final JTextField amountField = new JTextField(10);
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> filterBox = new JComboBox<>(
            new String[]{"All categories", "Food", "Travel", "Shopping", "Bills", "Education", "Health", "Other"});
    private final JLabel totalLabel = new JLabel("Total spending: ₹0.00");
    private final JLabel monthLabel = new JLabel("This month: ₹0.00");
    private final JLabel statusLabel = new JLabel("Ready");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Date", "Category", "Description", "Amount"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private int page = 0;
    private static final int PAGE_SIZE = 100;
    private int selectedId = -1;

    public ExpenseManager() {
        super("Expense Manager - B.C.Shervin");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 650));
        setLocationRelativeTo(null);
        buildInterface();
        refreshAll();
    }

    private Connection connect() throws SQLException {
        if (DB_PASSWORD == null || DB_PASSWORD.isBlank()) {
            throw new SQLException("Database password is not configured. Set EXPENSE_DB_PASSWORD before running.");
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void buildInterface() {
        Color background = new Color(244, 249, 244);
        Color green = new Color(72, 125, 91);
        getContentPane().setBackground(background);
        setLayout(new BorderLayout(12, 12));

        JLabel heading = new JLabel("Expense Manager", SwingConstants.LEFT);
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(green);
        JLabel subtitle = new JLabel("Track your spending calmly and clearly");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setBackground(background);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 4, 18));
        titlePanel.add(heading);
        titlePanel.add(subtitle);
        add(titlePanel, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(background);
        center.setBorder(BorderFactory.createEmptyBorder(0, 18, 10, 18));
        add(center, BorderLayout.CENTER);

        JPanel entryPanel = new JPanel(new GridBagLayout());
        entryPanel.setBackground(Color.WHITE);
        entryPanel.setBorder(BorderFactory.createTitledBorder("Expense details"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        addField(entryPanel, c, 0, 0, "Date (YYYY-MM-DD)", dateField);
        addField(entryPanel, c, 2, 0, "Category", categoryField);
        addField(entryPanel, c, 0, 1, "Description", descriptionField);
        addField(entryPanel, c, 2, 1, "Amount (₹)", amountField);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setBackground(Color.WHITE);
        JButton addButton = button("Add expense", green);
        JButton updateButton = button("Update selected", new Color(69, 105, 150));
        JButton deleteButton = button("Delete selected", new Color(164, 77, 77));
        JButton clearButton = button("Clear form", new Color(110, 117, 110));
        actions.add(addButton);
        actions.add(updateButton);
        actions.add(deleteButton);
        actions.add(clearButton);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        entryPanel.add(actions, c);
        center.add(entryPanel, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new BorderLayout(8, 8));
        listPanel.setBackground(background);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchPanel.setBackground(background);
        searchPanel.add(new JLabel("Search"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Category"));
        searchPanel.add(filterBox);
        JButton searchButton = button("Search", green);
        JButton resetButton = button("Show all", new Color(110, 117, 110));
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);
        listPanel.add(searchPanel, BorderLayout.NORTH);

        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        listPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBackground(background);
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 6));
        summaryPanel.setBackground(background);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        summaryPanel.add(totalLabel);
        summaryPanel.add(monthLabel);

        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        paging.setBackground(background);
        JButton previousButton = button("Previous", new Color(110, 117, 110));
        JButton nextButton = button("Next", new Color(110, 117, 110));
        paging.add(previousButton);
        paging.add(nextButton);
        bottom.add(summaryPanel, BorderLayout.NORTH);
        bottom.add(paging, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        listPanel.add(bottom, BorderLayout.SOUTH);
        center.add(listPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> addExpense());
        updateButton.addActionListener(e -> updateExpense());
        deleteButton.addActionListener(e -> deleteExpense());
        clearButton.addActionListener(e -> clearForm());
        searchButton.addActionListener(e -> {
            page = 0;
            loadExpenses();
        });
        searchField.addActionListener(e -> {
            page = 0;
            loadExpenses();
        });
        resetButton.addActionListener(e -> {
            searchField.setText("");
            filterBox.setSelectedIndex(0);
            page = 0;
            loadExpenses();
        });
        previousButton.addActionListener(e -> {
            if (page > 0) {
                page--;
                loadExpenses();
            }
        });
        nextButton.addActionListener(e -> {
            if ((page + 1) * PAGE_SIZE < countFilteredRows()) {
                page++;
                loadExpenses();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.convertRowIndexToModel(table.getSelectedRow());
                selectedId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                dateField.setText(tableModel.getValueAt(row, 1).toString());
                categoryField.setText(tableModel.getValueAt(row, 2).toString());
                descriptionField.setText(tableModel.getValueAt(row, 3) == null ? "" : tableModel.getValueAt(row, 3).toString());
                amountField.setText(tableModel.getValueAt(row, 4).toString());
            }
        });
    }

    private void addField(JPanel panel, GridBagConstraints c, int x, int y, String label, JTextField field) {
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridx = x;
        c.gridy = y;
        panel.add(new JLabel(label), c);
        c.gridx = x + 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(field, c);
        c.weightx = 0;
    }

    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return b;
    }

    private void addExpense() {
        ExpenseInput input = readInput();
        if (input == null) return;
        String sql = "INSERT INTO expenses (expense_date, category, description, amount) VALUES (?, ?, ?, ?)";
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(input.date));
            ps.setString(2, input.category);
            ps.setString(3, input.description.isBlank() ? null : input.description);
            ps.setBigDecimal(4, input.amount);
            ps.executeUpdate();
            clearForm();
            page = 0;
            refreshAll();
            statusLabel.setText("Expense added successfully.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateExpense() {
        if (selectedId < 0) {
            showMessage("Select an expense from the table first.");
            return;
        }
        ExpenseInput input = readInput();
        if (input == null) return;
        String sql = "UPDATE expenses SET expense_date=?, category=?, description=?, amount=? WHERE expense_id=?";
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(input.date));
            ps.setString(2, input.category);
            ps.setString(3, input.description.isBlank() ? null : input.description);
            ps.setBigDecimal(4, input.amount);
            ps.setInt(5, selectedId);
            ps.executeUpdate();
            refreshAll();
            statusLabel.setText("Expense updated successfully.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteExpense() {
        if (selectedId < 0) {
            showMessage("Select an expense from the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Delete selected expense #" + selectedId + "?",
                "Confirm deletion", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement("DELETE FROM expenses WHERE expense_id=?")) {
            ps.setInt(1, selectedId);
            ps.executeUpdate();
            clearForm();
            refreshAll();
            statusLabel.setText("Expense deleted successfully.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private ExpenseInput readInput() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            String category = categoryField.getText().trim();
            String description = descriptionField.getText().trim();
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (category.isEmpty()) throw new IllegalArgumentException("Category is required.");
            if (category.length() > 100) throw new IllegalArgumentException("Category must be 100 characters or fewer.");
            if (description.length() > 255) throw new IllegalArgumentException("Description must be 255 characters or fewer.");
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
            if (amount.scale() > 2) throw new IllegalArgumentException("Amount can have at most two decimal places.");
            if (amount.precision() > 10) throw new IllegalArgumentException("Amount is too large.");
            return new ExpenseInput(date, category, description, amount);
        } catch (DateTimeParseException ex) {
            showMessage("Enter a valid date in YYYY-MM-DD format.");
        } catch (NumberFormatException ex) {
            showMessage("Enter a valid numeric amount, for example 250.50.");
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        }
        return null;
    }

    private void loadExpenses() {
        tableModel.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT expense_id, expense_date, category, description, amount FROM expenses WHERE 1=1");
        if (!searchField.getText().trim().isEmpty()) {
            sql.append(" AND (category LIKE ? OR description LIKE ?)");
        }
        String category = selectedCategory();
        if (category != null) sql.append(" AND category = ?");
        sql.append(" ORDER BY expense_date DESC, expense_id DESC LIMIT ? OFFSET ?");

        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            String search = "%" + searchField.getText().trim() + "%";
            if (!searchField.getText().trim().isEmpty()) {
                ps.setString(i++, search);
                ps.setString(i++, search);
            }
            if (category != null) ps.setString(i++, category);
            ps.setInt(i++, PAGE_SIZE);
            ps.setInt(i, page * PAGE_SIZE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getInt("expense_id"),
                            rs.getDate("expense_date"),
                            rs.getString("category"),
                            rs.getString("description"),
                            rs.getBigDecimal("amount")
                    });
                }
            }
            statusLabel.setText("Showing page " + (page + 1) + " · " + tableModel.getRowCount() + " records on this page");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private int countFilteredRows() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM expenses WHERE 1=1");
        if (!searchField.getText().trim().isEmpty()) sql.append(" AND (category LIKE ? OR description LIKE ?)");
        if (selectedCategory() != null) sql.append(" AND category=?");
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            String search = "%" + searchField.getText().trim() + "%";
            if (!searchField.getText().trim().isEmpty()) {
                ps.setString(i++, search);
                ps.setString(i++, search);
            }
            if (selectedCategory() != null) ps.setString(i, selectedCategory());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            showError(ex);
            return 0;
        }
    }

    private String selectedCategory() {
        String value = (String) filterBox.getSelectedItem();
        return value == null || value.equals("All categories") ? null : value;
    }

    private void refreshAll() {
        loadExpenses();
        refreshTotals();
    }

    private void refreshTotals() {
        String totalSql = "SELECT COALESCE(SUM(amount),0) FROM expenses";
        String monthSql = "SELECT COALESCE(SUM(amount),0) FROM expenses WHERE expense_date >= ? AND expense_date < ?";
        try (Connection con = connect();
             PreparedStatement totalPs = con.prepareStatement(totalSql);
             PreparedStatement monthPs = con.prepareStatement(monthSql)) {
            try (ResultSet rs = totalPs.executeQuery()) {
                if (rs.next()) totalLabel.setText("Total spending: ₹" + rs.getBigDecimal(1).toPlainString());
            }
            YearMonth currentMonth = YearMonth.now();
            monthPs.setDate(1, Date.valueOf(currentMonth.atDay(1)));
            monthPs.setDate(2, Date.valueOf(currentMonth.plusMonths(1).atDay(1)));
            try (ResultSet rs = monthPs.executeQuery()) {
                if (rs.next()) monthLabel.setText("This month: ₹" + rs.getBigDecimal(1).toPlainString());
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void clearForm() {
        selectedId = -1;
        table.clearSelection();
        dateField.setText(LocalDate.now().toString());
        categoryField.setText("");
        descriptionField.setText("");
        amountField.setText("");
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Expense Manager", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(SQLException ex) {
        statusLabel.setText("Database operation failed.");
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }

    private static class ExpenseInput {
        final LocalDate date;
        final String category;
        final String description;
        final BigDecimal amount;

        ExpenseInput(LocalDate date, String category, String description, BigDecimal amount) {
            this.date = date;
            this.category = category;
            this.description = description;
            this.amount = amount;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new ExpenseManager().setVisible(true);
        });
    }
}
