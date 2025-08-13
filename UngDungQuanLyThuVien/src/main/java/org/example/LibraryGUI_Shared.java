package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class LibraryGUI_Shared {
    private final Library library;
    public LibraryGUI_Shared(Library lib) { this.library = lib; }

    /** Bản dùng chung của hàm returnDocument (copy từ LibraryGUI của bạn) */
    public void returnDocument() {
        LibraryUser lu = Session.getCurrentUser();
        if (lu == null) {
            JOptionPane.showMessageDialog(null, "No user logged in. Please login first.");
            return;
        }

        String[] columns = {"ID", "Title", "Type", "Author(s)"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);

        final java.util.List<LibraryItem> borrowedItems = new java.util.ArrayList<>();

        Runnable reloadBorrowedTable = () -> {
            borrowedItems.clear();
            model.setRowCount(0);
            try {
                UserDAO dao = new UserDAO();
                for (BorrowRecord br : dao.getBorrowRecords(lu.getUserId())) {
                    if (br.getReturnDate() == null && br.getBook() != null) {
                        LibraryItem it = br.getBook();
                        borrowedItems.add(it);
                        model.addRow(new Object[]{ it.getId(), it.getTitle(),
                                it.getClass().getSimpleName(), it.getAuthors() });
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error loading borrowed documents: " + ex.getMessage());
            }
        };

        reloadBorrowedTable.run();
        if (borrowedItems.isEmpty()) {
            JOptionPane.showMessageDialog(null, "You have no active borrowed documents.");
            return;
        }

        JButton btnReturnOne = new JButton("Return Selected Document");
        btnReturnOne.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(null, "Please select a document to return.");
                return;
            }
            LibraryItem item = borrowedItems.get(row);
            try {
                if (!(item instanceof Book b)) {
                    throw new IllegalStateException("Unsupported item type: " + item.getClass().getSimpleName());
                }
                String idStr = b.getId();
                if (idStr == null || idStr.isBlank()) {
                    JOptionPane.showMessageDialog(null, "Book has no DB id. Please reload.");
                    return;
                }

                new UserDAO().returnBookByBookId(lu.getUserId(), Integer.parseInt(idStr));
                lu.returnItem(item);

                reloadBorrowedTable.run();
                JOptionPane.showMessageDialog(null, "Document returned successfully.");
            } catch (Exception ex) {
                String msg = String.valueOf(ex.getMessage());
                if (msg.contains("Already returned") || msg.contains("No active borrow")) {
                    reloadBorrowedTable.run();
                    JOptionPane.showMessageDialog(null, "This item was already returned. List refreshed.");
                } else {
                    JOptionPane.showMessageDialog(null, "DB error: " + msg);
                }
            }
        });

        JButton btnReturnAll = new JButton("Return All Documents");
        btnReturnAll.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    null, "Return ALL borrowed documents?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            int success = 0, failed = 0;

            java.util.List<LibraryItem> snapshot = new java.util.ArrayList<>(borrowedItems);
            for (LibraryItem item : snapshot) {
                try {
                    if (!(item instanceof Book b)) throw new IllegalStateException("Unsupported item type.");
                    String idStr = b.getId();
                    if (idStr == null || idStr.isBlank()) throw new IllegalStateException("Book has no DB id.");

                    new UserDAO().returnBookByBookId(lu.getUserId(), Integer.parseInt(idStr));
                    lu.returnItem(item);
                    success++;
                } catch (Exception ex2) {
                    failed++;
                }
            }

            reloadBorrowedTable.run();
            String msg = (failed == 0) ? "All documents returned." : "Returned: " + success + ", Failed: " + failed;
            JOptionPane.showMessageDialog(null, msg);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnReturnOne);
        buttonPanel.add(btnReturnAll);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(null, panel, "Borrowed Documents", JOptionPane.PLAIN_MESSAGE);
    }
}