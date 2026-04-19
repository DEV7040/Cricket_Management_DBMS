
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

/**
 * HOME PAGE ───────── • Add Teams • Add Players to a team • Schedule a Match
 * (pick two teams + date) • View schedule → click a row to open LiveMatchPage
 */
public class Homepage extends JFrame {

    // ── colours ──────────────────────────────────────────────────────────────
    // Deep Obsidian & Purple Base (More "Cinematic" than Slate)
    private static final Color BG = new Color(7, 10, 15);        // Near black for maximum OLED contrast
    private static final Color CARD = new Color(26, 26, 46);      // Deep Midnight Blue/Purple
    private static final Color BORDER = new Color(43, 45, 66);    // Muted Indigo for subtle separation

// High-Voltage Typography 
    private static final Color TEXT = new Color(0, 200, 0);   // Crisp Off-White
    private static final Color SUBTEXT = new Color(154, 165, 177); // Cool Grey

// Neon Accents
    private static final Color ACCENT = new Color(233, 69, 96);    // "Cyber" Pink-Red (Primary Action)
    private static final Color BTN_TEAL = new Color(15, 212, 191); // Electric Teal (Success/Secondary)
    private static final Color BTN_VIOLET = new Color(155, 89, 182); // Amethyst (Info/Links)

    // ── components ───────────────────────────────────────────────────────────
    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> cmbTeams;      // for "add player" section
    private JComboBox<String> cmbHomeTeam, cmbAwayTeam;
    private JTextField txtTeamName, txtPlayerName;
    private JSpinner spinnerDate;

    public Homepage() {
        setTitle("Cricket Match Management – Home");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);

        setVisible(true);
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel logo = new JLabel("Cricket Match Management");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        logo.setForeground(ACCENT);

        JLabel sub = new JLabel("Schedule • Live • Stats");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(SUBTEXT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(logo);
        left.add(sub);

        JButton btnStats = styledButton("View Points Table", BTN_VIOLET);
        btnStats.addActionListener(e -> {
            new Statspage().setVisible(true);
            dispose();
        });

        p.add(left, BorderLayout.WEST);
        p.add(btnStats, BorderLayout.EAST);
        return p;
    }

    // ── MAIN PANEL (split left / right) ───────────────────────────────────────
    private JPanel buildMainPanel() {
        JPanel main = new JPanel(new GridLayout(1, 2, 12, 0));
        main.setBackground(BG);
        main.setBorder(new EmptyBorder(12, 12, 12, 12));
        main.add(buildLeftPanel());
        main.add(buildSchedulePanel());
        return main;
    }

    // ── LEFT: add team / player / schedule ────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);

