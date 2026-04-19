
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.*;

/**
 * LIVE MATCH PAGE ─────────────── • Scoreboard: innings, score, wickets, overs,
 * target • Batter / Bowler selectors loaded from DB • Buttons: 0,1,2,3,4,6,
 * Wide, No-Ball, Wicket • Swap Strike, Next Innings • ★ MATCH OVER button —
 * ends match immediately, saves result, shows Points Table • Auto-saves result
 * and updates points table on finish
 */
public class Livematchpage extends JFrame {

    // ── colours ──────────────────────────────────────────────────────────────
    private static final Color BG = new Color(15, 23, 42);
    private static final Color CARD = new Color(30, 41, 59);
    private static final Color ACCENT = new Color(6, 182, 212);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final Color YELLOW = new Color(234, 179, 8);
    private static final Color RED = new Color(239, 68, 68);
    private static final Color ORANGE = new Color(249, 115, 22);
    private static final Color PURPLE = new Color(139, 92, 246);
    private static final Color TEXT = Color.RED;
    private static final Color SUBTEXT = new Color(148, 163, 184);

    // ── match state ──────────────────────────────────────────────────────────
    private final int matchId;
    private int battingTeamId, bowlingTeamId;
    private final int teamAId, teamBId;
    private final String teamAName, teamBName;

    private int score = 0, wickets = 0, innings = 1, target = 0;
    private int balls = 0;           // legal balls delivered this innings
    private final int MAX_OVERS = 20;

    // current batter stats
    private int b1Runs = 0, b1Balls = 0, b1Fours = 0, b1Sixes = 0;
    private int b2Runs = 0, b2Balls = 0, b2Fours = 0, b2Sixes = 0;
    private boolean isBatter1Striker = true;

    // current over bowler stats
    private int bowlRunsGiven = 0, bowlWicketsThisOver = 0;

    // saved at end of 1st innings
    private int inn1Score = 0, inn1Wickets = 0;

    // extras counter
    private int extras = 0;

    // ── UI ───────────────────────────────────────────────────────────────────
    private JLabel lblScoreboard, lblBatters, lblBowler, lblOvers, lblExtras;
    private JComboBox<String> cmbBatter1, cmbBatter2, cmbBowler;

