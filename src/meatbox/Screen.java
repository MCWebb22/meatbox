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
        put("  " + Climate.SEASON[Climate.season(w.day % 365)] + " ");
        put(String.format("%.0fC", w.outT / 100.0), w.outT < 0 ? BCYAN : FG);
        put("  Pop " + w.popCount + "  Born " + w.births + "  Died " + w.deaths);
        int pop = Math.max(1, w.popCount);
        long foodDays = w.food / RATION / pop;
        put("  Food ");
        put(foodDays + "d", w.foodQ < 65536 ? BRED : foodDays < 30 ? YELLOW : GREEN);
        put("  Wood " + w.wood);
        if (w.fireQ < 65536) put(" (fire " + (w.fireQ * 100 >> 16) + "%)", BRED);
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
        int f = w.fat[p];
        if (f < FAT_IMMUNE) put(f < FAT_WASTING ? "STARVING " : "hungry ", f < FAT_WASTING ? BRED : YELLOW);
        if (b(p, B_TEMP) < HYPO_T) put("hypothermic ", BCYAN);

        at(4, PANEL_COL);
        put("Blood ");          pctGood(b[bb + B_VOL]);
        put(" SpO2 ");          pctGood(b[bb + B_O2]);
        put(" O2 to tissue ");  pctGood(b[bb + B_PERF]);
        put(" HR " + (int) b[bb + B_HR]);

        at(5, PANEL_COL);
        int t = b[bb + B_TEMP];
        put("Temp " + t / 100 + "." + (t % 100) / 10 + "C Infect ");
        pctBad(b[bb + B_PATH]);
        put(" Toxins ");  pctBad(b[bb + B_WASTE]);
        put(" Pain ");    pctBad(b[bb + B_PAIN]);

        at(6, PANEL_COL);
        int b2 = p * 4;
        put("Hb ");  pctGood(w.blood2[b2 + B2_HB]);
        int g = w.blood2[b2 + B2_GLU];
        put(" Glu " + g / 100 + "." + (g % 100) / 10, g > 1100 || g < 350 ? RED : g > 750 ? YELLOW : GREEN);
        int bp = w.blood2[b2 + B2_BP];
        put(" BP " + bp, bp >= 160 || bp < 80 ? RED : bp >= 140 ? YELLOW : GREEN);
        put(" Food ");   pctGood(w.fat[p]);
        put(" Warm ");   pctGood(w.warmth[p]);

        at(7, PANEL_COL);
        int fb = p * NFN;
        String[] fl = {"Walk", "LH", "RH", "Eyes", "Ears", "Stam", "Work"};
        int[] fi = {FN_MOBILITY, FN_MANIP_L, FN_MANIP_R, FN_VISION, FN_HEARING, FN_STAMINA, FN_WORK};
        for (int i = 0; i < fl.length; i++) { put(fl[i] + " "); pctShort(w.func[fb + fi[i]]); put(" "); }

        at(8, PANEL_COL);
        int pb = p * NPSY;
        int mood = w.psy[pb + PSY_MOOD];
        put("Mood " + mood / 100, mood < -4000 ? BRED : mood < 0 ? YELLOW : GREEN);
        put(" Anx " + w.psy[pb + PSY_ANXIETY] / 100);
        put(" Energy " + w.psy[pb + PSY_ENERGY] / 100);
        put(" Aggr " + w.psy[pb + PSY_AGGRESSION] / 100);
        int re = w.psy[pb + PSY_REALITY];
        if (re > 3000) put(" PSYCHOSIS", BRED);
        put(" Stress " + w.stress[p] / 100, w.stress[p] > 5000 ? RED : FG);

        at(9, PANEL_COL);
        put("Body %", WHITE);
        put("  (red = can't be used today)", GRAY);
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 6; c++) {
                int part = c * 8 + r;
                if (part >= NPARTS) continue;
                at(10 + r, PANEL_COL + c * 9);
                partCell(p, part);
            }

        at(18, PANEL_COL);
        int dens = w.bmd[p];
        put("Bones: density "); put(dens / 100 + "%", dens < Config.OSTEO_BMD ? RED : dens < 8500 ? YELLOW : GREEN);
        int shown = 0;
        int base = p * Bones.NBONES;
        for (int bn = 0; bn < Bones.NBONES && shown < 2; bn++) {
            int v = w.bone[base + bn];
            if (v >= HP_MAX) continue;
            put(shown == 0 ? "  broken: " : ", ");
            put(Bones.name(bn) + " " + v / 100 + "%", v < 5000 ? RED : YELLOW);
            shown++;
        }
        if (w.boneHurt[p] > 2) put(" +" + (w.boneHurt[p] - 2));

        at(19, PANEL_COL);
        put("Conditions", WHITE);
        put(" (" + w.affN[p] + ")");
        int lines = 0;
        for (int s = 0; s < NSLOTS && lines < 5; s++) {
            int id = w.affId[p * NSLOTS + s];
            if (id == 0) continue;
            Afflictions.Aff a = Afflictions.TABLE[id];
            int part = w.affPart[p * NSLOTS + s];
            int sv = w.affSev[p * NSLOTS + s];
            at(20 + lines, PANEL_COL);
            String nm = a.name.length() > 26 ? a.name.substring(0, 26) : a.name;
            pad(nm, 27, a.cat == Afflictions.C_MENTAL ? BMAG : a.cat == Afflictions.C_CANCER ? RED : FG);
            String site = a.has(Afflictions.ON_BONE) ? Bones.name(part) : Body.PARTS[part].abbr();
            pad(site.length() > 12 ? site.substring(0, 12) : site, 13);
            if ((sv & 0x8000) != 0) put("dormant", GRAY);
            else if (a.has(Afflictions.CHRONIC) || a.episodic()) put("sev " + (sv & 0x7FFF) / 100 + "%");
            else put((int) w.affDays[p * NSLOTS + s] + "d left");
            lines++;
        }
        if (lines == 0) { at(20, PANEL_COL); put("none", GREEN); }
        causes();
    }

    /** "Brai 99" - red name if the part can't be used today, grey if lost/absent. */
    void partCell(int p, int part) {
        int h = w.hp[p * NPARTS + part];
        boolean lame = ((w.lame[p] >>> part) & 1) != 0;
        String ab = Body.PARTS[part].abbr();
        if (h == 0) { put(ab + "  --", GRAY); return; }
        put(ab, lame ? RED : FG);
        Color c = h >= 6667 ? GREEN : h >= 3334 ? YELLOW : RED;
        String n = String.valueOf(h / 100);
        put(" ".repeat(4 - Math.min(3, n.length())) + n, c);
    }

    /** One line: the top causes of death so far. */
    void causes() {
        at(25, PANEL_COL);
        put("Top deaths: ", WHITE);
        boolean[] used = new boolean[Afflictions.COUNT];
        for (int row = 0; row < 3; row++) {
            int best = -1, bestN = 0;
            for (int id = 0; id < Afflictions.COUNT; id++)
                if (!used[id] && w.causeCount[id] > bestN) { bestN = w.causeCount[id]; best = id; }
            if (best == -1) break;
            used[best] = true;
            String nm = Afflictions.TABLE[best].name;
            if (nm.length() > 11) nm = nm.substring(0, 11);
            put(nm + " " + bestN + (row < 2 ? ", " : ""));
        }
    }

    void pad(String s, int width, Color c) { put(s, c); for (int i = s.length(); i < width; i++) put(" "); }
    void pctShort(int v) { put(String.valueOf(v / 100), v >= 7000 ? GREEN : v >= 3000 ? YELLOW : RED); }

    int b(int p, int slot) { return w.blood[p * 8 + slot]; }

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
            if (w.alive[p] && w.affN[p] >= 3) { s.sel = p; break; }
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
