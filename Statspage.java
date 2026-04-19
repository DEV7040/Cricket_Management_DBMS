
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Statspage extends JFrame {

    public Statspage() {
        setTitle("IPL 2026 - Stats Dashboard");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(new Color(25, 25, 112));
        JLabel title = new JLabel("IPL 2026 POINTS TABLE");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        header.add(title);
        add(header, BorderLayout.NORTH);

        // 1. Removed "NRR" from the columns array
        String[] columns = {"Rank", "Team", "Played", "Won", "Lost", "Points"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));

        loadPoints(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton backBtn = new JButton("← Back to Schedule");
        backBtn.setFont(new Font("Arial", Font.BOLD, 16));
        backBtn.addActionListener(e -> {
            new Homepage().setVisible(true);
            dispose();
        });

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(new Font("Arial", Font.BOLD, 16));
        refreshBtn.addActionListener(e -> loadPoints(model));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(backBtn);
        buttonPanel.add(refreshBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadPoints(DefaultTableModel model) {
        model.setRowCount(0);
        // Query to load points table data
        String query = "SELECT t.team_name, p.played, p.won, p.lost, p.points "
                + "FROM points_table p "
                + "JOIN teams t ON p.team_id = t.team_id "
                + "ORDER BY p.points DESC";

        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(query)) {

            int rank = 1;
            int count = 0;
            while (rs.next()) {
                count++;
                model.addRow(new Object[]{
                    rank++,
                    rs.getString("team_name"),
                    rs.getInt("played"),
                    rs.getInt("won"),
                    rs.getInt("lost"),
                    rs.getInt("points")
                });
            }

            if (count == 0) {
                JOptionPane.showMessageDialog(this, "No teams found in points table. Add teams first and play matches to see points.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading points: " + e.getMessage());
        }
    }
}
