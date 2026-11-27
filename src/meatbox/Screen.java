package meatbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static meatbox.Config.*;
import static meatbox.Body.*;
import static meatbox.World.*;

/**
 * =============================================================================
 *  SCREEN  -  the Swing window. Same layout as the assembly console version.
 * =============================================================================
 *  KEY IDEA: A FAKE TERMINAL
 *  -------------------------
 *  The asm wrote ANSI escape codes to a console. Here we keep a grid of
 *  120 x 30 character CELLS (a char + a colour each), fill it every frame
 *  with the same rows and columns as the asm, then paint the grid in one go.
 *
 *     rows 1      header           rows 2-25 / col 1    map (64 x 24)
 *     rows 2-25 / col 67  person panel      rows 26-29   event log
 *     row 30      key help
 *
 *  KEY IDEA: THE SWING THREAD
 *  --------------------------
 *  Swing is single-threaded: all drawing and key handling happen on the
 *  "Event Dispatch Thread" (EDT). A javax.swing.Timer fires on that same
 *  thread every 50 ms, so the sim, the keys and the painting never run at
 *  the same time - no locks needed.
 *
 *  Keys:  Space pause   + / - speed   N / P / arrows / Tab  next / prev person
 *         Q / Esc quit
 */
final class Screen extends JPanel {
    static final int COLS = 120, ROWS = 30, PANEL_COL = 67;

    // ---- colours (the asm's ANSI codes, as RGB) -------------------------------------
    static final Color BG      = new Color(12, 12, 12);
    static final Color FG      = new Color(204, 204, 204);   // default text
    static final Color GRAY    = new Color(118, 118, 118);   // 90  grass, faint text
    static final Color GREEN   = new Color(19, 161, 14);     // 32  growing / good
    static final Color BGREEN  = new Color(22, 198, 12);     // 92  mature tree
    static final Color YELLOW  = new Color(193, 156, 0);     // 33  stump / warning
    static final Color BYELLOW = new Color(249, 241, 165);   // 93  PAUSED
    static final Color BCYAN   = new Color(97, 214, 214);    // 96  man
    static final Color BMAG    = new Color(180, 0, 158);     // 95  woman
    static final Color RED     = new Color(197, 15, 31);     // 31  bad
    static final Color BRED    = new Color(231, 72, 86);     // 91  very bad
    static final Color WHITE   = new Color(242, 242, 242);   // 97  titles
    static final Color SEL_BG  = WHITE;                      // 7;97 selected = inverted

    static final char[]  TILE_CH  = {'.', ',', '"', 't', 'T', '_'};
    static final Color[] TILE_COL = {GRAY, GREEN, GREEN, GREEN, BGREEN, YELLOW};

    // ---- the cell grid ------------------------------------------------------------------
    final char[][]  ch  = new char[ROWS][COLS];
    final Color[][] col = new Color[ROWS][COLS];
    final boolean[][] inv = new boolean[ROWS][COLS];   // inverted (selected person)

    final World w;
    final Sim sim;
    int speedIdx = 1, sel = 0;
    boolean paused;

