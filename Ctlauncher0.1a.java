// ==============================================
// CTLauncher 0.1.1 — TLauncher-style GUI
// Run:  java CTlauncher0_1_1.java
// (c) 1999-2026 A.C Holdings / Team Flames
// ==============================================

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// ═════════════════════════════════════════════════════════════
//  CONSTANTS + ENTRY
// ═════════════════════════════════════════════════════════════

public class CTlauncher0_1_1 {
    // ── Palette ──
    static final Color BG_DARK       = new Color(15, 15, 15);
    static final Color BAR_BG        = new Color(10, 10, 10, 200);
    static final Color BAR_BG_SOLID  = new Color(20, 20, 22);
    static final Color INPUT_BG      = new Color(30, 30, 32);
    static final Color INPUT_BORDER  = new Color(60, 60, 65);
    static final Color PLAY_GREEN    = new Color(67, 181, 48);
    static final Color PLAY_HOVER    = new Color(82, 204, 58);
    static final Color PLAY_PRESS    = new Color(52, 150, 38);
    static final Color TAB_ACTIVE    = new Color(255, 255, 255, 40);
    static final Color TAB_HOVER     = new Color(255, 255, 255, 20);
    static final Color TEXT_WHITE    = new Color(240, 240, 240);
    static final Color TEXT_DIM      = new Color(160, 160, 165);
    static final Color TEXT_LABEL    = new Color(190, 190, 195);
    static final Color ACCENT_BLUE   = new Color(70, 160, 255);
    static final Color OVERLAY_BG    = new Color(22, 22, 26, 240);
    static final Color BORDER_DIM    = new Color(55, 55, 60);

    static final String MANIFEST_URL =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    static final String RESOURCES_URL =
        "https://resources.download.minecraft.net/";
    static final String JAVA_ALL_URL =
        "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

    static String gameDir() {
        return System.getProperty("user.home") + File.separator + ".ctlauncher";
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new CTLauncherFrame().setVisible(true));
    }
}

// ═════════════════════════════════════════════════════════════
//  MINI JSON PARSER
// ═════════════════════════════════════════════════════════════

class Json {
    private final String s;
    private int p;
    private Json(String s) { this.s = s; }
    static Object parse(String json) { return new Json(json).value(); }
    @SuppressWarnings("unchecked")
    static Map<String,Object> obj(Object o) { return (o instanceof Map) ? (Map<String,Object>)o : null; }
    @SuppressWarnings("unchecked")
    static List<Object> arr(Object o) { return (o instanceof List) ? (List<Object>)o : null; }
    static String str(Map<String,Object> m, String k) {
        if (m == null) return null; Object v = m.get(k); return v == null ? null : v.toString();
    }
    static long num(Map<String,Object> m, String k) {
        if (m == null) return 0; Object v = m.get(k);
        return (v instanceof Number) ? ((Number)v).longValue() : 0;
    }
    static Map<String,Object> sub(Map<String,Object> m, String k) {
        return m == null ? null : obj(m.get(k));
    }
    static List<Object> subarr(Map<String,Object> m, String k) {
        return m == null ? null : arr(m.get(k));
    }
    private void ws() { while (p < s.length() && s.charAt(p) <= ' ') p++; }
    private char peek() { ws(); return p < s.length() ? s.charAt(p) : 0; }
    private char next() { ws(); return p < s.length() ? s.charAt(p++) : 0; }
    private Object value() {
        char c = peek();
        if (c == '{') return object(); if (c == '[') return array();
        if (c == '"') return string();
        if (c == 't') { p += 4; return Boolean.TRUE; }
        if (c == 'f') { p += 5; return Boolean.FALSE; }
        if (c == 'n') { p += 4; return null; }
        return number();
    }
    private Map<String,Object> object() {
        Map<String,Object> m = new LinkedHashMap<>(); next();
        while (peek() != '}') { String key = string(); next(); m.put(key, value()); if (peek() == ',') next(); }
        next(); return m;
    }
    private List<Object> array() {
        List<Object> a = new ArrayList<>(); next();
        while (peek() != ']') { a.add(value()); if (peek() == ',') next(); }
        next(); return a;
    }
    private String string() {
        next(); StringBuilder sb = new StringBuilder();
        while (p < s.length()) {
            char c = s.charAt(p++);
            if (c == '"') return sb.toString();
            if (c == '\\') { char e = s.charAt(p++);
                switch (e) {
                    case '"': case '\\': case '/': sb.append(e); break;
                    case 'n': sb.append('\n'); break; case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'u': sb.append((char) Integer.parseInt(s.substring(p, p+4), 16)); p += 4; break;
                    default: sb.append(e);
                }
            } else sb.append(c);
        }
        return sb.toString();
    }
    private Number number() {
        ws(); int start = p; boolean flt = false;
        if (p < s.length() && s.charAt(p) == '-') p++;
        while (p < s.length() && Character.isDigit(s.charAt(p))) p++;
        if (p < s.length() && s.charAt(p) == '.') { flt = true; p++;
            while (p < s.length() && Character.isDigit(s.charAt(p))) p++; }
        if (p < s.length() && (s.charAt(p) == 'e' || s.charAt(p) == 'E')) { flt = true; p++;
            if (p < s.length() && (s.charAt(p) == '+' || s.charAt(p) == '-')) p++;
            while (p < s.length() && Character.isDigit(s.charAt(p))) p++; }
        String t = s.substring(start, p);
        if (flt) return Double.parseDouble(t);
        long v = Long.parseLong(t);
        if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) return (int) v;
        return v;
    }
}

// ═════════════════════════════════════════════════════════════
//  NET UTILITIES
// ═════════════════════════════════════════════════════════════

class Net {
    static String fetchString(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "CTLauncher/0.1.1");
        try (InputStream in = c.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally { c.disconnect(); }
    }
    static boolean download(String url, File dest, long expectedSize,
                            ProgressCallback cb) throws IOException {
        if (dest.exists() && (expectedSize <= 0 || dest.length() == expectedSize)) return false;
        dest.getParentFile().mkdirs();
        File tmp = new File(dest.getPath() + ".tmp");
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "CTLauncher/0.1.1");
        long total = c.getContentLengthLong(); if (total <= 0) total = expectedSize;
        try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192]; long done = 0; int n;
            while ((n = in.read(buf)) != -1) { out.write(buf, 0, n); done += n; if (cb != null) cb.progress(done, total); }
        } finally { c.disconnect(); }
        Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
    interface ProgressCallback { void progress(long done, long total); }
}

// ═════════════════════════════════════════════════════════════
//  PROCEDURAL MINECRAFT BACKGROUND
// ═════════════════════════════════════════════════════════════