    // ─────────────────────────────────────────────────────────────────────────
    public Livematchpage(int matchId, String teamA, String teamB) {
        this.matchId = matchId;
        this.teamAName = teamA;
        this.teamBName = teamB;
        this.teamAId = fetchTeamId(teamA);
        this.teamBId = fetchTeamId(teamB);
        this.battingTeamId = teamAId;
        this.bowlingTeamId = teamBId;

        setTitle("Live: " + teamA + "  vs  " + teamB);
        setSize(1020, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(BG);

        // Warn before closing via X button
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(Livematchpage.this,
                        "Exit without saving result?\nUse 'MATCH OVER' button to save properly.",
                        "Confirm Exit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r == JOptionPane.YES_OPTION) {
                    new Homepage();
                    dispose();
                }
            }
        });

        setLayout(new BorderLayout(0, 0));
        add(buildScoreboardPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildControlPanel(), BorderLayout.SOUTH);

        refreshUI();
        setVisible(true);
    }

    // =========================================================================
    //  SCOREBOARD PANEL
    // =========================================================================
    private JPanel buildScoreboardPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(8, 12, 28));
        p.setBorder(new EmptyBorder(16, 24, 16, 24));

        lblScoreboard = centreLabel("", new Font("Monospaced", Font.BOLD, 28), ACCENT);
        lblBatters = centreLabel("", new Font("Segoe UI", Font.PLAIN, 15), TEXT);
        lblBowler = centreLabel("", new Font("Segoe UI", Font.PLAIN, 13), YELLOW);
        lblOvers = centreLabel("", new Font("Segoe UI", Font.PLAIN, 12), GREEN);
        lblExtras = centreLabel("", new Font("Segoe UI", Font.PLAIN, 12), SUBTEXT);

        JPanel inner = new JPanel(new GridLayout(5, 1, 0, 3));
        inner.setOpaque(false);
        inner.add(lblScoreboard);
        inner.add(lblBatters);
        inner.add(lblBowler);
        inner.add(lblOvers);
        inner.add(lblExtras);

        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    //  CENTER — Player Selectors
    // =========================================================================
    private JPanel buildCenterPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 18));
        p.setBackground(CARD);

        cmbBatter1 = styledCombo(fetchPlayers(battingTeamId));
        cmbBatter2 = styledCombo(fetchPlayers(battingTeamId));
        if (cmbBatter2.getItemCount() > 1) {
            cmbBatter2.setSelectedIndex(1);
        }
        cmbBowler = styledCombo(fetchPlayers(bowlingTeamId));

        p.add(selectorBlock("Striker  (Bat 1)", cmbBatter1));
        p.add(selectorBlock("Non-Striker  (Bat 2)", cmbBatter2));
        p.add(selectorBlock("Current Bowler", cmbBowler));

        return p;
    }

    private JPanel selectorBlock(String title, JComboBox<String> cmb) {
        JPanel bp = new JPanel();
        bp.setLayout(new BoxLayout(bp, BoxLayout.Y_AXIS));
        bp.setOpaque(false);
        JLabel l = new JLabel(title);
        l.setForeground(SUBTEXT);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setAlignmentX(CENTER_ALIGNMENT);
        cmb.setAlignmentX(CENTER_ALIGNMENT);
        bp.add(l);
        bp.add(Box.createVerticalStrut(5));
        bp.add(cmb);
        return bp;
    }

    // =========================================================================
    //  SOUTH — All Buttons
    // =========================================================================
    private JPanel buildControlPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(10, 14, 14, 14));

        // ── ROW 1: Run buttons + Wicket ──────────────────────────────────────
        JPanel runRow = new JPanel(new GridLayout(1, 7, 8, 0));
        runRow.setOpaque(false);

        String[] runLabels = {"0  Dot", "1  Run", "2  Runs", "3  Runs", "4  ●", "6  ●", "WICKET"};
        String[] runCodes = {"0", "1", "2", "3", "4", "6", "W"};
        Color[] runColors = {SUBTEXT, TEXT, TEXT, TEXT, GREEN, ACCENT, RED};

        for (int i = 0; i < runLabels.length; i++) {
            final String code = runCodes[i];
            JButton b = bigBtn(runLabels[i], runColors[i]);
            b.addActionListener(e -> handleBall(code));
            runRow.add(b);
        }

        // ── ROW 2: Extras + Navigation + MATCH OVER ──────────────────────────
        JPanel navRow = new JPanel(new GridLayout(1, 5, 8, 0));
        navRow.setOpaque(false);

        JButton btnWide = bigBtn("Wide  +1", YELLOW);
        JButton btnNoBall = bigBtn("No Ball  +1", YELLOW);
        JButton btnSwap = bigBtn("Swap Strike", PURPLE);
        JButton btnNextInn = bigBtn("Next Innings", new Color(59, 130, 246));

        // ★ MATCH OVER BUTTON ★
        JButton btnMatchOver = new JButton("MATCH OVER");
        btnMatchOver.setBackground(new Color(60, 28, 8));
        btnMatchOver.setForeground(ORANGE);
        btnMatchOver.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnMatchOver.setFocusPainted(false);
        btnMatchOver.setBorder(new LineBorder(ORANGE, 2, true));
        btnMatchOver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMatchOver.setPreferredSize(new Dimension(0, 52));

        btnWide.addActionListener(e -> handleBall("WIDE"));
        btnNoBall.addActionListener(e -> handleBall("NOBALL"));
        btnSwap.addActionListener(e -> {
            isBatter1Striker = !isBatter1Striker;
            refreshUI();
        });
        btnNextInn.addActionListener(e -> switchInnings());
        btnMatchOver.addActionListener(e -> confirmMatchOver());   // ← wired here

        navRow.add(btnWide);
        navRow.add(btnNoBall);
        navRow.add(btnSwap);
        navRow.add(btnNextInn);
        navRow.add(btnMatchOver);

        outer.add(runRow, BorderLayout.NORTH);
        outer.add(navRow, BorderLayout.SOUTH);
        return outer;
    }

    // =========================================================================
    //  MATCH OVER — confirm dialog with current summary, then finish
    // =========================================================================
    private void confirmMatchOver() {
        int completedOvers = balls / 6;
        int currentBall = balls % 6;

        StringBuilder sb = new StringBuilder();
        sb.append("Current Match Status\n");
        sb.append("─────────────────────────────────\n");

        if (innings == 1) {
            sb.append("Innings 1  |  ").append(teamName(battingTeamId)).append(" batting\n");
            sb.append("Score : ").append(score).append("/").append(wickets)
                    .append("  (").append(completedOvers).append(".").append(currentBall).append(" ov)\n\n");
            sb.append("⚠  Ending now gives WIN to ").append(teamName(bowlingTeamId)).append(".\n");
            sb.append("   Use 'Next Innings' first if you want a 2nd innings.");
        } else {
            sb.append("Innings 2  |  ").append(teamName(battingTeamId)).append(" chasing ").append(target).append("\n");
            sb.append("Score : ").append(score).append("/").append(wickets)
                    .append("  (").append(completedOvers).append(".").append(currentBall).append(" ov)\n");
            int runsLeft = target - score;
            if (runsLeft <= 0) {
                sb.append("\n ").append(teamName(battingTeamId)).append(" has already won!");
            } else {
                sb.append("Need  : ").append(runsLeft).append(" more runs\n\n");
                sb.append("Ending now → ").append(teamName(bowlingTeamId))
                        .append(" wins by ").append(runsLeft).append(" runs.");
            }
        }

        sb.append("\n─────────────────────────────────\n");
        sb.append("Confirm? This will save the result\nand open the Points Table.");

        int choice = JOptionPane.showConfirmDialog(
                this, sb.toString(),
                "End Match",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            finishMatch(innings == 2 && score >= target);
        }
    }

    // =========================================================================
    //  BALL HANDLER
    // =========================================================================
    private void handleBall(String action) {
        if (cmbBatter1.getSelectedItem() == null || cmbBatter2.getSelectedItem() == null) {
            toast("Please select both batters.");
            return;
        }
        if (cmbBatter1.getSelectedItem().equals(cmbBatter2.getSelectedItem())) {
            toast("Batter 1 and Batter 2 must be different players!");
            return;
        }

        boolean legalDelivery = true;

        switch (action) {
            case "0":
                addRuns(0);
                break;
            case "1":
                addRuns(1);
                isBatter1Striker = !isBatter1Striker;
                break;
            case "2":
                addRuns(2);
                break;
            case "3":
                addRuns(3);
                isBatter1Striker = !isBatter1Striker;
                break;
            case "4":
                addRuns(4);
                if (isBatter1Striker) {
                    b1Fours++;
                } else {
                    b2Fours++;
                }
                break;
            case "6":
                addRuns(6);
                if (isBatter1Striker) {
                    b1Sixes++;
                } else {
                    b2Sixes++;
                }
                break;
            case "W":
                wickets++;
                bowlWicketsThisOver++;
                addRuns(0);
                handleWicket();
                break;
            case "WIDE":
                score++;
                extras++;
                bowlRunsGiven++;
                legalDelivery = false;
                break;
            case "NOBALL":
                score++;
                extras++;
                bowlRunsGiven++;
                legalDelivery = false;
                break;
        }

        if (legalDelivery) {
            balls++;
            checkOverComplete();
        }

        // Chase-win auto-detect
        if (innings == 2 && score >= target) {
            JOptionPane.showMessageDialog(this,
                    "  " + teamName(battingTeamId) + " wins by " + (10 - wickets) + " wickets!");
            finishMatch(true);
            return;
        }

        refreshUI();
    }

    private void addRuns(int runs) {
        score += runs;
        bowlRunsGiven += runs;
        if (isBatter1Striker) {
            b1Runs += runs;
            b1Balls++;
        } else {
            b2Runs += runs;
            b2Balls++;
        }
    }

    // ── Over-complete check ───────────────────────────────────────────────────
    private void checkOverComplete() {
        if (balls > 0 && balls % 6 == 0) {
            int ov = balls / 6;
            JOptionPane.showMessageDialog(this,
                    "Over " + ov + " complete!\n"
                    + "Bowler: " + bowlWicketsThisOver + "W  /  " + bowlRunsGiven + "R\n"
                    + "Change the bowler and swap strike.");
            isBatter1Striker = !isBatter1Striker;
            bowlWicketsThisOver = 0;
            bowlRunsGiven = 0;

            if (ov >= MAX_OVERS) {
                JOptionPane.showMessageDialog(this, +MAX_OVERS + " overs completed!");
                if (innings == 1) {
                    switchInnings();
                } else {
                    finishMatch(false);
                }
            }
        }
    }

    // ── Wicket handling ───────────────────────────────────────────────────────
    private void handleWicket() {
        if (isBatter1Striker) {
            b1Runs = 0;
            b1Balls = 0;
            b1Fours = 0;
            b1Sixes = 0;
        } else {
            b2Runs = 0;
            b2Balls = 0;
            b2Fours = 0;
            b2Sixes = 0;
        }

        if (wickets >= 10) {
            JOptionPane.showMessageDialog(this, "All out! Innings over.");
            if (innings == 1) {
                switchInnings();
            } else {
                finishMatch(false);
            }
        } else {
            JOptionPane.showMessageDialog(this, " Wicket! Select the new batter from the dropdown.");
        }
    }

    // ── Switch Innings ────────────────────────────────────────────────────────
    private void switchInnings() {
        if (innings == 2) {
            toast("Already in 2nd innings.");
            return;
        }

        inn1Score = score;
        inn1Wickets = wickets;
        target = score + 1;
        innings = 2;

        int tmp = battingTeamId;
        battingTeamId = bowlingTeamId;
        bowlingTeamId = tmp;

        score = 0;
        wickets = 0;
        balls = 0;
        extras = 0;
        b1Runs = 0;
        b1Balls = 0;
        b1Fours = 0;
        b1Sixes = 0;
        b2Runs = 0;
        b2Balls = 0;
        b2Fours = 0;
        b2Sixes = 0;
        bowlRunsGiven = 0;
        bowlWicketsThisOver = 0;
        isBatter1Striker = true;

        cmbBatter1.setModel(new DefaultComboBoxModel<>(fetchPlayers(battingTeamId)));
        cmbBatter2.setModel(new DefaultComboBoxModel<>(fetchPlayers(battingTeamId)));
        if (cmbBatter2.getItemCount() > 1) {
            cmbBatter2.setSelectedIndex(1);
        }
        cmbBowler.setModel(new DefaultComboBoxModel<>(fetchPlayers(bowlingTeamId)));

        refreshUI();
        JOptionPane.showMessageDialog(this,
                "2nd Innings started!\n"
                + teamName(battingTeamId) + " needs " + target + " runs to win.");
    }

    // =========================================================================
    //  FINISH MATCH — saves result to DB, then opens StatsPage
    // =========================================================================
    private void finishMatch(boolean chasingWon) {
        int winnerId, loserId;
        String winMargin;

        if (innings == 1) {
            winnerId = bowlingTeamId;
            loserId = battingTeamId;
            winMargin = "innings declared";
            inn1Score = score;
            inn1Wickets = wickets;
        } else if (chasingWon) {
            winnerId = battingTeamId;
            loserId = bowlingTeamId;
            winMargin = (10 - wickets) + " wickets";
        } else {
            if (score >= target) {
                winnerId = battingTeamId;
                loserId = bowlingTeamId;
                winMargin = (10 - wickets) + " wickets";
            } else {
                winnerId = bowlingTeamId;
                loserId = battingTeamId;
                winMargin = (target - 1 - score) + " runs";
            }
        }

        String t1Str = inn1Score + "/" + inn1Wickets + " (" + teamAName + ")";
        String t2Str = score + "/" + wickets + " (" + teamBName + ")";

        try (Connection con = DBConnection.getConnection()) {

            // 1. Mark match completed
            PreparedStatement psStatus = con.prepareStatement(
                    "UPDATE ipl_schedule SET status='completed' WHERE match_id=?");
            psStatus.setInt(1, matchId);
            psStatus.executeUpdate();

            // 2. Insert result row
            PreparedStatement psResult = con.prepareStatement(
                    "INSERT INTO match_results "
                    + "(match_id, winner_team_id, loser_team_id, win_margin, team1_score, team2_score) "
                    + "VALUES (?,?,?,?,?,?)");
            psResult.setInt(1, matchId);
            psResult.setInt(2, winnerId);
            psResult.setInt(3, loserId);
            psResult.setString(4, winMargin);
            psResult.setString(5, t1Str);
            psResult.setString(6, t2Str);
            psResult.executeUpdate();

            // 3. Winner: +2 points
            PreparedStatement psW = con.prepareStatement(
                    "UPDATE points_table SET played=played+1, won=won+1, points=points+2 WHERE team_id=?");
            psW.setInt(1, winnerId);
            psW.executeUpdate();

            // 4. Loser: +1 played, +1 lost
            PreparedStatement psL = con.prepareStatement(
                    "UPDATE points_table SET played=played+1, lost=lost+1 WHERE team_id=?");
            psL.setInt(1, loserId);
            psL.executeUpdate();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "⚠ DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Final result popup
        JOptionPane.showMessageDialog(this,
                "MATCH RESULT\n"
                + "════════════════════════════\n"
                + "Winner  :  " + teamName(winnerId) + "\n"
                + "Margin  :  " + winMargin + "\n"
                + "────────────────────────────\n"
                + t1Str + "\n"
                + t2Str + "\n"
                + "════════════════════════════\n"
                + "Opening Points Table...",
                "Match Over", JOptionPane.INFORMATION_MESSAGE);

        // Navigate to Points Table / Stats
        new Statspage().setVisible(true);
        dispose();
    }

    // =========================================================================
    //  UI REFRESH
    // =========================================================================
    private void refreshUI() {
        int completedOvers = balls / 6;
        int currentBall = balls % 6;

        // Score header
        String header = "INN " + innings + "   |   "
                + score + " / " + wickets + "   (" + completedOvers + "." + currentBall + " ov)";
        if (innings == 2) {
            int need = Math.max(0, target - score);
            header += "   |   TARGET: " + target + "   |   NEED: " + need;
        }
        lblScoreboard.setText(header);

        // Batter line
        String s1 = isBatter1Striker ? " ⚡" : "  ";
        String s2 = !isBatter1Striker ? " ⚡" : "  ";
        String b1 = cmbBatter1.getSelectedItem() == null ? "-" : (String) cmbBatter1.getSelectedItem();
        String b2 = cmbBatter2.getSelectedItem() == null ? "-" : (String) cmbBatter2.getSelectedItem();
        lblBatters.setText(String.format(
                "<html><center>"
                + "%s%s &nbsp; <b>%d(%d)</b> [4s:%d &nbsp; 6s:%d]"
                + " &nbsp;&nbsp;|&nbsp;&nbsp; "
                + "%s%s &nbsp; <b>%d(%d)</b> [4s:%d &nbsp; 6s:%d]"
                + "</center></html>",
                b1, s1, b1Runs, b1Balls, b1Fours, b1Sixes,
                b2, s2, b2Runs, b2Balls, b2Fours, b2Sixes));

        // Bowler line
        String bwl = cmbBowler.getSelectedItem() == null ? "-" : (String) cmbBowler.getSelectedItem();
        lblBowler.setText("Bowler: " + bwl
                + "   |   This Over: " + bowlWicketsThisOver + "W / " + bowlRunsGiven + "R");

        // Overs line
        lblOvers.setText("Overs: " + completedOvers + " / " + MAX_OVERS
                + "   (" + (MAX_OVERS - completedOvers) + " remaining)");

        // Extras
        lblExtras.setText("Extras: " + extras);
    }

    // =========================================================================
    //  DB HELPERS
    // =========================================================================
    private String[] fetchPlayers(int teamId) {
        ArrayList<String> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT player_name FROM players WHERE team_id=? ORDER BY player_name");
            ps.setInt(1, teamId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (list.isEmpty()) {
            list.add("(no players — add on Home Page)");
        }
        return list.toArray(new String[0]);
    }

    private int fetchTeamId(String name) {
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT team_id FROM teams WHERE team_name=?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private String teamName(int id) {
        return id == teamAId ? teamAName : teamBName;
    }

    // =========================================================================
    //  SWING HELPERS
    // =========================================================================
    private JButton bigBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(new Color(30, 41, 59));
        b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(fg.darker(), 1, true));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(0, 52));
        return b;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setBackground(new Color(15, 23, 42));
        c.setForeground(TEXT);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setPreferredSize(new Dimension(210, 32));
        return c;
    }

    private JLabel centreLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private void toast(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