    final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 15);
    int cw, chh, asc;                                   // cell width, height, baseline

    Screen(World w, Sim sim) {
        this.w = w;
        this.sim = sim;
        setBackground(BG);
        FontMetrics fm = getFontMetrics(font);
        cw = fm.charWidth('M');
        chh = fm.getHeight();
        asc = fm.getAscent();
        setPreferredSize(new Dimension(COLS * cw + 8, ROWS * chh + 8));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { key(e); }
        });
    }

    /** Builds the window and starts the 50 ms heartbeat. */
    static void open(World w, Sim sim) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Meatbox");
            Screen s = new Screen(w, sim);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(s);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            s.requestFocusInWindow();
            new Timer(FRAME_MS, e -> s.frame()).start();
        });
    }

    /** One heartbeat: run some days (unless paused), rebuild the grid, repaint. */
    void frame() {
        if (!paused) for (int d = 0; d < SPEEDS[speedIdx]; d++) sim.day();
        build();
        repaint();
    }

    // =====================================================================================
    //  INPUT
    // =====================================================================================
    void key(KeyEvent e) {
        char c = Character.toLowerCase(e.getKeyChar());
        int k = e.getKeyCode();
        if (c == ' ') paused = !paused;
        else if (c == '+' || c == '=') { if (speedIdx < SPEEDS.length - 1) speedIdx++; }
        else if (c == '-' || c == '_') { if (speedIdx > 0) speedIdx--; }
        else if (c == 'n' || k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_TAB) selectStep(1);
        else if (c == 'p' || k == KeyEvent.VK_LEFT) selectStep(-1);
        else if (c == 'q' || k == KeyEvent.VK_ESCAPE) System.exit(0);
        else return;
        build();
        repaint();
    }

    /** Move the selection to the next/previous LIVING person (wraps around). */
    void selectStep(int dir) {
        int hi = w.popHi;
        if (hi == 0) return;
        int i = sel;
        for (int tries = hi; tries > 0; tries--) {
            i += dir;
            if (i < 0) i = hi - 1;
            if (i >= hi) i = 0;
            if (w.alive[i]) { sel = i; return; }
        }
    }

    // =====================================================================================
    //  GRID WRITING HELPERS  (row and col are 1-based, like ANSI "ESC[row;colH")
    // =====================================================================================
    int cr, cc;                                  // cursor
    Color pen = FG;

    void at(int row, int c) { cr = row - 1; cc = c - 1; pen = FG; }

    void put(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (cr >= 0 && cr < ROWS && cc >= 0 && cc < COLS) {
                ch[cr][cc] = s.charAt(i);
                col[cr][cc] = pen;
                inv[cr][cc] = false;
            }
            cc++;
        }
    }
    void put(String s, Color c) { Color old = pen; pen = c; put(s); pen = old; }
    void pad(String s, int width) { put(s); for (int i = s.length(); i < width; i++) put(" "); }
    void num(int v, int width) { put(String.format("%" + (width > 0 ? width : "") + "d", v)); }
    /** Clear to end of line (ANSI "ESC[K"). */
    void eol() { while (cc < COLS) { put(" "); } }

    // =====================================================================================
    //  BUILD ONE FRAME
    // =====================================================================================
    void build() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) { ch[r][c] = ' '; col[r][c] = FG; inv[r][c] = false; }

        if (!w.alive[sel]) selectStep(1);

        // ---- header ----
        at(1, 1);
        put(" MEATBOX ", WHITE);
        put(" Year " + (w.day / 365) + " Day ");
        num(w.day % 365, 3);
        put("  Pop " + w.popCount + "  Born " + w.births + "  Died " + w.deaths
            + "  Disabled " + w.disabledN + "  Wood " + w.wood);
        int trees = 0;
        for (byte t : w.map) if (t == T_TREE) trees++;
        put("  Trees " + trees + "  x" + SPEEDS[speedIdx] + "d");
        if (paused) put(" PAUSED", BYELLOW);

        // ---- map, then people on top ----
        for (int i = 0; i < MAP_SIZE; i++) {
            int r = 1 + i / MAP_W, c = i % MAP_W;
            ch[r][c] = TILE_CH[w.map[i]];
            col[r][c] = TILE_COL[w.map[i]];
        }
        for (int p = 0; p < w.popHi; p++) if (w.alive[p]) glyph(p, false);
        if (w.alive[sel]) glyph(sel, true);

        panel();

        // ---- event log ----
        for (int i = 0; i < LOG_N; i++) { at(26 + i, 1); put(w.logLines[i]); }
        at(30, 1);
        put("[Space] pause  [+/-] speed  [N/P or arrows] next/prev person  [Q] quit", GRAY);
    }

    /** f / m = child, F / M = adult. */
    void glyph(int p, boolean selected) {
        int r = 1 + (w.y[p] & 0xFF), c = w.x[p] & 0xFF;
        boolean male = w.sex[p] == SEX_M;
        char g = male ? 'M' : 'F';
        if (Config.years(w.age[p]) < 16) g = Character.toLowerCase(g);
        ch[r][c] = g;
        col[r][c] = male ? BCYAN : BMAG;
        inv[r][c] = selected;
    }

    // ---- the selected person: rows 2-25, from column 67 ----
    void panel() {
        int p = sel, bb = p * 8;
        char[] b = w.blood;
        at(2, PANEL_COL);
        if (w.popCount == 0) {
            put("Everyone has died.", BRED);
            causes();
            return;
        }
        put(w.personName(p), WHITE);
        put(" #" + p + (w.sex[p] == SEX_M ? "  male  " : "  female") + "  age " + Config.years(w.age[p]));

        at(3, PANEL_COL);
        if (w.bed[p]) put("BEDRIDDEN ", BRED);
        int cap = w.cap[p];
        if ((cap & Sim.LEGS) == 0) put("can't walk ", BRED);
        if ((cap & Sim.ARMS) == 0) put("no usable arm ", BRED);
        if (w.preg[p] > 0) put("pregnant (" + w.preg[p] + "d) ");

        at(4, PANEL_COL);
        put("Blood ");          pctGood(b[bb + B_VOL]);
        put("  SpO2 ");         pctGood(b[bb + B_O2]);
        put("  O2 to tissue "); pctGood(b[bb + B_PERF]);
        put("  HR " + (int) b[bb + B_HR]);

        at(5, PANEL_COL);
        int t = b[bb + B_TEMP];
        put("Temp " + t / 100 + "." + (t % 100) / 10 + "C  Infection ");
        pctBad(b[bb + B_PATH]);
        put("  Toxins ");  pctBad(b[bb + B_WASTE]);
        put("  Pain ");    pctBad(b[bb + B_PAIN]);

        at(6, PANEL_COL);
        put("Body", WHITE);
        put("  (red name = can't be used)");
        for (int r = 0; r < 8; r++) {
            at(7 + r, PANEL_COL);       partLine(p, r);
            at(7 + r, PANEL_COL + 27);  partLine(p, r + 8);
        }

        at(15, PANEL_COL);
        put("Afflictions", WHITE);
        int lines = 0;
        for (int s = 0; s < NSLOTS && lines < 4; s++) {
            int v = w.aff[p * NSLOTS + s];
            if ((v & 0xFF) == 0) continue;
            Afflictions.Aff a = Afflictions.TABLE[v & 0xFF];
            at(16 + lines, PANEL_COL);
            pad(a.name, 17);
            put("on ");
            pad(Body.name((v >>> 8) & 0xFF), 10);
            num(v >>> 16, 5);
            put(a.has(Afflictions.CHRONIC) ? "d so far (chronic)" : "d left");
            lines++;
        }
        if (lines == 0) { at(16, PANEL_COL); put("none", GREEN); }
        causes();
    }

    /** "Lungs     [######--]  75%"  - name in red if the part can't be used today. */
    void partLine(int p, int part) {
        boolean usable = ((w.cap[p] >>> part) & 1) != 0;
        pad(Body.name(part), 10);
        if (!usable) for (int i = cc - 10; i < cc; i++) col[cr][i] = RED;
        if (((w.lame[p] >>> part) & 1) != 0) { put("[DISABLED]    ", RED); return; }
        int h = w.hp[p * NPARTS + part];
        if (h == 0) { put("[  LOST  ]    ", GRAY); return; }
        Color c = h >= 6667 ? GREEN : h >= 3334 ? YELLOW : RED;
        int filled = (h * 8 + HP_MAX - 1) / HP_MAX;             // round up
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 8; i++) bar.append(i < filled ? '#' : '-');
        put(bar.append(']').toString(), c);
        num(h / 100, 4);
        put("%");
    }

    /** Top 5 causes of death so far. */
    void causes() {
        at(20, PANEL_COL);
        put("Causes of death", WHITE);
        long used = 0;
        for (int row = 0; row < 5; row++) {
            int best = -1, bestN = 0;
            for (int id = 0; id < Afflictions.COUNT; id++)
                if (((used >>> id) & 1) == 0 && w.causeCount[id] > bestN) { bestN = w.causeCount[id]; best = id; }
            if (best == -1) break;
            used |= 1L << best;
            at(21 + row, PANEL_COL);
            pad(Afflictions.TABLE[best].name, 18);
            num(bestN, 5);
        }
    }

    /** High is good: green >= 90 %, yellow >= 70 %, else red. */
    void pctGood(int v) { put(v / 100 + "%", v >= 9000 ? GREEN : v >= 7000 ? YELLOW : RED); }
    /** High is bad: green < 10 %, yellow < 40 %, else red. */
    void pctBad(int v)  { put(v / 100 + "%", v < 1000 ? GREEN : v < 4000 ? YELLOW : RED); }

    // =====================================================================================
    //  PAINT: draw every cell. Runs of one colour are drawn as one string (faster).
    // =====================================================================================
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        for (int r = 0; r < ROWS; r++) {
            int y = 4 + r * chh;
            int c = 0;
            while (c < COLS) {
                Color fg = col[r][c];
                boolean iv = inv[r][c];
                int start = c;
                while (c < COLS && col[r][c] == fg && inv[r][c] == iv) c++;
                if (iv) {
                    g.setColor(SEL_BG);
                    g.fillRect(4 + start * cw, y, (c - start) * cw, chh);
                    g.setColor(BG);
                } else g.setColor(fg);
                g.drawString(new String(ch[r], start, c - start), 4 + start * cw, y + asc);
            }
        }
    }

    /** Test helper: render one frame to a PNG without opening a window. */
    static void snapshot(World w, Sim sim, int days, String file) throws Exception {
        for (int d = 0; d < days; d++) sim.day();
        Screen s = new Screen(w, sim);
        // pick someone with afflictions so the panel shows something
        for (int p = 0; p < w.popHi; p++)
            if (w.alive[p] && w.aff[p * NSLOTS] != 0) { s.sel = p; break; }
        s.build();
        Dimension d = s.getPreferredSize();
        s.setSize(d);
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(d.width, d.height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        s.paint(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", new java.io.File(file));
    }
}