class McBackground {
    private static BufferedImage cached;
    private static int cw, ch;

    static BufferedImage get(int w, int h) {
        if (cached != null && cw == w && ch == h) return cached;
        cw = w; ch = h;
        cached = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cached.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ── Sky gradient ──
        GradientPaint sky = new GradientPaint(0, 0, new Color(25, 30, 60),
                                               0, h * 0.6f, new Color(60, 100, 160));
        g.setPaint(sky);
        g.fillRect(0, 0, w, h);

        // ── Stars ──
        Random rng = new Random(42);
        g.setColor(new Color(255, 255, 255, 60));
        for (int i = 0; i < 80; i++) {
            int sx = rng.nextInt(w), sy = rng.nextInt((int)(h * 0.35));
            int ss = 1 + rng.nextInt(2);
            g.fillOval(sx, sy, ss, ss);
        }

        // ── Moon ──
        int moonX = (int)(w * 0.78), moonY = (int)(h * 0.10), moonR = 40;
        g.setColor(new Color(230, 230, 210, 180));
        g.fillOval(moonX, moonY, moonR, moonR);
        g.setColor(new Color(200, 200, 180, 80));
        g.fillOval(moonX + 8, moonY + 5, 10, 10);
        g.fillOval(moonX + 20, moonY + 18, 7, 7);

        // ── Horizon glow ──
        int horizonY = (int)(h * 0.55);
        GradientPaint glow = new GradientPaint(0, horizonY - 40, new Color(80, 60, 30, 0),
                                                0, horizonY, new Color(100, 70, 40, 60));
        g.setPaint(glow);
        g.fillRect(0, horizonY - 40, w, 50);

        // ── Terrain layers (pixel block style) ──
        int blockSize = Math.max(6, w / 160);
        int cols = w / blockSize + 2;
        int groundY = (int)(h * 0.55);

        // Hill profile
        double[] heights = new double[cols];
        for (int i = 0; i < cols; i++) {
            heights[i] = groundY
                + Math.sin(i * 0.04) * 30
                + Math.sin(i * 0.11 + 2) * 15
                + Math.sin(i * 0.02 - 1) * 45;
        }

        Color grassTop  = new Color(75, 140, 50);
        Color grassDark = new Color(55, 110, 35);
        Color dirt       = new Color(120, 85, 55);
        Color dirtDark   = new Color(95, 68, 42);
        Color stone      = new Color(80, 80, 85);
        Color stoneDark  = new Color(60, 60, 65);

        for (int col = 0; col < cols; col++) {
            int bx = col * blockSize;
            int topY = (int) heights[col];

            // Grass layer (2 blocks)
            for (int row = 0; row < 2; row++) {
                int by = topY + row * blockSize;
                Color c = (col + row) % 3 == 0 ? grassDark : grassTop;
                g.setColor(c);
                g.fillRect(bx, by, blockSize, blockSize);
            }
            // Dirt layer (4 blocks)
            for (int row = 2; row < 6; row++) {
                int by = topY + row * blockSize;
                Color c = (col + row) % 4 == 0 ? dirtDark : dirt;
                g.setColor(c);
                g.fillRect(bx, by, blockSize, blockSize);
            }
            // Stone to bottom
            for (int row = 6; topY + row * blockSize < h + blockSize; row++) {
                int by = topY + row * blockSize;
                Color c = (col + row) % 5 == 0 ? stoneDark : stone;
                g.setColor(c);
                g.fillRect(bx, by, blockSize, blockSize);
            }
        }

        // ── Trees ──
        int[] treePositions = {(int)(w*0.08), (int)(w*0.22), (int)(w*0.38),
                               (int)(w*0.62), (int)(w*0.75), (int)(w*0.90)};
        Color trunk = new Color(85, 60, 35);
        Color leaf  = new Color(40, 110, 30);
        Color leafH = new Color(50, 130, 40);

        for (int tx : treePositions) {
            int ci = Math.min(cols - 1, Math.max(0, tx / blockSize));
            int treeBase = (int) heights[ci];
            int tw = blockSize * 2, th = blockSize * 7;
            // Trunk
            g.setColor(trunk);
            g.fillRect(tx, treeBase - th, tw, th);
            // Canopy
            int canopyW = blockSize * 7, canopyH = blockSize * 5;
            int cx = tx + tw / 2 - canopyW / 2;
            int cy = treeBase - th - canopyH + blockSize;
            for (int br = 0; br < canopyH / blockSize; br++) {
                for (int bc = 0; bc < canopyW / blockSize; bc++) {
                    // Rough oval shape
                    double dx = (bc - canopyW / blockSize / 2.0) / (canopyW / blockSize / 2.0);
                    double dy = (br - canopyH / blockSize / 2.0) / (canopyH / blockSize / 2.0);
                    if (dx * dx + dy * dy < 0.85 + rng.nextDouble() * 0.3) {
                        g.setColor((bc + br) % 3 == 0 ? leafH : leaf);
                        g.fillRect(cx + bc * blockSize, cy + br * blockSize, blockSize, blockSize);
                    }
                }
            }
        }

        // ── Subtle vignette ──
        RadialGradientPaint vig = new RadialGradientPaint(
            new Point2D.Float(w / 2f, h / 2f),
            Math.max(w, h) * 0.7f,
            new float[]{0f, 0.6f, 1f},
            new Color[]{new Color(0,0,0,0), new Color(0,0,0,40), new Color(0,0,0,140)});
        g.setPaint(vig);
        g.fillRect(0, 0, w, h);

        g.dispose();
        return cached;
    }
}

// ═════════════════════════════════════════════════════════════
//  MAIN FRAME — TLauncher layout
// ═════════════════════════════════════════════════════════════

class CTLauncherFrame extends JFrame {
    final JComboBox<String> versionSelector;
    final JTextField usernameField;
    final JSlider memorySlider;
    final JLabel memoryLabel;
    private final JPanel overlayContainer;
    private final CardLayout overlayCL;
    private String currentOverlay = "none";