        p.add(buildAddTeamCard());
        p.add(Box.createVerticalStrut(10));
        p.add(buildAddPlayerCard());
        p.add(Box.createVerticalStrut(10));
        p.add(buildScheduleCard());
        return p;
    }

    // ── Card: Add Team ────────────────────────────────────────────────────────
    private JPanel buildAddTeamCard() {
        JPanel card = card("Add Team");
        txtTeamName = styledField("e.g. India");

        JButton btn = styledButton("Add Team", BTN_TEAL);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.addActionListener(e -> addTeam());

        card.add(label("Team Name:"));
        card.add(Box.createVerticalStrut(4));
        card.add(txtTeamName);
        card.add(Box.createVerticalStrut(8));
        card.add(btn);
        return card;
    }

    // ── Card: Add Player ─────────────────────────────────────────────────────
    private JPanel buildAddPlayerCard() {
        JPanel card = card(" Add Player");

        cmbTeams = styledCombo();
        loadTeamsInto(cmbTeams);
        txtPlayerName = styledField("Player name");

        JButton btnRefresh = styledButton("↻", new Color(100, 116, 139));
        btnRefresh.setMaximumSize(new Dimension(40, 30));
        btnRefresh.setToolTipText("Refresh team list");
        btnRefresh.addActionListener(e -> loadTeamsInto(cmbTeams));

        JPanel teamRow = new JPanel(new BorderLayout(4, 0));
        teamRow.setOpaque(false);
        teamRow.add(cmbTeams, BorderLayout.CENTER);
        teamRow.add(btnRefresh, BorderLayout.EAST);

        JButton btn = styledButton("Add Player", BTN_TEAL);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.addActionListener(e -> addPlayer());

        card.add(label("Select Team:"));
        card.add(Box.createVerticalStrut(4));
        card.add(teamRow);
        card.add(Box.createVerticalStrut(6));
        card.add(label("Player Name:"));
        card.add(Box.createVerticalStrut(4));
        card.add(txtPlayerName);
        card.add(Box.createVerticalStrut(8));
        card.add(btn);
        return card;
    }

    // ── Card: Schedule Match ──────────────────────────────────────────────────
    private JPanel buildScheduleCard() {
        JPanel card = card("Schedule a Match");

        cmbHomeTeam = styledCombo();
        cmbAwayTeam = styledCombo();
        loadTeamsInto(cmbHomeTeam);
        loadTeamsInto(cmbAwayTeam);

        spinnerDate = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor de = new JSpinner.DateEditor(spinnerDate, "dd-MM-yyyy");
        spinnerDate.setEditor(de);
        spinnerDate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        styleSpinner(spinnerDate);

        JButton btnRefresh = styledButton("Refresh", new Color(100, 116, 139));
        btnRefresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btnRefresh.addActionListener(e -> {
            loadTeamsInto(cmbHomeTeam);
            loadTeamsInto(cmbAwayTeam);
        });

        JButton btn = styledButton("Schedule Match", BTN_TEAL);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.addActionListener(e -> scheduleMatch());

        card.add(label("Home Team:"));
        card.add(Box.createVerticalStrut(4));
        card.add(cmbHomeTeam);
        card.add(Box.createVerticalStrut(6));
        card.add(label("Away Team:"));
        card.add(Box.createVerticalStrut(4));
        card.add(cmbAwayTeam);
        card.add(Box.createVerticalStrut(6));
        card.add(label("Match Date:"));
        card.add(Box.createVerticalStrut(4));
        card.add(spinnerDate);
        card.add(Box.createVerticalStrut(6));
        card.add(btnRefresh);
        card.add(Box.createVerticalStrut(4));
        card.add(btn);
        return card;
    }

    // ── RIGHT: Schedule table ─────────────────────────────────────────────────
    private JPanel buildSchedulePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(
                new LineBorder(ACCENT, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel("Match Schedule");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(ACCENT);

        JLabel hint = new JLabel("Click a row to start Live Match tracking");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(SUBTEXT);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.NORTH);
        hdr.add(hint, BorderLayout.SOUTH);

        String[] cols = {"ID", "Home Team", "Away Team", "Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        scheduleTable = new JTable(tableModel);
        scheduleTable.setBackground(new Color(15, 23, 42));
        scheduleTable.setForeground(TEXT);
        scheduleTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        scheduleTable.setRowHeight(30);
        scheduleTable.getTableHeader().setBackground(CARD);
        scheduleTable.getTableHeader().setForeground(ACCENT);
        scheduleTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        scheduleTable.setSelectionBackground(new Color(6, 182, 212, 60));
        scheduleTable.setSelectionForeground(TEXT);
        scheduleTable.setGridColor(new Color(51, 65, 85));
        scheduleTable.getColumnModel().getColumn(0).setMaxWidth(40);

        scheduleTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = scheduleTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        openLiveMatchForRow(row);
                        scheduleTable.clearSelection();
                    }
                }
            }
        });

        JButton btnRefresh = styledButton("Refresh Schedule", BTN_VIOLET);
        btnRefresh.addActionListener(e -> loadSchedule());

        JScrollPane scroll = new JScrollPane(scheduleTable);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);

        p.add(hdr, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        p.add(btnRefresh, BorderLayout.SOUTH);

        loadSchedule();
        return p;
    }

    private void openLiveMatchForRow(int row) {
        int matchId = (int) tableModel.getValueAt(row, 0);
        String home = (String) tableModel.getValueAt(row, 1);
        String away = (String) tableModel.getValueAt(row, 2);
        String status = (String) tableModel.getValueAt(row, 4);
        if ("completed".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "This match is already completed.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Start live scoring for:\n" + home + " vs " + away + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new Livematchpage(matchId, home, away);
            dispose();
        }
    }

    // ── DB ACTIONS ────────────────────────────────────────────────────────────
    private void addTeam() {
        String name = txtTeamName.getText().trim();
        if (name.isEmpty()) {
            toast("Enter a team name.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Insert team
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO teams (team_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.executeUpdate();

            // Get the generated team_id
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int teamId = rs.getInt(1);

                // Initialize points table entry
                PreparedStatement psPoints = con.prepareStatement(
                        "INSERT INTO points_table (team_id, played, won, lost, points) VALUES (?, 0, 0, 0, 0)");
                psPoints.setInt(1, teamId);
                psPoints.executeUpdate();
            }

            txtTeamName.setText("");
            loadTeamsInto(cmbTeams);
            loadTeamsInto(cmbHomeTeam);
            loadTeamsInto(cmbAwayTeam);
            toast("Team '" + name + "' added!");
        } catch (SQLIntegrityConstraintViolationException ex) {
            toast("Team already exists.");
        } catch (Exception ex) {
            toast("Error: " + ex.getMessage());
        }
    }

    private void addPlayer() {
        String team = (String) cmbTeams.getSelectedItem();
        String player = txtPlayerName.getText().trim();
        if (team == null || player.isEmpty()) {
            toast("Select a team and enter player name.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            int teamId = getTeamId(con, team);
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO players (player_name, team_id) VALUES (?,?)");
            ps.setString(1, player);
            ps.setInt(2, teamId);
            ps.executeUpdate();
            txtPlayerName.setText("");
            toast("Player '" + player + "' added to " + team + "!");
        } catch (Exception ex) {
            toast("Error: " + ex.getMessage());
        }
    }

    private void scheduleMatch() {
        String home = (String) cmbHomeTeam.getSelectedItem();
        String away = (String) cmbAwayTeam.getSelectedItem();
        if (home == null || away == null) {
            toast("Add at least 2 teams first.");
            return;
        }
        if (home.equals(away)) {
            toast("Home and Away team must be different.");
            return;
        }

        Date d = (Date) spinnerDate.getValue();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(d);

        try (Connection con = DBConnection.getConnection()) {
            int homeId = getTeamId(con, home);
            int awayId = getTeamId(con, away);
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO ipl_schedule (home_team_id, away_team_id, match_date) VALUES (?,?,?)");
            ps.setInt(1, homeId);
            ps.setInt(2, awayId);
            ps.setString(3, dateStr);
            ps.executeUpdate();
            loadSchedule();
            toast("Match scheduled: " + home + " vs " + away + " on " + dateStr);
        } catch (Exception ex) {
            toast("Error: " + ex.getMessage());
        }
    }

    private void loadSchedule() {
        tableModel.setRowCount(0);
        String sql = "SELECT s.match_id, t1.team_name, t2.team_name, s.match_date, s.status "
                + "FROM ipl_schedule s "
                + "JOIN teams t1 ON s.home_team_id = t1.team_id "
                + "JOIN teams t2 ON s.away_team_id = t2.team_id "
                + "ORDER BY s.match_date DESC";
        try (Connection con = DBConnection.getConnection(); ResultSet rs = con.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDate(4),
                    rs.getString(5)
                });
            }
        } catch (Exception ex) {
            toast("DB Error: " + ex.getMessage());
        }
    }

    private void loadTeamsInto(JComboBox<String> cmb) {
        cmb.removeAllItems();
        try (Connection con = DBConnection.getConnection(); ResultSet rs = con.createStatement().executeQuery("SELECT team_name FROM teams ORDER BY team_name")) {
            while (rs.next()) {
                cmb.addItem(rs.getString(1));
            }
        } catch (Exception ex) {
            /* silent */ }
    }

    private int getTeamId(Connection con, String name) throws Exception {
        PreparedStatement ps = con.prepareStatement("SELECT team_id FROM teams WHERE team_name=?");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        throw new Exception("Team not found: " + name);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private JPanel card(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 14, 14, 14)
        ));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(ACCENT);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(10));
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(SUBTEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField styledField(String hint) {
        JTextField f = new JTextField();
        f.setBackground(new Color(15, 23, 42));
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(71, 85, 105), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        f.setAlignmentX(LEFT_ALIGNMENT);
        // placeholder via focus
        f.setText(hint);
        f.setForeground(SUBTEXT);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(hint)) {
                    f.setText("");
                    f.setForeground(TEXT);
                }
            }

            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(hint);
                    f.setForeground(SUBTEXT);
                }
            }
        });
        return f;
    }

    private JComboBox<String> styledCombo() {
        JComboBox<String> c = new JComboBox<>();
        c.setBackground(new Color(15, 23, 42));
        c.setForeground(TEXT);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    private void styleSpinner(JSpinner s) {
        s.setBackground(new Color(15, 23, 42));
        s.setForeground(TEXT);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setBackground(new Color(15, 23, 42));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setForeground(TEXT);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void toast(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── MAIN ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(Homepage::new);
    }
}