    CTLauncherFrame() {
        setTitle("CTLauncher 0.1.1");
        setSize(1050, 620);
        setMinimumSize(new Dimension(900, 520));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setLocationRelativeTo(null);

        // ── Root layered pane ──
        JLayeredPane root = new JLayeredPane();
        root.setLayout(null);
        setContentPane(root);

        // ── Background + main UI layer ──
        JPanel mainLayer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(McBackground.get(getWidth(), getHeight()), 0, 0, null);
            }
        };
        mainLayer.setOpaque(true);
        mainLayer.setBackground(CTlauncher0_1_1.BG_DARK);

        // Title bar
        mainLayer.add(new TitleBarPanel(this), BorderLayout.NORTH);

        // Bottom control bar
        JPanel bottomBar = createBottomBar();
        mainLayer.add(bottomBar, BorderLayout.SOUTH);

        // Center — logo
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        JLabel logo = new JLabel("CTLauncher");
        logo.setFont(new Font("SansSerif", Font.BOLD, 42));
        logo.setForeground(new Color(255, 255, 255, 200));
        centerPanel.add(logo);
        mainLayer.add(centerPanel, BorderLayout.CENTER);

        root.add(mainLayer, JLayeredPane.DEFAULT_LAYER);

        // ── Overlay layer (settings/about) ──
        overlayContainer = new JPanel();
        overlayCL = new CardLayout();
        overlayContainer.setLayout(overlayCL);
        overlayContainer.setOpaque(false);

        // Empty card
        JPanel emptyCard = new JPanel();
        emptyCard.setOpaque(false);
        overlayContainer.add(emptyCard, "none");
        overlayContainer.add(new SettingsOverlay(this), "settings");
        overlayContainer.add(new VersionsOverlay(this), "versions");
        overlayContainer.add(new AboutOverlay(), "about");

        root.add(overlayContainer, JLayeredPane.PALETTE_LAYER);

        // ── Resize handler ──
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                Dimension sz = root.getSize();
                mainLayer.setBounds(0, 0, sz.width, sz.height);
                overlayContainer.setBounds(0, 0, sz.width, sz.height);
            }
        });

        // ── Init data fields ──
        versionSelector = (JComboBox<String>) findComponent(bottomBar, JComboBox.class);
        usernameField = (JTextField) findComponent(bottomBar, JTextField.class);

        // Memory slider/label stored from createBottomBar
        memorySlider = memSlider;
        memoryLabel = memLabel;

        // ── Async-load versions ──
        new Thread(() -> {
            try {
                String json = Net.fetchString(CTlauncher0_1_1.MANIFEST_URL);
                Map<String,Object> m = Json.obj(Json.parse(json));
                List<Object> vers = Json.subarr(m, "versions");
                List<String> releases = new ArrayList<>();
                for (Object v : vers) {
                    Map<String,Object> vm = Json.obj(v);
                    if ("release".equals(Json.str(vm, "type")))
                        releases.add(Json.str(vm, "id"));
                }
                SwingUtilities.invokeLater(() -> {
                    if (versionSelector != null) {
                        versionSelector.removeAllItems();
                        for (String r : releases) versionSelector.addItem(r);
                    }
                });
            } catch (Exception ignored) {}
        }, "CTL-Manifest").start();
    }

    // Temp references set during createBottomBar
    private JSlider memSlider;
    private JLabel memLabel;

    void toggleOverlay(String name) {
        if (currentOverlay.equals(name)) {
            overlayCL.show(overlayContainer, "none");
            currentOverlay = "none";
        } else {
            overlayCL.show(overlayContainer, name);
            currentOverlay = name;
        }
        overlayContainer.repaint();
    }

    @SuppressWarnings("unchecked")
    private <T> T findComponent(Container c, Class<T> type) {
        for (Component comp : c.getComponents()) {
            if (type.isInstance(comp)) return (T) comp;
            if (comp instanceof Container) {
                T found = findComponent((Container) comp, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════
    //  BOTTOM BAR — TLauncher style
    // ═══════════════════════════════════════════

    private JPanel createBottomBar() {
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(CTlauncher0_1_1.BAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Top highlight line
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 120));
        bar.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 12, 8, 12);
        g.gridy = 0;
        g.anchor = GridBagConstraints.CENTER;

        // ── Username ──
        JPanel userPanel = new JPanel(new BorderLayout(0, 2));
        userPanel.setOpaque(false);
        JLabel userLbl = smallLabel("USERNAME");
        JTextField userField = styledField("Player", 14);
        userPanel.add(userLbl, BorderLayout.NORTH);
        userPanel.add(userField, BorderLayout.CENTER);

        g.gridx = 0; g.weightx = 0.2; g.fill = GridBagConstraints.HORIZONTAL;
        bar.add(userPanel, g);

        // ── Version selector ──
        JPanel verPanel = new JPanel(new BorderLayout(0, 2));
        verPanel.setOpaque(false);
        JLabel verLbl = smallLabel("VERSION");
        JComboBox<String> verBox = new JComboBox<>(
            new String[]{"1.21.4","1.21.3","1.21.1","1.20.6","1.20.4",
                          "1.20.2","1.19.4","1.18.2","1.16.5","1.12.2"});
        verBox.setBackground(CTlauncher0_1_1.INPUT_BG);
        verBox.setForeground(CTlauncher0_1_1.TEXT_WHITE);
        verBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        verBox.setPreferredSize(new Dimension(160, 36));
        verBox.setBorder(new LineBorder(CTlauncher0_1_1.INPUT_BORDER, 1));
        verPanel.add(verLbl, BorderLayout.NORTH);
        verPanel.add(verBox, BorderLayout.CENTER);

        g.gridx = 1; g.weightx = 0.15;
        bar.add(verPanel, g);

        // ── PLAY button ──
        JButton playBtn = new JButton("PLAY") {
            boolean hover = false, press = false;
            {
                setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
                setFont(new Font("SansSerif", Font.BOLD, 26));
                setForeground(Color.WHITE);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(220, 60));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e)  { hover = true; repaint(); }
                    public void mouseExited(MouseEvent e)   { hover = false; press = false; repaint(); }
                    public void mousePressed(MouseEvent e)  { press = true; repaint(); }
                    public void mouseReleased(MouseEvent e) { press = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g1) {
                Graphics2D g2 = (Graphics2D) g1.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = press ? CTlauncher0_1_1.PLAY_PRESS
                         : hover ? CTlauncher0_1_1.PLAY_HOVER
                                 : CTlauncher0_1_1.PLAY_GREEN;
                // Shadow
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, 12, 12);
                // Main button
                GradientPaint gp = new GradientPaint(0, 0, bg.brighter(),
                                                      0, getHeight(), bg.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 10, 10);
                // Top highlight
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(2, 2, getWidth() - 6, getHeight() / 2 - 4, 8, 8);
                // Text
                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                // Text shadow
                g2.setColor(new Color(0, 60, 0, 120));
                g2.drawString(getText(), tx + 1, ty + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        playBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            if (user.isEmpty()) user = "Player";
            String ver = (String) verBox.getSelectedItem();
            int ram = memSlider.getValue();
            GameLauncher.launchAsync(user, ver, ram, this);
        });

        g.gridx = 2; g.weightx = 0.3; g.fill = GridBagConstraints.NONE;
        bar.add(playBtn, g);

        // ── Memory slider ──
        JPanel memPanel = new JPanel(new BorderLayout(0, 2));
        memPanel.setOpaque(false);
        memLabel = new JLabel("RAM: 4 GB");
        memLabel.setForeground(CTlauncher0_1_1.TEXT_LABEL);
        memLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        memSlider = new JSlider(1, 16, 4);
        memSlider.setOpaque(false);
        memSlider.setForeground(CTlauncher0_1_1.PLAY_GREEN);
        memSlider.setPreferredSize(new Dimension(140, 28));
        memSlider.addChangeListener(ev ->
            memLabel.setText("RAM: " + memSlider.getValue() + " GB"));

        memPanel.add(memLabel, BorderLayout.NORTH);
        memPanel.add(memSlider, BorderLayout.CENTER);

        g.gridx = 3; g.weightx = 0.2; g.fill = GridBagConstraints.HORIZONTAL;
        bar.add(memPanel, g);

        return bar;
    }

    static JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(CTlauncher0_1_1.TEXT_DIM);
        return l;
    }

    static JTextField styledField(String def, int fontSize) {
        JTextField f = new JTextField(def, 14);
        f.setBackground(CTlauncher0_1_1.INPUT_BG);
        f.setForeground(CTlauncher0_1_1.TEXT_WHITE);
        f.setCaretColor(CTlauncher0_1_1.TEXT_WHITE);
        f.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(CTlauncher0_1_1.INPUT_BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setPreferredSize(new Dimension(160, 36));
        return f;
    }
}

// ═════════════════════════════════════════════════════════════
//  TITLE BAR — Transparent top strip
// ═════════════════════════════════════════════════════════════

class TitleBarPanel extends JPanel {
    private final JFrame frame;
    private int mx, my;

    TitleBarPanel(CTLauncherFrame f) {
        this.frame = f;
        setOpaque(false);
        setPreferredSize(new Dimension(0, 38));
        setLayout(new BorderLayout());

        // Left — brand
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        left.setOpaque(false);
        JLabel brand = new JLabel("  CTLauncher 0.1.1");
        brand.setForeground(new Color(255, 255, 255, 180));
        brand.setFont(new Font("SansSerif", Font.BOLD, 13));
        left.add(brand);
        add(left, BorderLayout.WEST);

        // Center — tabs
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tabs.setOpaque(false);
        tabs.add(tabBtn("SETTINGS", () -> f.toggleOverlay("settings")));
        tabs.add(tabBtn("VERSIONS", () -> f.toggleOverlay("versions")));
        tabs.add(tabBtn("ABOUT",    () -> f.toggleOverlay("about")));
        add(tabs, BorderLayout.CENTER);

        // Right — window controls
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(ctrlBtn("\u2014", CTlauncher0_1_1.TEXT_DIM, e -> frame.setState(JFrame.ICONIFIED)));
        right.add(ctrlBtn("\u2715", new Color(230, 70, 70), e -> System.exit(0)));
        add(right, BorderLayout.EAST);

        // Drag support
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { mx = e.getX(); my = e.getY(); }
        });
        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                frame.setLocation(e.getXOnScreen() - mx, e.getYOnScreen() - my);
            }
        });
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 8));
        g2.fillRect(0, getHeight() - 1, getWidth(), 1);
        g2.dispose();
    }

    private JButton tabBtn(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setForeground(new Color(220, 220, 220, 200));
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(100, 38));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(CTlauncher0_1_1.TAB_HOVER); b.setOpaque(true); b.repaint(); }
            public void mouseExited(MouseEvent e)  { b.setOpaque(false); b.repaint(); }
        });
        return b;
    }

    private JButton ctrlBtn(String txt, Color fg, ActionListener al) {
        JButton b = new JButton(txt);
        b.setFont(new Font("SansSerif", Font.PLAIN, 15));
        b.setForeground(fg);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setPreferredSize(new Dimension(46, 38));
        b.addActionListener(al);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(60,60,60)); b.setOpaque(true); b.repaint(); }
            public void mouseExited(MouseEvent e)  { b.setOpaque(false); b.repaint(); }
        });
        return b;
    }
}

// ═════════════════════════════════════════════════════════════
//  OVERLAY PANELS
// ═════════════════════════════════════════════════════════════

class SettingsOverlay extends JPanel {
    SettingsOverlay(CTLauncherFrame f) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        JPanel card = overlayCard(460, 320, "Settings");
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 20, 6, 20);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridwidth = 2;

        g.gridy = 0;
        card.add(overlayTitle("Settings"), g);

        g.gridy = 1;
        card.add(CTLauncherFrame.smallLabel("GAME DIRECTORY"), g);
        g.gridy = 2;
        card.add(CTLauncherFrame.styledField(CTlauncher0_1_1.gameDir(), 12), g);

        g.gridy = 3;
        card.add(CTLauncherFrame.smallLabel("CUSTOM JAVA ARGUMENTS"), g);
        g.gridy = 4;
        card.add(CTLauncherFrame.styledField("-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions", 12), g);

        g.gridy = 5;
        JCheckBox cb = new JCheckBox("Keep launcher open after game starts");
        cb.setForeground(CTlauncher0_1_1.TEXT_DIM);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setOpaque(false);
        card.add(cb, g);

        g.gridy = 6;
        JLabel javaInfo = new JLabel("Java: " + System.getProperty("java.version")
            + " (" + System.getProperty("os.arch") + ")");
        javaInfo.setForeground(CTlauncher0_1_1.TEXT_DIM);
        javaInfo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        card.add(javaInfo, g);

        add(card);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    static JPanel overlayCard(int w, int h, String name) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CTlauncher0_1_1.OVERLAY_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(CTlauncher0_1_1.BORDER_DIM);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(w, h));
        return card;
    }

    static JLabel overlayTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 20));
        l.setForeground(CTlauncher0_1_1.TEXT_WHITE);
        l.setBorder(new EmptyBorder(8, 0, 8, 0));
        return l;
    }
}

class VersionsOverlay extends JPanel {
    VersionsOverlay(CTLauncherFrame f) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        JPanel card = SettingsOverlay.overlayCard(500, 380, "Versions");
        card.setLayout(new BorderLayout(0, 0));

        JLabel title = SettingsOverlay.overlayTitle("Version Management");
        title.setBorder(new EmptyBorder(14, 20, 8, 20));
        card.add(title, BorderLayout.NORTH);

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Loading versions from Mojang...");

        JList<String> list = new JList<>(model);
        list.setBackground(new Color(16, 16, 20));
        list.setForeground(CTlauncher0_1_1.ACCENT_BLUE);
        list.setSelectionBackground(CTlauncher0_1_1.PLAY_GREEN);
        list.setSelectionForeground(Color.WHITE);
        list.setFont(new Font("SansSerif", Font.PLAIN, 14));
        list.setFixedCellHeight(32);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> l, Object v, int i, boolean sel, boolean foc) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, sel, foc);
                c.setBorder(new EmptyBorder(4, 16, 4, 16));
                c.setBackground(sel ? CTlauncher0_1_1.PLAY_GREEN : new Color(16, 16, 20));
                c.setForeground(sel ? Color.WHITE : CTlauncher0_1_1.ACCENT_BLUE);
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(new EmptyBorder(4, 16, 16, 16));
        sp.getViewport().setBackground(new Color(16, 16, 20));
        card.add(sp, BorderLayout.CENTER);

        add(card);

        new Thread(() -> {
            try {
                String json = Net.fetchString(CTlauncher0_1_1.MANIFEST_URL);
                Map<String,Object> m = Json.obj(Json.parse(json));
                List<Object> vers = Json.subarr(m, "versions");
                List<String> entries = new ArrayList<>();
                for (Object v : vers) {
                    Map<String,Object> vm = Json.obj(v);
                    if ("release".equals(Json.str(vm, "type")))
                        entries.add(Json.str(vm, "id") + "  \u2014  Release");
                }
                SwingUtilities.invokeLater(() -> {
                    model.clear();
                    for (String e : entries) model.addElement(e);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    model.clear(); model.addElement("Failed: " + ex.getMessage());
                });
            }
        }, "CTL-VerList").start();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}

class AboutOverlay extends JPanel {
    AboutOverlay() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        JPanel card = SettingsOverlay.overlayCard(400, 220, "About");
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 20, 6, 20);
        g.anchor = GridBagConstraints.CENTER;

        g.gridy = 0;
        JLabel t = new JLabel("CTLauncher 0.1.1");
        t.setFont(new Font("SansSerif", Font.BOLD, 24));
        t.setForeground(CTlauncher0_1_1.TEXT_WHITE);
        card.add(t, g);

        g.gridy = 1;
        JLabel copy = new JLabel("\u00a9 1999-2026 A.C Holdings / Team Flames");
        copy.setFont(new Font("SansSerif", Font.PLAIN, 12));
        copy.setForeground(CTlauncher0_1_1.PLAY_GREEN);
        card.add(copy, g);

        g.gridy = 2;
        JLabel desc = new JLabel("<html><center>Auto-downloading Minecraft launcher."
            + "<br>Fetches client, libraries, and assets from Mojang."
            + "<br>Offline mode. Academic sandbox.</center></html>");
        desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        desc.setForeground(CTlauncher0_1_1.TEXT_DIM);
        card.add(desc, g);

        add(card);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}

// ═════════════════════════════════════════════════════════════
//  PROGRESS DIALOG
// ═════════════════════════════════════════════════════════════

class ProgressDialog extends JDialog {
    private final JLabel statusLabel;
    private final JProgressBar bar;
    private final JLabel fileLabel;

    ProgressDialog(JFrame parent) {
        super(parent, "Preparing Minecraft...", false);
        setSize(460, 150);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setUndecorated(true);

        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CTlauncher0_1_1.OVERLAY_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(CTlauncher0_1_1.BORDER_DIM);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        statusLabel = new JLabel("Starting...");
        statusLabel.setForeground(CTlauncher0_1_1.PLAY_GREEN);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        p.add(statusLabel);
        p.add(Box.createVerticalStrut(10));

        bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setBackground(CTlauncher0_1_1.INPUT_BG);
        bar.setForeground(CTlauncher0_1_1.PLAY_GREEN);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        p.add(bar);
        p.add(Box.createVerticalStrut(6));

        fileLabel = new JLabel(" ");
        fileLabel.setForeground(CTlauncher0_1_1.TEXT_DIM);
        fileLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        fileLabel.setAlignmentX(LEFT_ALIGNMENT);
        p.add(fileLabel);

        setContentPane(p);
        setBackground(new Color(0, 0, 0, 0));
    }

    void status(String msg) {
        SwingUtilities.invokeLater(() -> { statusLabel.setText(msg); bar.setIndeterminate(true); });
    }

    void fileProgress(String name, long done, long total) {
        SwingUtilities.invokeLater(() -> {
            bar.setIndeterminate(false);
            if (total > 0) { int pct = (int)(done * 100 / total); bar.setValue(pct); bar.setString(pct + "%"); }
            fileLabel.setText(name + "  (" + (done/1024) + " / " + (total/1024) + " KB)");
        });
    }
}

// ═════════════════════════════════════════════════════════════
//  DOWNLOAD + LAUNCH ENGINE (all previous fixes preserved)
// ═════════════════════════════════════════════════════════════

class GameLauncher {

    static String osName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac") || os.contains("osx")) return "osx";
        return "linux";
    }

    static boolean osMatches(String mojangName) {
        if (mojangName == null) return false;
        String cur = osName();
        if (cur.equals(mojangName)) return true;
        if ("osx".equals(cur) && "macos".equals(mojangName)) return true;
        if ("macos".equals(cur) && "osx".equals(mojangName)) return true;
        return false;
    }

    static void launchAsync(String username, String versionId, int ramGB, JFrame parent) {
        new Thread(() -> {
            ProgressDialog dlg = new ProgressDialog(parent);
            try {
                SwingUtilities.invokeLater(() -> dlg.setVisible(true));

                String base   = CTlauncher0_1_1.gameDir();
                String verDir = base + File.separator + "versions" + File.separator + versionId;
                String libDir = base + File.separator + "libraries";
                String assDir = base + File.separator + "assets";
                String natDir = verDir + File.separator + "natives";
                new File(verDir).mkdirs(); new File(natDir).mkdirs();
                new File(libDir).mkdirs(); new File(assDir).mkdirs();

                // 1. Fetch manifest
                dlg.status("Fetching version manifest...");
                String manifestJson = Net.fetchString(CTlauncher0_1_1.MANIFEST_URL);
                Map<String,Object> manifest = Json.obj(Json.parse(manifestJson));
                List<Object> versions = Json.subarr(manifest, "versions");

                String versionUrl = null;
                for (Object v : versions) {
                    Map<String,Object> vm = Json.obj(v);
                    if (versionId.equals(Json.str(vm, "id"))) { versionUrl = Json.str(vm, "url"); break; }
                }
                if (versionUrl == null) throw new RuntimeException("Version " + versionId + " not found");

                // 2. Fetch version JSON
                dlg.status("Fetching " + versionId + " metadata...");
                String vJson = Net.fetchString(versionUrl);
                Map<String,Object> ver = Json.obj(Json.parse(vJson));
                Files.writeString(Path.of(verDir, versionId + ".json"), vJson);
                String mainClass = Json.str(ver, "mainClass");

                // 3. Download client.jar
                Map<String,Object> clientDl = Json.sub(Json.sub(ver, "downloads"), "client");
                String clientUrl = Json.str(clientDl, "url");
                long clientSize  = Json.num(clientDl, "size");
                File clientJar   = new File(verDir + File.separator + versionId + ".jar");
                dlg.status("Downloading " + versionId + ".jar ...");
                Net.download(clientUrl, clientJar, clientSize,
                    (done, total) -> dlg.fileProgress(versionId + ".jar", done, total));

                // 4. Download libraries
                List<Object> libs = Json.subarr(ver, "libraries");
                List<String> classpathEntries = new ArrayList<>();
                classpathEntries.add(clientJar.getAbsolutePath());

                int libIdx = 0;
                for (Object lo : libs) {
                    Map<String,Object> lib = Json.obj(lo); libIdx++;
                    if (!rulesAllow(lib)) continue;
                    Map<String,Object> libDownloads = Json.sub(lib, "downloads");
                    if (libDownloads == null) continue;

                    Map<String,Object> artifact = Json.sub(libDownloads, "artifact");
                    if (artifact != null) {
                        String path = Json.str(artifact, "path");
                        String url  = Json.str(artifact, "url");
                        long size   = Json.num(artifact, "size");
                        File dest   = new File(libDir + File.separator + path.replace("/", File.separator));
                        String name = dest.getName();
                        int li = libIdx;
                        dlg.status("Libraries (" + li + "/" + libs.size() + ") " + name);
                        Net.download(url, dest, size, (done, total) -> dlg.fileProgress(name, done, total));
                        classpathEntries.add(dest.getAbsolutePath());
                        if (path != null && path.contains("natives-")) extractZip(dest, new File(natDir));
                    }

                    Map<String,Object> natives = Json.sub(lib, "natives");
                    if (natives != null) {
                        String classifier = Json.str(natives, osName());
                        if (classifier == null && "osx".equals(osName())) classifier = Json.str(natives, "macos");
                        if (classifier != null) {
                            classifier = classifier.replace("${arch}",
                                System.getProperty("os.arch").contains("64") ? "64" : "32");
                            Map<String,Object> classifiers = Json.sub(libDownloads, "classifiers");
                            if (classifiers != null) {
                                Map<String,Object> natArt = Json.sub(classifiers, classifier);
                                if (natArt != null) {
                                    File dest = new File(libDir + File.separator +
                                        Json.str(natArt, "path").replace("/", File.separator));
                                    dlg.status("Natives: " + dest.getName());
                                    Net.download(Json.str(natArt, "url"), dest, Json.num(natArt, "size"), null);
                                    extractZip(dest, new File(natDir));
                                }
                            }
                        }
                    }
                }

                // 5. Download asset index
                Map<String,Object> assetIndex = Json.sub(ver, "assetIndex");
                String assetId = Json.str(assetIndex, "id");
                File indexDir  = new File(assDir + File.separator + "indexes"); indexDir.mkdirs();
                File indexFile = new File(indexDir, assetId + ".json");
                dlg.status("Asset index " + assetId + "...");
                Net.download(Json.str(assetIndex, "url"), indexFile, Json.num(assetIndex, "size"), null);

                // 6. Download asset objects
                Map<String,Object> objects = Json.sub(Json.obj(Json.parse(Files.readString(indexFile.toPath()))), "objects");
                if (objects != null) {
                    File objDir = new File(assDir + File.separator + "objects"); objDir.mkdirs();
                    int total = objects.size(), count = 0;
                    for (Map.Entry<String,Object> e : objects.entrySet()) {
                        Map<String,Object> asset = Json.obj(e.getValue());
                        String hash = Json.str(asset, "hash"); String pre = hash.substring(0, 2);
                        File dest = new File(objDir, pre + File.separator + hash); count++;
                        if (!dest.exists()) {
                            if (count % 50 == 0 || count == total) dlg.status("Assets (" + count + "/" + total + ")...");
                            Net.download(CTlauncher0_1_1.RESOURCES_URL + pre + "/" + hash, dest, Json.num(asset, "size"), null);
                        }
                    }
                }

                // 7. Resolve Java runtime (auto-download if needed)
                String javaPath = ensureJavaRuntime(ver, base, dlg);
                if (javaPath == null) {
                    // No specific runtime needed or download failed — use system Java
                    javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                    if (osName().equals("windows")) javaPath += ".exe";
                }
                dlg.status("Launching " + versionId + "...");
                String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()).toString();
                String cp = String.join(File.pathSeparator, classpathEntries);

                Map<String,String> vars = new LinkedHashMap<>();
                vars.put("${auth_player_name}", username);
                vars.put("${version_name}", versionId);
                vars.put("${game_directory}", base);
                vars.put("${assets_root}", assDir);
                vars.put("${assets_index_name}", assetId);
                vars.put("${auth_uuid}", uuid);
                vars.put("${auth_access_token}", "0");
                vars.put("${user_type}", "legacy");
                vars.put("${version_type}", Json.str(ver, "type") != null ? Json.str(ver, "type") : "release");
                vars.put("${user_properties}", "{}");
                vars.put("${auth_session}", "token:0");
                vars.put("${game_assets}", assDir);
                vars.put("${natives_directory}", natDir);
                vars.put("${launcher_name}", "CTLauncher");
                vars.put("${launcher_version}", "0.1.1");
                vars.put("${classpath}", cp);
                vars.put("${classpath_separator}", File.pathSeparator);
                vars.put("${library_directory}", libDir);
                vars.put("${resolution_width}", "854");
                vars.put("${resolution_height}", "480");
                vars.put("${clientid}", "");
                vars.put("${auth_xuid}", "");
                vars.put("${path_separator}", File.pathSeparator);

                List<String> cmd = new ArrayList<>();
                cmd.add(javaPath);

                Map<String,Object> arguments = Json.sub(ver, "arguments");
                if (arguments != null) {
                    List<Object> jvmArgs = Json.subarr(arguments, "jvm");
                    if (jvmArgs != null) {
                        for (Object arg : jvmArgs) {
                            if (arg instanceof String) { cmd.add(subVars((String) arg, vars)); }
                            else {
                                Map<String,Object> argObj = Json.obj(arg);
                                if (argObj != null && argRulesAllow(argObj)) {
                                    Object val = argObj.get("value");
                                    if (val instanceof String) cmd.add(subVars((String) val, vars));
                                    else { List<Object> vals = Json.arr(val);
                                        if (vals != null) for (Object v2 : vals) if (v2 instanceof String) cmd.add(subVars((String) v2, vars)); }
                                }
                            }
                        }
                    }
                } else {
                    if ("osx".equals(osName())) cmd.add("-XstartOnFirstThread");
                    cmd.add("-Djava.library.path=" + natDir);
                    cmd.add("-Dminecraft.launcher.brand=CTLauncher");
                    cmd.add("-Dminecraft.launcher.version=0.1.1");
                }

                cmd.removeIf(a -> a.startsWith("-Xmx")); cmd.removeIf(a -> a.startsWith("-Xms"));
                cmd.add(1, "-Xmx" + ramGB + "G"); cmd.add(2, "-Xms512M");

                if (!cmd.contains("-cp") && !cmd.contains("-classpath")) { cmd.add("-cp"); cmd.add(cp); }
                cmd.add(mainClass);

                if (arguments != null) {
                    List<Object> gameArgs = Json.subarr(arguments, "game");
                    if (gameArgs != null) {
                        for (Object arg : gameArgs) {
                            if (arg instanceof String) { cmd.add(subVars((String) arg, vars)); }
                            else {
                                Map<String,Object> argObj = Json.obj(arg);
                                if (argObj != null && argRulesAllow(argObj)) {
                                    Object val = argObj.get("value");
                                    if (val instanceof String) cmd.add(subVars((String) val, vars));
                                    else { List<Object> vals = Json.arr(val);
                                        if (vals != null) for (Object v2 : vals) if (v2 instanceof String) cmd.add(subVars((String) v2, vars)); }
                                }
                            }
                        }
                    }
                } else {
                    String mcArgs = Json.str(ver, "minecraftArguments");
                    if (mcArgs != null) { for (String part : mcArgs.split(" ")) cmd.add(subVars(part.trim(), vars)); }
                    else {
                        cmd.add("--username"); cmd.add(username); cmd.add("--version"); cmd.add(versionId);
                        cmd.add("--gameDir"); cmd.add(base); cmd.add("--assetsDir"); cmd.add(assDir);
                        cmd.add("--assetIndex"); cmd.add(assetId); cmd.add("--uuid"); cmd.add(uuid);
                        cmd.add("--accessToken"); cmd.add("0"); cmd.add("--userType"); cmd.add("legacy");
                        cmd.add("--versionType"); cmd.add("release"); cmd.add("--userProperties"); cmd.add("{}");
                    }
                }

                // Strip unresolved vars
                cmd.removeIf(a -> a.contains("${"));

                // Safety net: if ensureJavaRuntime fell back to system Java,
                // filter flags it might not support
                if (cmd.get(0).contains(System.getProperty("java.home"))) {
                    int curJavaVer = currentJavaMajor();
                    cmd.removeIf(a -> a.startsWith("--sun-misc-unsafe-memory-access") && curJavaVer < 23);
                    cmd.removeIf(a -> a.startsWith("--enable-native-access") && curJavaVer < 16);
                }

                System.out.println("=== CTLauncher 0.1.1 ======================");
                System.out.println("Version:  " + versionId);
                System.out.println("Username: " + username + "  (offline)");
                System.out.println("RAM:      " + ramGB + " GB");
                System.out.println("Java:     " + cmd.get(0));
                System.out.println("Main:     " + mainClass);
                System.out.println("===========================================");

                SwingUtilities.invokeLater(() -> dlg.dispose());

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(new File(base));
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                StringBuilder procOutput = new StringBuilder();
                BufferedReader rd = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    System.out.println("[MC] " + line);
                    if (procOutput.length() < 4096) procOutput.append(line).append("\n");
                }
                int exit = proc.waitFor();
                System.out.println("=== Exited (code " + exit + ") ===");

                if (exit != 0) {
                    String out = procOutput.toString();
                    String snippet = out.length() > 800 ? "..." + out.substring(out.length() - 800) : out;
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                        "Minecraft exited with code " + exit + ":\n\n" + snippet,
                        "Launch Error", JOptionPane.ERROR_MESSAGE));
                }

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> dlg.dispose());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Launch failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "CTL-Launch").start();
    }

    // ── Mojang JRE auto-download ───────────────────────────

    /** Returns the Mojang platform key for runtime downloads */
    static String runtimePlatform() {
        String os = osName();
        String arch = System.getProperty("os.arch").toLowerCase();
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        if ("osx".equals(os))     return arm ? "mac-os-arm64" : "mac-os";
        if ("linux".equals(os))   return arm ? "linux-arm64"  : "linux";
        /* windows */             return arm ? "windows-arm64": (arch.contains("64") ? "windows-x64" : "windows-x86");
    }

    /**
     * Ensure the correct Mojang JRE is installed for this MC version.
     * Downloads it to ~/.ctlauncher/runtimes/<component>/ if missing.
     * Returns path to java binary, or null to fall back to system Java.
     */
    static String ensureJavaRuntime(Map<String,Object> ver, String base,
                                    ProgressDialog dlg) {
        try {
            Map<String,Object> jv = Json.sub(ver, "javaVersion");
            if (jv == null) return null; // ancient version, system Java fine

            String component = Json.str(jv, "component");
            if (component == null) return null;

            String rtDir = base + File.separator + "runtimes" + File.separator + component;

            // Already installed?
            String existing = findJavaBin(rtDir);
            if (existing != null) {
                System.out.println("[JRE] Using cached: " + component + " → " + existing);
                return existing;
            }

            int majorVer = (int) Json.num(jv, "majorVersion");
            dlg.status("Downloading Java " + majorVer + " (" + component + ")...");
            System.out.println("[JRE] Downloading Mojang runtime: " + component
                + " (Java " + majorVer + ") for " + runtimePlatform());

            // 1. Fetch Mojang's master runtime list
            String allJson = Net.fetchString(CTlauncher0_1_1.JAVA_ALL_URL);
            Map<String,Object> all = Json.obj(Json.parse(allJson));

            String platform = runtimePlatform();
            Map<String,Object> platRuntimes = Json.sub(all, platform);
            if (platRuntimes == null) platRuntimes = Json.sub(all, "gamecore");
            if (platRuntimes == null) {
                System.err.println("[JRE] No runtimes for platform: " + platform);
                return null;
            }

            List<Object> compList = Json.subarr(platRuntimes, component);
            if (compList == null || compList.isEmpty()) {
                System.err.println("[JRE] Component not found: " + component);
                return null;
            }

            // 2. Get the manifest URL for this component
            Map<String,Object> compEntry = Json.obj(compList.get(0));
            Map<String,Object> manifest  = Json.sub(compEntry, "manifest");
            String manifestUrl = Json.str(manifest, "url");

            Map<String,Object> versionInfo = Json.sub(compEntry, "version");
            String rtVersion = versionInfo != null ? Json.str(versionInfo, "name") : "?";
            System.out.println("[JRE] Runtime version: " + rtVersion);

            // 3. Fetch file manifest
            dlg.status("Fetching Java " + majorVer + " file list...");
            String mJson = Net.fetchString(manifestUrl);
            Map<String,Object> mRoot = Json.obj(Json.parse(mJson));
            Map<String,Object> files = Json.sub(mRoot, "files");
            if (files == null || files.isEmpty()) {
                System.err.println("[JRE] Empty runtime manifest");
                return null;
            }

            // 4. Download all files
            new File(rtDir).mkdirs();
            int total = files.size();
            int count = 0;

            for (Map.Entry<String,Object> entry : files.entrySet()) {
                String path = entry.getKey();
                Map<String,Object> info = Json.obj(entry.getValue());
                String type = Json.str(info, "type");
                count++;

                File dest = new File(rtDir + File.separator +
                    path.replace("/", File.separator));

                if ("directory".equals(type)) {
                    dest.mkdirs();

                } else if ("file".equals(type)) {
                    Map<String,Object> downloads = Json.sub(info, "downloads");
                    if (downloads == null) continue;
                    Map<String,Object> raw = Json.sub(downloads, "raw");
                    if (raw == null) continue;

                    String url  = Json.str(raw, "url");
                    long   size = Json.num(raw, "size");

                    if (count % 40 == 0 || count == total)
                        dlg.status("Java " + majorVer + "  (" + count + "/" + total + " files)");

                    Net.download(url, dest, size, null);

                    // Set executable bit (java, javac, etc.)
                    Object exec = info.get("executable");
                    if (Boolean.TRUE.equals(exec))
                        dest.setExecutable(true, false);

                } else if ("link".equals(type)) {
                    // Symlinks (macOS / Linux)
                    String target = Json.str(info, "target");
                    if (target != null && !"windows".equals(osName())) {
                        dest.getParentFile().mkdirs();
                        try {
                            Files.deleteIfExists(dest.toPath());
                            Files.createSymbolicLink(dest.toPath(), Path.of(target));
                        } catch (Exception ignored) {}
                    }
                }
            }

            String javaBin = findJavaBin(rtDir);
            if (javaBin != null) {
                System.out.println("[JRE] Installed Java " + majorVer + " → " + javaBin);
            } else {
                System.err.println("[JRE] java binary not found after install in " + rtDir);
            }
            return javaBin;

        } catch (Exception e) {
            System.err.println("[JRE] Auto-download failed, falling back to system Java: " + e.getMessage());
            return null;
        }
    }

    /** Find the java binary inside a Mojang runtime directory */
    private static String findJavaBin(String rtDir) {
        // macOS: jre.bundle/Contents/Home/bin/java
        File f = new File(rtDir, "jre.bundle/Contents/Home/bin/java".replace("/", File.separator));
        if (f.isFile()) return f.getAbsolutePath();
        // Linux: bin/java
        f = new File(rtDir, "bin" + File.separator + "java");
        if (f.isFile()) return f.getAbsolutePath();
        // Windows: bin/java.exe
        f = new File(rtDir, "bin" + File.separator + "java.exe");
        if (f.isFile()) return f.getAbsolutePath();
        return null;
    }

    // ── helpers ──────────────────────────────────────────────

    private static String subVars(String arg, Map<String,String> vars) {
        String r = arg;
        for (Map.Entry<String,String> e : vars.entrySet()) r = r.replace(e.getKey(), e.getValue());
        return r;
    }

    private static int currentJavaMajor() {
        String ver = System.getProperty("java.version", "1.8");
        if (ver.startsWith("1.")) ver = ver.substring(2);
        int dot = ver.indexOf('.'); if (dot > 0) ver = ver.substring(0, dot);
        int dash = ver.indexOf('-'); if (dash > 0) ver = ver.substring(0, dash);
        try { return Integer.parseInt(ver); } catch (NumberFormatException e) { return 8; }
    }

    private static boolean argRulesAllow(Map<String,Object> argObj) {
        List<Object> rules = Json.subarr(argObj, "rules");
        if (rules == null) return true;
        boolean allowed = false;
        for (Object ro : rules) {
            Map<String,Object> rule = Json.obj(ro); if (rule == null) continue;
            String action = Json.str(rule, "action");
            Map<String,Object> os = Json.sub(rule, "os");
            Map<String,Object> features = Json.sub(rule, "features");
            if (features != null) continue;
            if (os != null) {
                String name = Json.str(os, "name"); String arch = Json.str(os, "arch");
                if (name != null && !osMatches(name)) continue;
                if (arch != null) { String curArch = System.getProperty("os.arch");
                    boolean is64 = curArch.contains("64") || curArch.contains("aarch64");
                    if ("x86".equals(arch) && is64) continue; }
            }
            allowed = "allow".equals(action);
        }
        return allowed;
    }

    private static boolean rulesAllow(Map<String,Object> lib) {
        List<Object> rules = Json.subarr(lib, "rules");
        if (rules == null) return true;
        boolean allowed = false;
        for (Object ro : rules) {
            Map<String,Object> rule = Json.obj(ro);
            String action = Json.str(rule, "action"); Map<String,Object> os = Json.sub(rule, "os");
            if (os != null) {
                String name = Json.str(os, "name"); String arch = Json.str(os, "arch");
                if (name != null && !osMatches(name)) continue;
                if (arch != null) { String curArch = System.getProperty("os.arch");
                    boolean is64 = curArch.contains("64") || curArch.contains("aarch64");
                    if ("x86".equals(arch) && is64) continue; }
            }
            allowed = "allow".equals(action);
        }
        return allowed;
    }

    private static void extractZip(File zip, File destDir) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.startsWith("META-INF")) continue;
                File out = new File(destDir, name); out.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096]; int n;
                    while ((n = zis.read(buf)) != -1) fos.write(buf, 0, n);
                }
            }
        } catch (IOException e) { System.err.println("Extract failed: " + zip.getName() + ": " + e.getMessage()); }
    }
}
