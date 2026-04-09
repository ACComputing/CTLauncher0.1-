// ==============================================
// CTLauncher 0.2 — Minecraft Launcher with Forge
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CTlauncher0_1_1 {
    static final Color BG_DARK=new Color(22,22,26),BG_PANEL=new Color(30,30,35),BG_INPUT=new Color(38,38,44);
    static final Color PLAY_GREEN=new Color(67,181,48),PLAY_HOVER=new Color(82,204,58),PLAY_PRESS=new Color(52,150,38);
    static final Color TEXT_WHITE=new Color(240,240,240),TEXT_DIM=new Color(140,140,145);
    static final Color ACCENT_CYAN=new Color(0,200,220),ACCENT_BLUE=new Color(70,160,255);
    static final Color BORDER_DIM=new Color(55,55,60),OVERLAY_BG=new Color(28,28,34,245);
    static final Color INPUT_BORDER=new Color(60,60,68),NEWS_BG=new Color(36,36,42);
    static final String MANIFEST_URL="https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    static final String RESOURCES_URL="https://resources.download.minecraft.net/";
    static final String JAVA_ALL_URL="https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";
    static final String FORGE_PROMOS="https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    static String gameDir(){return System.getProperty("user.home")+File.separator+".ctlauncher";}
    public static void main(String[] a){
        try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());}catch(Exception ignored){}
        SwingUtilities.invokeLater(()->new CTLauncherFrame().setVisible(true));
    }
}

class Json{
    private final String s;private int p;private Json(String s){this.s=s;}
    static Object parse(String j){return new Json(j).value();}
    @SuppressWarnings("unchecked") static Map<String,Object> obj(Object o){return(o instanceof Map)?(Map<String,Object>)o:null;}
    @SuppressWarnings("unchecked") static List<Object> arr(Object o){return(o instanceof List)?(List<Object>)o:null;}
    static String str(Map<String,Object> m,String k){if(m==null)return null;Object v=m.get(k);return v==null?null:v.toString();}
    static long num(Map<String,Object> m,String k){if(m==null)return 0;Object v=m.get(k);return(v instanceof Number)?((Number)v).longValue():0;}
    static Map<String,Object> sub(Map<String,Object> m,String k){return m==null?null:obj(m.get(k));}
    static List<Object> subarr(Map<String,Object> m,String k){return m==null?null:arr(m.get(k));}
    private void ws(){while(p<s.length()&&s.charAt(p)<=' ')p++;}
    private char peek(){ws();return p<s.length()?s.charAt(p):0;}
    private char next(){ws();return p<s.length()?s.charAt(p++):0;}
    private Object value(){char c=peek();if(c=='{')return object();if(c=='[')return array();if(c=='"')return string();if(c=='t'){p+=4;return Boolean.TRUE;}if(c=='f'){p+=5;return Boolean.FALSE;}if(c=='n'){p+=4;return null;}return number();}
    private Map<String,Object> object(){Map<String,Object> m=new LinkedHashMap<>();next();while(peek()!='}'){String key=string();next();m.put(key,value());if(peek()==',')next();}next();return m;}
    private List<Object> array(){List<Object> a=new ArrayList<>();next();while(peek()!=']'){a.add(value());if(peek()==',')next();}next();return a;}
    private String string(){next();StringBuilder sb=new StringBuilder();while(p<s.length()){char c=s.charAt(p++);if(c=='"')return sb.toString();if(c=='\\'){char e=s.charAt(p++);switch(e){case'"':case'\\':case'/':sb.append(e);break;case'n':sb.append('\n');break;case't':sb.append('\t');break;case'r':sb.append('\r');break;case'u':sb.append((char)Integer.parseInt(s.substring(p,p+4),16));p+=4;break;default:sb.append(e);}}else sb.append(c);}return sb.toString();}
    private Number number(){ws();int start=p;boolean flt=false;if(p<s.length()&&s.charAt(p)=='-')p++;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;if(p<s.length()&&s.charAt(p)=='.'){flt=true;p++;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;}if(p<s.length()&&(s.charAt(p)=='e'||s.charAt(p)=='E')){flt=true;p++;if(p<s.length()&&(s.charAt(p)=='+'||s.charAt(p)=='-'))p++;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;}String t=s.substring(start,p);if(flt)return Double.parseDouble(t);long v=Long.parseLong(t);if(v>=Integer.MIN_VALUE&&v<=Integer.MAX_VALUE)return(int)v;return v;}
}

class Net{
    static String fetchString(String url) throws IOException{
        HttpURLConnection c=(HttpURLConnection)URI.create(url).toURL().openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","CTLauncher/0.2");
        try(InputStream in=c.getInputStream()){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}finally{c.disconnect();}
    }
    static boolean download(String url,File dest,long sz,Net.PC cb) throws IOException{
        if(dest.exists()&&(sz<=0||dest.length()==sz))return false;
        dest.getParentFile().mkdirs();File tmp=new File(dest.getPath()+".tmp");
        HttpURLConnection c=(HttpURLConnection)URI.create(url).toURL().openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","CTLauncher/0.2");
        long total=c.getContentLengthLong();if(total<=0)total=sz;
        try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(tmp)){
            byte[] buf=new byte[8192];long done=0;int n;
            while((n=in.read(buf))!=-1){out.write(buf,0,n);done+=n;if(cb!=null)cb.progress(done,total);}
        }finally{c.disconnect();}
        Files.move(tmp.toPath(),dest.toPath(),StandardCopyOption.REPLACE_EXISTING);return true;
    }
    interface PC{void progress(long done,long total);}
}

class McBg{
    private static BufferedImage cached;private static int cw,ch;
    static BufferedImage get(int w,int h){
        if(cached!=null&&cw==w&&ch==h)return cached;cw=w;ch=h;
        cached=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=cached.createGraphics();
        g.setPaint(new GradientPaint(0,0,new Color(100,170,60),0,h*.6f,new Color(120,180,80)));g.fillRect(0,0,w,h);
        Random rng=new Random(42);int bs=Math.max(6,w/160),cols=w/bs+2,gy=(int)(h*.45);
        double[] ht=new double[cols];for(int i=0;i<cols;i++)ht[i]=gy+Math.sin(i*.04)*30+Math.sin(i*.11+2)*15+Math.sin(i*.02-1)*45;
        Color gT=new Color(95,160,55),gD=new Color(70,130,40),d=new Color(120,85,55),dD=new Color(95,68,42),s=new Color(80,80,85),sD=new Color(60,60,65);
        for(int c2=0;c2<cols;c2++){int bx=c2*bs,ty=(int)ht[c2];
            for(int r=0;r<2;r++){g.setColor((c2+r)%3==0?gD:gT);g.fillRect(bx,ty+r*bs,bs,bs);}
            for(int r=2;r<6;r++){g.setColor((c2+r)%4==0?dD:d);g.fillRect(bx,ty+r*bs,bs,bs);}
            for(int r=6;ty+r*bs<h+bs;r++){g.setColor((c2+r)%5==0?sD:s);g.fillRect(bx,ty+r*bs,bs,bs);}
        }
        Color tk=new Color(85,60,35),lf=new Color(40,110,30),lH=new Color(50,130,40);
        for(int tx:new int[]{(int)(w*.08),(int)(w*.22),(int)(w*.42),(int)(w*.62),(int)(w*.78),(int)(w*.92)}){
            int ci=Math.min(cols-1,Math.max(0,tx/bs)),tb=(int)ht[ci],tw=bs*2,th=bs*7;
            g.setColor(tk);g.fillRect(tx,tb-th,tw,th);
            int cW=bs*7,cH=bs*5,cx=tx+tw/2-cW/2,cy=tb-th-cH+bs;
            for(int br=0;br<cH/bs;br++)for(int bc=0;bc<cW/bs;bc++){
                double dx2=(bc-cW/bs/2.0)/(cW/bs/2.0),dy2=(br-cH/bs/2.0)/(cH/bs/2.0);
                if(dx2*dx2+dy2*dy2<.85+rng.nextDouble()*.3){g.setColor((bc+br)%3==0?lH:lf);g.fillRect(cx+bc*bs,cy+br*bs,bs,bs);}
            }
        }
        g.setColor(new Color(0,0,0,100));g.fillRect(0,(int)(h*.4),w,h);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w/2f,h/2f),Math.max(w,h)*.7f,new float[]{0,.6f,1},new Color[]{new Color(0,0,0,0),new Color(0,0,0,30),new Color(0,0,0,120)}));
        g.fillRect(0,0,w,h);g.dispose();return cached;
    }
}

// ═══════════════════════════════════════════════════════════
//  MAIN FRAME — Matching mockup layout
// ═══════════════════════════════════════════════════════════
class CTLauncherFrame extends JFrame{
    final DefaultListModel<String> verModel=new DefaultListModel<>();
    final JLabel statusLabel;
    String username="Player";
    int ramGB=4;
    private JPanel bottomPanel;private CardLayout bottomCL;private String curTab="none";
    private final File modsDir;

    CTLauncherFrame(){
        setTitle("CTLauncher 0.2");setSize(960,640);setMinimumSize(new Dimension(800,520));
        setDefaultCloseOperation(EXIT_ON_CLOSE);setUndecorated(true);setLocationRelativeTo(null);
        modsDir=new File(CTlauncher0_1_1.gameDir()+File.separator+"mods");modsDir.mkdirs();

        JPanel root=new JPanel(new BorderLayout()){
            protected void paintComponent(Graphics g){super.paintComponent(g);g.drawImage(McBg.get(getWidth(),getHeight()),0,0,null);}
        };root.setBackground(CTlauncher0_1_1.BG_DARK);setContentPane(root);

        // Title bar
        JPanel titleBar=new JPanel(new BorderLayout());titleBar.setBackground(new Color(0,0,0,180));titleBar.setPreferredSize(new Dimension(0,36));
        // Deco edge
        JPanel deco=new JPanel(){protected void paintComponent(Graphics g){g.setColor(new Color(100,100,100));for(int x=0;x<getWidth();x+=18)g.fillRect(x+3,1,12,4);}};
        deco.setOpaque(false);deco.setPreferredSize(new Dimension(0,7));titleBar.add(deco,BorderLayout.NORTH);
        JPanel tRight=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));tRight.setOpaque(false);
        tRight.add(ctrlBtn("\u2014",CTlauncher0_1_1.TEXT_DIM,e->setState(ICONIFIED)));
        tRight.add(ctrlBtn("\u2715",new Color(230,70,70),e->System.exit(0)));
        titleBar.add(tRight,BorderLayout.EAST);
        root.add(titleBar,BorderLayout.NORTH);

        // Center
        JPanel center=new JPanel();center.setOpaque(false);center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));

        // Logo
        JPanel logoRow=new JPanel(new FlowLayout(FlowLayout.CENTER));logoRow.setOpaque(false);logoRow.setBorder(new EmptyBorder(14,0,6,0));
        logoRow.add(new JLabel(){
            protected void paintComponent(Graphics g1){
                Graphics2D g=(Graphics2D)g1;g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(new Font("SansSerif",Font.BOLD,50));g.setColor(new Color(0,0,0,80));g.drawString("CT",3,48);
                g.setColor(CTlauncher0_1_1.ACCENT_CYAN);g.drawString("CT",0,46);
                g.setFont(new Font("SansSerif",Font.PLAIN,40));g.setColor(new Color(0,0,0,60));g.drawString("Launcher",80,48);
                g.setColor(CTlauncher0_1_1.TEXT_WHITE);g.drawString("Launcher",78,46);
            }
            public Dimension getPreferredSize(){return new Dimension(370,56);}
        });
        center.add(logoRow);

        // PLAY
        JPanel playRow=new JPanel(new FlowLayout(FlowLayout.CENTER));playRow.setOpaque(false);playRow.setBorder(new EmptyBorder(2,0,10,0));
        JButton playBtn=new JButton("PLAY"){
            boolean hv,pr;{setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);
                setFont(new Font("SansSerif",Font.BOLD,26));setForeground(Color.WHITE);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(320,52));
                addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){hv=true;repaint();}public void mouseExited(MouseEvent e){hv=false;pr=false;repaint();}
                    public void mousePressed(MouseEvent e){pr=true;repaint();}public void mouseReleased(MouseEvent e){pr=false;repaint();}});}
            protected void paintComponent(Graphics g1){Graphics2D g=(Graphics2D)g1.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg=pr?CTlauncher0_1_1.PLAY_PRESS:hv?CTlauncher0_1_1.PLAY_HOVER:CTlauncher0_1_1.PLAY_GREEN;
                g.setColor(new Color(0,0,0,60));g.fillRoundRect(3,4,getWidth()-6,getHeight()-4,10,10);
                g.setPaint(new GradientPaint(0,0,bg.brighter(),0,getHeight(),bg.darker()));g.fillRoundRect(0,0,getWidth()-3,getHeight()-4,8,8);
                g.setColor(new Color(255,255,255,30));g.drawRoundRect(0,0,getWidth()-4,getHeight()-5,8,8);
                g.setFont(getFont());FontMetrics fm=g.getFontMetrics();int tx=(getWidth()-fm.stringWidth("PLAY"))/2,ty=(getHeight()+fm.getAscent()-fm.getDescent())/2-2;
                g.setColor(new Color(0,60,0,120));g.drawString("PLAY",tx+1,ty+1);g.setColor(Color.WHITE);g.drawString("PLAY",tx,ty);g.dispose();}
        };
        playBtn.addActionListener(e->launchGame());
        playRow.add(playBtn);center.add(playRow);

        // 3-column panel
        JPanel cols=new JPanel(new GridBagLayout());cols.setOpaque(false);cols.setBorder(new EmptyBorder(0,28,0,28));
        GridBagConstraints gc=new GridBagConstraints();gc.gridy=0;gc.fill=GridBagConstraints.BOTH;gc.insets=new Insets(0,3,0,3);
        gc.gridx=0;gc.weightx=.25;gc.weighty=1;cols.add(buildVersionPanel(),gc);
        gc.gridx=1;gc.weightx=.28;cols.add(buildProfilePanel(),gc);
        gc.gridx=2;gc.weightx=.47;cols.add(buildNewsPanel(),gc);
        JPanel cw=new JPanel(new BorderLayout());cw.setOpaque(false);cw.setMaximumSize(new Dimension(9999,250));cw.setPreferredSize(new Dimension(900,230));
        cw.add(cols);center.add(cw);

        // Bottom tab bar
        JPanel tabBar=new JPanel(new FlowLayout(FlowLayout.CENTER,18,2));tabBar.setOpaque(false);tabBar.setBorder(new EmptyBorder(6,0,2,0));
        tabBar.add(iconTab("\u2699","Settings",()->toggleBot("settings")));
        tabBar.add(iconTab("\u2692","Mods",()->toggleBot("mods")));
        tabBar.add(iconTab("\u265E","Skins",()->toggleBot("skins")));
        tabBar.add(iconTab("\u2630","Servers",()->toggleBot("servers")));
        tabBar.add(iconTab("\uD83D\uDCC1","Folder",()->openDir()));
        center.add(tabBar);

        // Swappable bottom
        bottomCL=new CardLayout();bottomPanel=new JPanel(bottomCL);bottomPanel.setOpaque(false);
        bottomPanel.setMaximumSize(new Dimension(9999,0));
        JPanel ep=new JPanel();ep.setOpaque(false);bottomPanel.add(ep,"none");
        bottomPanel.add(buildSettingsCard(),"settings");bottomPanel.add(buildModsCard(),"mods");
        bottomPanel.add(cLabel("Skins \u2014 Coming soon"),"skins");bottomPanel.add(cLabel("Servers \u2014 Coming soon"),"servers");
        center.add(bottomPanel);

        // Status
        statusLabel=new JLabel("Ready to play");statusLabel.setFont(new Font("SansSerif",Font.BOLD,15));
        statusLabel.setForeground(CTlauncher0_1_1.PLAY_GREEN);statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel sRow=new JPanel(new FlowLayout(FlowLayout.CENTER));sRow.setOpaque(false);sRow.setBorder(new EmptyBorder(4,0,8,0));sRow.add(statusLabel);
        center.add(sRow);

        root.add(center,BorderLayout.CENTER);

        // Drag
        final int[] dr=new int[2];
        root.addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){dr[0]=e.getX();dr[1]=e.getY();}});
        root.addMouseMotionListener(new MouseAdapter(){public void mouseDragged(MouseEvent e){setLocation(e.getXOnScreen()-dr[0],e.getYOnScreen()-dr[1]);}});

        loadVersions();
    }

    // ── Version panel ──
    JPanel buildVersionPanel(){
        JPanel p=dCard();p.setLayout(new BorderLayout());
        JLabel h=new JLabel("  Version \u25BC");h.setFont(new Font("SansSerif",Font.BOLD,13));h.setForeground(CTlauncher0_1_1.TEXT_WHITE);
        h.setBorder(new EmptyBorder(8,8,4,8));p.add(h,BorderLayout.NORTH);
        verModel.addElement("Loading...");
        JList<String> list=new JList<>(verModel);list.setBackground(CTlauncher0_1_1.BG_PANEL);list.setForeground(CTlauncher0_1_1.TEXT_WHITE);
        list.setSelectionBackground(CTlauncher0_1_1.PLAY_GREEN);list.setFont(new Font("SansSerif",Font.PLAIN,13));list.setFixedCellHeight(26);
        list.setCellRenderer(new DefaultListCellRenderer(){
            public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean sel,boolean f){
                JLabel c=(JLabel)super.getListCellRendererComponent(l,v,i,sel,f);c.setBorder(new EmptyBorder(2,10,2,6));
                c.setBackground(sel?CTlauncher0_1_1.PLAY_GREEN:CTlauncher0_1_1.BG_PANEL);
                String t=v.toString();if(t.contains("forge")||t.contains("Forge"))c.setForeground(sel?Color.WHITE:new Color(220,160,50));
                else c.setForeground(sel?Color.WHITE:CTlauncher0_1_1.TEXT_WHITE);return c;}});
        JScrollPane sp=new JScrollPane(list);sp.setBorder(new EmptyBorder(0,4,4,4));sp.getViewport().setBackground(CTlauncher0_1_1.BG_PANEL);
        p.add(sp,BorderLayout.CENTER);return p;
    }

    // ── Profile panel ──
    JPanel buildProfilePanel(){
        JPanel p=dCard();p.setLayout(new GridBagLayout());GridBagConstraints g=new GridBagConstraints();
        g.gridx=0;g.anchor=GridBagConstraints.CENTER;
        g.gridy=0;g.insets=new Insets(8,0,4,0);JLabel h=new JLabel("Profile");h.setFont(new Font("SansSerif",Font.BOLD,14));h.setForeground(CTlauncher0_1_1.TEXT_WHITE);p.add(h,g);
        g.gridy=1;g.insets=new Insets(2,0,2,0);
        // Steve skin
        JPanel skin=new JPanel(){protected void paintComponent(Graphics g1){Graphics2D g2=(Graphics2D)g1;int cx=getWidth()/2,cy=4,s=5;
            g2.setColor(new Color(140,100,60));g2.fillRect(cx-4*s,cy,8*s,8*s);g2.setColor(new Color(200,170,130));g2.fillRect(cx-3*s,cy+2*s,6*s,5*s);
            g2.setColor(new Color(80,50,30));g2.fillRect(cx-2*s,cy+3*s,s,s);g2.fillRect(cx+s,cy+3*s,s,s);
            int by=cy+8*s+2;g2.setColor(new Color(0,170,170));g2.fillRect(cx-2*s,by,4*s,5*s);g2.fillRect(cx-4*s,by,2*s,5*s);g2.fillRect(cx+2*s,by,2*s,5*s);
            int ly=by+5*s;g2.setColor(new Color(50,50,150));g2.fillRect(cx-2*s,ly,2*s,5*s);g2.fillRect(cx,ly,2*s,5*s);
            g2.setColor(new Color(60,60,60));g2.fillRect(cx-2*s,ly+4*s,2*s,s);g2.fillRect(cx,ly+4*s,2*s,s);}
            public Dimension getPreferredSize(){return new Dimension(70,115);}};
        skin.setOpaque(false);p.add(skin,g);
        g.gridy=2;JLabel nm=new JLabel("Steve");nm.setFont(new Font("SansSerif",Font.BOLD,13));nm.setForeground(CTlauncher0_1_1.TEXT_WHITE);p.add(nm,g);
        g.gridy=3;JLabel fl=new JLabel("\u2699Forge");fl.setFont(new Font("SansSerif",Font.PLAIN,11));fl.setForeground(new Color(220,160,50));p.add(fl,g);
        return p;
    }

    // ── News panel ──
    JPanel buildNewsPanel(){
        JPanel p=dCard();p.setLayout(new BorderLayout());
        JPanel hr=new JPanel(new BorderLayout());hr.setOpaque(false);hr.setBorder(new EmptyBorder(8,10,4,8));
        JLabel h=new JLabel("News Feed");h.setFont(new Font("SansSerif",Font.BOLD,13));h.setForeground(CTlauncher0_1_1.TEXT_WHITE);hr.add(h,BorderLayout.WEST);
        JLabel x=new JLabel("X ");x.setForeground(CTlauncher0_1_1.TEXT_DIM);x.setFont(new Font("SansSerif",Font.BOLD,13));hr.add(x,BorderLayout.EAST);
        p.add(hr,BorderLayout.NORTH);
        JPanel nl=new JPanel();nl.setLayout(new BoxLayout(nl,BoxLayout.Y_AXIS));nl.setBackground(CTlauncher0_1_1.BG_PANEL);
        nl.add(newsItem("Minecraft 1.21.4 Update","Minecraft 1.21 \u2014 Mods Available\nCandRings",true));
        nl.add(newsItem("New Mods Available","Minecraft 1.21 \u2014 Mods Available\nSupport Forge",false));
        nl.add(newsItem("Minecraft","Minecraft 1.21 \u2014 Mods Available",false));
        JScrollPane sp=new JScrollPane(nl);sp.setBorder(new EmptyBorder(0,4,4,4));sp.getViewport().setBackground(CTlauncher0_1_1.BG_PANEL);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);p.add(sp,BorderLayout.CENTER);return p;
    }
    JPanel newsItem(String t,String d,boolean tag){
        JPanel i=new JPanel(new BorderLayout(8,0));i.setBackground(CTlauncher0_1_1.NEWS_BG);i.setBorder(new EmptyBorder(6,8,6,8));i.setMaximumSize(new Dimension(9999,64));
        JPanel th=new JPanel(){protected void paintComponent(Graphics g){g.setColor(new Color(60,100,50));g.fillRect(0,0,getWidth(),getHeight());g.setColor(new Color(80,130,60));g.fillRect(2,getHeight()/2,getWidth()-4,getHeight()/2);}};
        th.setPreferredSize(new Dimension(44,44));i.add(th,BorderLayout.WEST);
        JPanel tx=new JPanel();tx.setOpaque(false);tx.setLayout(new BoxLayout(tx,BoxLayout.Y_AXIS));
        JLabel tl=new JLabel(t);tl.setFont(new Font("SansSerif",Font.BOLD,11));tl.setForeground(CTlauncher0_1_1.TEXT_WHITE);tx.add(tl);
        JLabel dl=new JLabel("<html>"+d.replace("\n","<br>")+"</html>");dl.setFont(new Font("SansSerif",Font.PLAIN,9));dl.setForeground(CTlauncher0_1_1.TEXT_DIM);tx.add(dl);
        i.add(tx,BorderLayout.CENTER);
        if(tag){JLabel tg=new JLabel("\u25A0");tg.setFont(new Font("SansSerif",Font.PLAIN,16));tg.setForeground(CTlauncher0_1_1.PLAY_GREEN);i.add(tg,BorderLayout.EAST);}
        return i;
    }

    // ── Settings card ──
    JPanel buildSettingsCard(){
        JPanel c=new JPanel(new GridBagLayout());c.setBackground(new Color(24,24,28,220));
        GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(3,14,3,14);g.fill=GridBagConstraints.HORIZONTAL;g.gridx=0;g.gridwidth=2;
        g.gridy=0;c.add(lbl("Settings",16,CTlauncher0_1_1.TEXT_WHITE),g);
        g.gridy=1;c.add(lbl("USERNAME",10,CTlauncher0_1_1.TEXT_DIM),g);
        g.gridy=2;JTextField nf=sf("Player");nf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){username=nf.getText();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){username=nf.getText();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}});c.add(nf,g);
        g.gridy=3;c.add(lbl("RAM",10,CTlauncher0_1_1.TEXT_DIM),g);
        g.gridy=4;JSlider sl=new JSlider(1,16,4);sl.setOpaque(false);sl.setForeground(CTlauncher0_1_1.PLAY_GREEN);
        JLabel rl=new JLabel("4 GB");rl.setForeground(CTlauncher0_1_1.TEXT_WHITE);rl.setFont(new Font("SansSerif",Font.PLAIN,12));
        sl.addChangeListener(ev->{rl.setText(sl.getValue()+" GB");ramGB=sl.getValue();});
        JPanel sr=new JPanel(new BorderLayout(8,0));sr.setOpaque(false);sr.add(sl,BorderLayout.CENTER);sr.add(rl,BorderLayout.EAST);c.add(sr,g);
        g.gridy=5;c.add(lbl("Java: "+System.getProperty("java.version")+" ("+System.getProperty("os.arch")+") \u2022 Auto-download",11,CTlauncher0_1_1.TEXT_DIM),g);
        return c;
    }

    // ── Mods card ──
    JPanel buildModsCard(){
        JPanel c=new JPanel(new BorderLayout());c.setBackground(new Color(24,24,28,220));
        JPanel hdr=new JPanel(new BorderLayout());hdr.setOpaque(false);hdr.setBorder(new EmptyBorder(6,14,2,14));
        hdr.add(lbl("Mod Manager",15,CTlauncher0_1_1.TEXT_WHITE),BorderLayout.WEST);
        JPanel hb=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));hb.setOpaque(false);
        hb.add(aBtn("Install Forge",new Color(220,160,50),e->installForge()));
        hb.add(aBtn("Add Mod",CTlauncher0_1_1.PLAY_GREEN,e->addMod()));
        hb.add(aBtn("Open Folder",CTlauncher0_1_1.ACCENT_BLUE,e->openDir()));
        hdr.add(hb,BorderLayout.EAST);c.add(hdr,BorderLayout.NORTH);
        DefaultListModel<String> mm=new DefaultListModel<>();refreshMods(mm);
        JList<String> ml=new JList<>(mm);ml.setBackground(new Color(20,20,24));ml.setForeground(CTlauncher0_1_1.TEXT_WHITE);
        ml.setFont(new Font("SansSerif",Font.PLAIN,12));ml.setFixedCellHeight(28);ml.setSelectionBackground(new Color(50,50,60));
        ml.setCellRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
            JLabel cl=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);cl.setBorder(new EmptyBorder(3,10,3,10));
            cl.setBackground(s?new Color(50,50,60):new Color(20,20,24));cl.setForeground(v.toString().contains("DISABLED")?CTlauncher0_1_1.TEXT_DIM:CTlauncher0_1_1.TEXT_WHITE);return cl;}});
        JScrollPane sp=new JScrollPane(ml);sp.setBorder(new EmptyBorder(2,10,2,10));sp.getViewport().setBackground(new Color(20,20,24));
        sp.setPreferredSize(new Dimension(0,90));c.add(sp,BorderLayout.CENTER);
        JPanel bot=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));bot.setOpaque(false);bot.setBorder(new EmptyBorder(0,10,4,10));
        bot.add(aBtn("Toggle",new Color(220,180,40),e->{String sv=ml.getSelectedValue();if(sv==null)return;
            String cl2=sv.replace("  [DISABLED]","").replace("  [ENABLED]","").trim();File[] fs=modsDir.listFiles();
            if(fs!=null)for(File fi:fs)if(fi.getName().replace(".disabled","").equals(cl2)||fi.getName().equals(cl2)){
                fi.renameTo(new File(modsDir,fi.getName().endsWith(".disabled")?fi.getName().replace(".jar.disabled",".jar"):fi.getName()+".disabled"));break;}
            refreshMods(mm);}));
        bot.add(aBtn("Delete",new Color(220,60,60),e->{String sv=ml.getSelectedValue();if(sv==null)return;
            if(JOptionPane.showConfirmDialog(this,"Delete?","Confirm",JOptionPane.YES_NO_OPTION)==0){
                String cl2=sv.replace("  [DISABLED]","").replace("  [ENABLED]","").trim();File[] fs=modsDir.listFiles();
                if(fs!=null)for(File fi:fs)if(fi.getName().replace(".disabled","").equals(cl2)||fi.getName().equals(cl2)){fi.delete();break;}
                refreshMods(mm);}}));
        bot.add(aBtn("Refresh",CTlauncher0_1_1.TEXT_DIM,e->refreshMods(mm)));c.add(bot,BorderLayout.SOUTH);return c;
    }

    void refreshMods(DefaultListModel<String> m){m.clear();File[] fs=modsDir.listFiles();if(fs==null)return;
        java.util.Arrays.sort(fs,(a,b)->a.getName().compareToIgnoreCase(b.getName()));
        for(File f:fs){String n=f.getName().toLowerCase();if(f.isFile()&&(n.endsWith(".jar")||n.endsWith(".jar.disabled")))
            m.addElement(f.getName().replace(".disabled","")+(n.endsWith(".disabled")?"  [DISABLED]":"  [ENABLED]"));}
        if(m.isEmpty())m.addElement("No mods \u2014 click Add Mod or Install Forge");}
    void addMod(){JFileChooser fc=new JFileChooser();fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Mod JARs","jar"));
        if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)for(File s:fc.getSelectedFiles())
            try{Files.copy(s.toPath(),new File(modsDir,s.getName()).toPath(),StandardCopyOption.REPLACE_EXISTING);}catch(IOException ex){}}

    // ── Forge installer ──
    void installForge(){
        String mcVer=(String)JOptionPane.showInputDialog(this,"Install Forge for which MC version?","Install Forge",JOptionPane.QUESTION_MESSAGE,null,null,"1.21.4");
        if(mcVer==null||mcVer.isBlank())return;final String fv=mcVer.trim();
        statusLabel.setText("Installing Forge for "+fv+"...");statusLabel.setForeground(new Color(220,160,50));
        new Thread(()->{try{
            String pj=Net.fetchString(CTlauncher0_1_1.FORGE_PROMOS);Map<String,Object> pr=Json.sub(Json.obj(Json.parse(pj)),"promos");
            if(pr==null)throw new RuntimeException("No Forge data");
            String fVer=Json.str(pr,fv+"-recommended");if(fVer==null)fVer=Json.str(pr,fv+"-latest");
            if(fVer==null)throw new RuntimeException("No Forge for MC "+fv);
            String full=fv+"-"+fVer;System.out.println("[Forge] Installing "+full);
            String url="https://maven.minecraftforge.net/net/minecraftforge/forge/"+full+"/forge-"+full+"-installer.jar";
            File jar=new File(CTlauncher0_1_1.gameDir(),"forge-installer-"+full+".jar");
            SwingUtilities.invokeLater(()->statusLabel.setText("Downloading Forge "+full+"..."));
            Net.download(url,jar,-1,null);
            SwingUtilities.invokeLater(()->statusLabel.setText("Running Forge installer..."));
            String jp=System.getProperty("java.home")+File.separator+"bin"+File.separator+"java";
            Process p=new ProcessBuilder(jp,"-jar",jar.getAbsolutePath(),"--installClient",CTlauncher0_1_1.gameDir()).redirectErrorStream(true).start();
            BufferedReader rd=new BufferedReader(new InputStreamReader(p.getInputStream()));String ln;while((ln=rd.readLine())!=null)System.out.println("[Forge] "+ln);
            int ex=p.waitFor();jar.delete();
            if(ex==0){SwingUtilities.invokeLater(()->{statusLabel.setText("Forge installed!");statusLabel.setForeground(CTlauncher0_1_1.PLAY_GREEN);loadVersions();});}
            else throw new RuntimeException("Installer exit code "+ex);
        }catch(Exception ex){ex.printStackTrace();SwingUtilities.invokeLater(()->{statusLabel.setText("Forge failed: "+ex.getMessage());statusLabel.setForeground(new Color(220,60,60));
            JOptionPane.showMessageDialog(this,"Forge install failed:\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);});}
        },"CTL-Forge").start();
    }

    // ── Launch ──
    void launchGame(){
        String ver=null;for(int i=0;i<verModel.size();i++){String v=verModel.get(i);
            if(!v.startsWith("\u2500")&&!v.startsWith("Load")&&!v.startsWith("Fail")&&!v.startsWith("No ")){ver=v;break;}}
        if(ver==null)ver="1.21.4";String u=username.isBlank()?"Player":username;
        statusLabel.setText("Launching "+ver+"...");statusLabel.setForeground(new Color(220,200,80));
        GameLauncher.launchAsync(u,ver,ramGB,this);
        final String fv2=ver;new Timer(5000,ev->{statusLabel.setText("Ready to play");statusLabel.setForeground(CTlauncher0_1_1.PLAY_GREEN);((Timer)ev.getSource()).stop();}).start();
    }

    // ── Version loading ──
    void loadVersions(){new Thread(()->{try{
        List<String> forge=new ArrayList<>();File vd=new File(CTlauncher0_1_1.gameDir()+File.separator+"versions");
        if(vd.isDirectory()){File[] ds=vd.listFiles(File::isDirectory);if(ds!=null)for(File d:ds){String n=d.getName();
            if((n.contains("forge")||n.contains("Forge")||n.contains("fabric"))&&new File(d,n+".json").exists())forge.add(n);}}
        String j=Net.fetchString(CTlauncher0_1_1.MANIFEST_URL);List<Object> vs=Json.subarr(Json.obj(Json.parse(j)),"versions");
        List<String> rel=new ArrayList<>();for(Object v:vs){Map<String,Object> vm=Json.obj(v);if("release".equals(Json.str(vm,"type")))rel.add(Json.str(vm,"id"));}
        SwingUtilities.invokeLater(()->{verModel.clear();for(String f:forge)verModel.addElement(f);for(String r:rel)verModel.addElement(r);});
    }catch(Exception ex){SwingUtilities.invokeLater(()->{verModel.clear();verModel.addElement("1.21.4");verModel.addElement("1.20.4");verModel.addElement("Error: "+ex.getMessage());});}
    },"CTL-Ver").start();}

    // ── UI Helpers ──
    void toggleBot(String n){if(curTab.equals(n)){bottomPanel.setMaximumSize(new Dimension(9999,0));bottomPanel.setPreferredSize(new Dimension(0,0));bottomCL.show(bottomPanel,"none");curTab="none";}
        else{bottomPanel.setMaximumSize(new Dimension(9999,190));bottomPanel.setPreferredSize(new Dimension(0,180));bottomCL.show(bottomPanel,n);curTab=n;}
        bottomPanel.revalidate();bottomPanel.repaint();getContentPane().revalidate();}
    void openDir(){try{Desktop.getDesktop().open(new File(CTlauncher0_1_1.gameDir()));}catch(Exception ex){try{Runtime.getRuntime().exec(new String[]{"open",CTlauncher0_1_1.gameDir()});}catch(Exception ignored){}}}
    JPanel dCard(){JPanel p=new JPanel(){protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(28,28,34,220));g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);g2.setColor(CTlauncher0_1_1.BORDER_DIM);g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);g2.dispose();}};
        p.setOpaque(false);return p;}
    JPanel iconTab(String ic,String lb,Runnable act){JPanel t=new JPanel();t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));t.setOpaque(false);t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel il=new JLabel(ic);il.setFont(new Font("SansSerif",Font.PLAIN,20));il.setForeground(CTlauncher0_1_1.TEXT_DIM);il.setAlignmentX(CENTER_ALIGNMENT);t.add(il);
        JLabel ll=new JLabel(lb);ll.setFont(new Font("SansSerif",Font.PLAIN,9));ll.setForeground(CTlauncher0_1_1.TEXT_DIM);ll.setAlignmentX(CENTER_ALIGNMENT);t.add(ll);
        t.setPreferredSize(new Dimension(60,40));t.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){act.run();}public void mouseEntered(MouseEvent e){il.setForeground(CTlauncher0_1_1.TEXT_WHITE);ll.setForeground(CTlauncher0_1_1.TEXT_WHITE);}
            public void mouseExited(MouseEvent e){il.setForeground(CTlauncher0_1_1.TEXT_DIM);ll.setForeground(CTlauncher0_1_1.TEXT_DIM);}});return t;}
    JButton ctrlBtn(String t,Color fg,ActionListener al){JButton b=new JButton(t);b.setFont(new Font("SansSerif",Font.PLAIN,14));b.setForeground(fg);
        b.setFocusPainted(false);b.setBorderPainted(false);b.setContentAreaFilled(false);b.setPreferredSize(new Dimension(40,30));b.addActionListener(al);
        b.addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){b.setBackground(new Color(60,60,60));b.setOpaque(true);b.repaint();}
            public void mouseExited(MouseEvent e){b.setOpaque(false);b.repaint();}});return b;}
    JButton aBtn(String t,Color fg,ActionListener al){JButton b=new JButton(t);b.setFont(new Font("SansSerif",Font.BOLD,10));b.setForeground(fg);
        b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(al);
        b.addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){b.setBackground(new Color(50,50,55));b.setOpaque(true);b.repaint();}
            public void mouseExited(MouseEvent e){b.setOpaque(false);b.repaint();}});return b;}
    static JTextField sf(String d){JTextField f=new JTextField(d,14);f.setBackground(CTlauncher0_1_1.BG_INPUT);f.setForeground(CTlauncher0_1_1.TEXT_WHITE);
        f.setCaretColor(CTlauncher0_1_1.TEXT_WHITE);f.setFont(new Font("SansSerif",Font.PLAIN,13));
        f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(CTlauncher0_1_1.INPUT_BORDER,1),BorderFactory.createEmptyBorder(5,8,5,8)));return f;}
    static JLabel lbl(String t,int sz,Color c){JLabel l=new JLabel(t);l.setFont(new Font("SansSerif",sz>12?Font.BOLD:Font.PLAIN,sz));l.setForeground(c);return l;}
    JPanel cLabel(String t){JPanel p=new JPanel(new GridBagLayout());p.setBackground(new Color(24,24,28,200));JLabel l=new JLabel(t);l.setForeground(CTlauncher0_1_1.TEXT_DIM);l.setFont(new Font("SansSerif",Font.PLAIN,13));p.add(l);return p;}
}

// ═══════════════════════════════════════════════════════════
//  PROGRESS DIALOG
// ═══════════════════════════════════════════════════════════
class ProgressDialog extends JDialog{
    private final JLabel statusLabel;private final JProgressBar bar;private final JLabel fileLabel;
    ProgressDialog(JFrame parent){super(parent,"Preparing...",false);setSize(460,150);setLocationRelativeTo(parent);setResizable(false);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);setUndecorated(true);
        JPanel p=new JPanel(){protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CTlauncher0_1_1.OVERLAY_BG);g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);g2.setColor(CTlauncher0_1_1.BORDER_DIM);g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);g2.dispose();}};
        p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(new EmptyBorder(16,20,16,20));
        statusLabel=new JLabel("Starting...");statusLabel.setForeground(CTlauncher0_1_1.PLAY_GREEN);statusLabel.setFont(new Font("SansSerif",Font.BOLD,13));statusLabel.setAlignmentX(LEFT_ALIGNMENT);p.add(statusLabel);p.add(Box.createVerticalStrut(10));
        bar=new JProgressBar(0,100);bar.setStringPainted(true);bar.setBackground(CTlauncher0_1_1.BG_INPUT);bar.setForeground(CTlauncher0_1_1.PLAY_GREEN);bar.setAlignmentX(LEFT_ALIGNMENT);bar.setMaximumSize(new Dimension(Integer.MAX_VALUE,22));p.add(bar);p.add(Box.createVerticalStrut(6));
        fileLabel=new JLabel(" ");fileLabel.setForeground(CTlauncher0_1_1.TEXT_DIM);fileLabel.setFont(new Font("SansSerif",Font.PLAIN,11));fileLabel.setAlignmentX(LEFT_ALIGNMENT);p.add(fileLabel);
        setContentPane(p);setBackground(new Color(0,0,0,0));}
    void status(String m){SwingUtilities.invokeLater(()->{statusLabel.setText(m);bar.setIndeterminate(true);});}
    void fileProgress(String n,long d,long t){SwingUtilities.invokeLater(()->{bar.setIndeterminate(false);if(t>0){int pc=(int)(d*100/t);bar.setValue(pc);bar.setString(pc+"%");}fileLabel.setText(n+"  ("+d/1024+" / "+t/1024+" KB)");});}
}

// ═══════════════════════════════════════════════════════════
//  GAME LAUNCHER ENGINE (all fixes preserved)
// ═══════════════════════════════════════════════════════════
class GameLauncher{
    static String osName(){String o=System.getProperty("os.name").toLowerCase();if(o.contains("win"))return"windows";if(o.contains("mac")||o.contains("osx"))return"osx";return"linux";}
    static boolean osMatches(String n){if(n==null)return false;String c=osName();if(c.equals(n))return true;if("osx".equals(c)&&"macos".equals(n))return true;if("macos".equals(c)&&"osx".equals(n))return true;return false;}
    static void launchAsync(String username,String versionId,int ramGB,JFrame parent){
        new Thread(()->{ProgressDialog dlg=new ProgressDialog(parent);try{
            SwingUtilities.invokeLater(()->dlg.setVisible(true));
            String base=CTlauncher0_1_1.gameDir(),verDir=base+File.separator+"versions"+File.separator+versionId;
            String libDir=base+File.separator+"libraries",assDir=base+File.separator+"assets",natDir=verDir+File.separator+"natives";
            new File(verDir).mkdirs();new File(natDir).mkdirs();new File(libDir).mkdirs();new File(assDir).mkdirs();new File(base+File.separator+"mods").mkdirs();
            dlg.status("Fetching version manifest...");
            String mj=Net.fetchString(CTlauncher0_1_1.MANIFEST_URL);List<Object> versions=Json.subarr(Json.obj(Json.parse(mj)),"versions");
            String vUrl=null;for(Object v:versions){Map<String,Object> vm=Json.obj(v);if(versionId.equals(Json.str(vm,"id"))){vUrl=Json.str(vm,"url");break;}}
            // Try local version JSON for Forge
            if(vUrl==null){File lj=new File(verDir,versionId+".json");
                if(lj.exists()){Map<String,Object> lv=Json.obj(Json.parse(Files.readString(lj.toPath())));String inh=Json.str(lv,"inheritsFrom");
                    if(inh!=null)for(Object v:versions){Map<String,Object> vm=Json.obj(v);if(inh.equals(Json.str(vm,"id"))){vUrl=Json.str(vm,"url");break;}}}
                if(vUrl==null&&!new File(verDir,versionId+".json").exists())throw new RuntimeException("Version "+versionId+" not found");}
            dlg.status("Fetching "+versionId+" metadata...");
            Map<String,Object> ver;if(vUrl!=null){String vj=Net.fetchString(vUrl);
                // Patch version type so MC title screen shows "CTLauncher" in bottom-left
                vj=vj.replaceFirst("\"type\"\\s*:\\s*\"[^\"]+\"","\"type\":\"CTLauncher\"");
                ver=Json.obj(Json.parse(vj));Files.writeString(Path.of(verDir,versionId+".json"),vj);}
            else{String cached=Files.readString(Path.of(verDir,versionId+".json"));
                if(!cached.contains("\"type\":\"CTLauncher\"")){cached=cached.replaceFirst("\"type\"\\s*:\\s*\"[^\"]+\"","\"type\":\"CTLauncher\"");
                    Files.writeString(Path.of(verDir,versionId+".json"),cached);}
                ver=Json.obj(Json.parse(cached));}
            String mainClass=Json.str(ver,"mainClass");
            Map<String,Object> dl=Json.sub(ver,"downloads");if(dl!=null){Map<String,Object> cd=Json.sub(dl,"client");if(cd!=null){
                File cj=new File(verDir+File.separator+versionId+".jar");dlg.status("Downloading "+versionId+".jar...");Net.download(Json.str(cd,"url"),cj,Json.num(cd,"size"),(d,t)->dlg.fileProgress(versionId+".jar",d,t));}}
            List<Object> libs=Json.subarr(ver,"libraries");List<String> cp=new ArrayList<>();File cj2=new File(verDir+File.separator+versionId+".jar");if(cj2.exists())cp.add(cj2.getAbsolutePath());
            if(libs!=null){int li=0;for(Object lo:libs){Map<String,Object> lib=Json.obj(lo);li++;if(!rulesAllow(lib))continue;
                Map<String,Object> ld=Json.sub(lib,"downloads");if(ld==null)continue;
                Map<String,Object> art=Json.sub(ld,"artifact");if(art!=null){String path=Json.str(art,"path");File dest=new File(libDir+File.separator+path.replace("/",File.separator));
                    dlg.status("Lib ("+li+"/"+libs.size()+") "+dest.getName());Net.download(Json.str(art,"url"),dest,Json.num(art,"size"),(d,t)->dlg.fileProgress(dest.getName(),d,t));
                    cp.add(dest.getAbsolutePath());if(path!=null&&path.contains("natives-"))extractZip(dest,new File(natDir));}
                Map<String,Object> nat=Json.sub(lib,"natives");if(nat!=null){String cl=Json.str(nat,osName());if(cl==null&&"osx".equals(osName()))cl=Json.str(nat,"macos");
                    if(cl!=null){cl=cl.replace("${arch}",System.getProperty("os.arch").contains("64")?"64":"32");Map<String,Object> cls=Json.sub(ld,"classifiers");
                        if(cls!=null){Map<String,Object> na=Json.sub(cls,cl);if(na!=null){File dest=new File(libDir+File.separator+Json.str(na,"path").replace("/",File.separator));
                            Net.download(Json.str(na,"url"),dest,Json.num(na,"size"),null);extractZip(dest,new File(natDir));}}}}}}
            Map<String,Object> ai=Json.sub(ver,"assetIndex");String assetId="legacy";
            if(ai!=null){assetId=Json.str(ai,"id");File id2=new File(assDir+File.separator+"indexes");id2.mkdirs();File iff=new File(id2,assetId+".json");
                dlg.status("Assets index...");Net.download(Json.str(ai,"url"),iff,Json.num(ai,"size"),null);
                Map<String,Object> objs=Json.sub(Json.obj(Json.parse(Files.readString(iff.toPath()))),"objects");
                if(objs!=null){File od=new File(assDir+File.separator+"objects");od.mkdirs();int tot=objs.size(),cnt=0;
                    for(Map.Entry<String,Object> e:objs.entrySet()){Map<String,Object> a=Json.obj(e.getValue());String h=Json.str(a,"hash");String pr=h.substring(0,2);File de=new File(od,pr+File.separator+h);cnt++;
                        if(!de.exists()){if(cnt%50==0||cnt==tot)dlg.status("Assets ("+cnt+"/"+tot+")...");Net.download(CTlauncher0_1_1.RESOURCES_URL+pr+"/"+h,de,Json.num(a,"size"),null);}}}}
            String javaPath=ensureJR(ver,base,dlg);if(javaPath==null){javaPath=System.getProperty("java.home")+File.separator+"bin"+File.separator+"java";if(osName().equals("windows"))javaPath+=".exe";}
            dlg.status("Launching...");String uuid=UUID.nameUUIDFromBytes(("OfflinePlayer:"+username).getBytes()).toString();String cps=String.join(File.pathSeparator,cp);
            Map<String,String> vars=new LinkedHashMap<>();vars.put("${auth_player_name}",username);vars.put("${version_name}",versionId);vars.put("${game_directory}",base);
            vars.put("${assets_root}",assDir);vars.put("${assets_index_name}",assetId);vars.put("${auth_uuid}",uuid);vars.put("${auth_access_token}","0");
            vars.put("${user_type}","legacy");vars.put("${version_type}","CTLauncher");vars.put("${user_properties}","{}");
            vars.put("${auth_session}","token:0");vars.put("${game_assets}",assDir);vars.put("${natives_directory}",natDir);vars.put("${launcher_name}","CTLauncher");
            vars.put("${launcher_version}","0.2");vars.put("${classpath}",cps);vars.put("${classpath_separator}",File.pathSeparator);vars.put("${library_directory}",libDir);
            vars.put("${resolution_width}","854");vars.put("${resolution_height}","480");vars.put("${clientid}","");vars.put("${auth_xuid}","");vars.put("${path_separator}",File.pathSeparator);
            List<String> cmd=new ArrayList<>();cmd.add(javaPath);
            Map<String,Object> args=Json.sub(ver,"arguments");
            if(args!=null){List<Object> ja=Json.subarr(args,"jvm");if(ja!=null)for(Object a:ja){if(a instanceof String)cmd.add(sv((String)a,vars));
                else{Map<String,Object> ao=Json.obj(a);if(ao!=null&&arAllow(ao)){Object val=ao.get("value");if(val instanceof String)cmd.add(sv((String)val,vars));
                    else{List<Object> vl=Json.arr(val);if(vl!=null)for(Object v2:vl)if(v2 instanceof String)cmd.add(sv((String)v2,vars));}}}}}
            else{if("osx".equals(osName()))cmd.add("-XstartOnFirstThread");cmd.add("-Djava.library.path="+natDir);cmd.add("-Dminecraft.launcher.brand=CTLauncher");}
            cmd.removeIf(a->a.startsWith("-Xmx"));cmd.removeIf(a->a.startsWith("-Xms"));cmd.add(1,"-Xmx"+ramGB+"G");cmd.add(2,"-Xms512M");
            if(!cmd.contains("-cp")&&!cmd.contains("-classpath")){cmd.add("-cp");cmd.add(cps);}cmd.add(mainClass);
            if(args!=null){List<Object> ga=Json.subarr(args,"game");if(ga!=null)for(Object a:ga){if(a instanceof String)cmd.add(sv((String)a,vars));
                else{Map<String,Object> ao=Json.obj(a);if(ao!=null&&arAllow(ao)){Object val=ao.get("value");if(val instanceof String)cmd.add(sv((String)val,vars));
                    else{List<Object> vl=Json.arr(val);if(vl!=null)for(Object v2:vl)if(v2 instanceof String)cmd.add(sv((String)v2,vars));}}}}}
            else{String ma=Json.str(ver,"minecraftArguments");if(ma!=null)for(String pt:ma.split(" "))cmd.add(sv(pt.trim(),vars));
                else{cmd.add("--username");cmd.add(username);cmd.add("--version");cmd.add(versionId);cmd.add("--gameDir");cmd.add(base);cmd.add("--assetsDir");cmd.add(assDir);
                    cmd.add("--assetIndex");cmd.add(assetId);cmd.add("--uuid");cmd.add(uuid);cmd.add("--accessToken");cmd.add("0");cmd.add("--versionType");cmd.add("CTLauncher");cmd.add("--userProperties");cmd.add("{}");}}
            cmd.removeIf(a->a.contains("${"));
            if(cmd.get(0).contains(System.getProperty("java.home"))){int jv=jMaj();cmd.removeIf(a->a.startsWith("--sun-misc-unsafe-memory-access")&&jv<23);cmd.removeIf(a->a.startsWith("--enable-native-access")&&jv<16);}
            System.out.println("=== CTLauncher 0.2 === Version: "+versionId+" User: "+username+" Java: "+cmd.get(0)+" ===");
            SwingUtilities.invokeLater(()->dlg.dispose());
            ProcessBuilder pb=new ProcessBuilder(cmd);pb.directory(new File(base));pb.redirectErrorStream(true);Process proc=pb.start();
            StringBuilder po=new StringBuilder();BufferedReader rd=new BufferedReader(new InputStreamReader(proc.getInputStream()));String ln;
            while((ln=rd.readLine())!=null){System.out.println("[MC] "+ln);if(po.length()<4096)po.append(ln).append("\n");}
            int ex=proc.waitFor();System.out.println("=== Exit "+ex+" ===");
            if(ex!=0){String o=po.toString();String sn=o.length()>800?"..."+o.substring(o.length()-800):o;SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(parent,"Exit "+ex+":\n\n"+sn,"Error",JOptionPane.ERROR_MESSAGE));}
        }catch(Exception ex){SwingUtilities.invokeLater(()->dlg.dispose());ex.printStackTrace();SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(parent,"Failed:\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE));}
        },"CTL-Launch").start();
    }
    static String rp(){String o=osName();String a=System.getProperty("os.arch").toLowerCase();boolean arm=a.contains("aarch64")||a.contains("arm64");
        if("osx".equals(o))return arm?"mac-os-arm64":"mac-os";if("linux".equals(o))return arm?"linux-arm64":"linux";return arm?"windows-arm64":a.contains("64")?"windows-x64":"windows-x86";}
    static String ensureJR(Map<String,Object> ver,String base,ProgressDialog dlg){try{
        Map<String,Object> jv=Json.sub(ver,"javaVersion");if(jv==null)return null;String comp=Json.str(jv,"component");if(comp==null)return null;
        String rd2=base+File.separator+"runtimes"+File.separator+comp;String ex=findJB(rd2);if(ex!=null){System.out.println("[JRE] Cached: "+comp);return ex;}
        int mv=(int)Json.num(jv,"majorVersion");dlg.status("Downloading Java "+mv+"...");
        Map<String,Object> all=Json.obj(Json.parse(Net.fetchString(CTlauncher0_1_1.JAVA_ALL_URL)));
        Map<String,Object> pl=Json.sub(all,rp());if(pl==null)pl=Json.sub(all,"gamecore");if(pl==null)return null;
        List<Object> cl=Json.subarr(pl,comp);if(cl==null||cl.isEmpty())return null;
        String mu=Json.str(Json.sub(Json.obj(cl.get(0)),"manifest"),"url");
        Map<String,Object> files=Json.sub(Json.obj(Json.parse(Net.fetchString(mu))),"files");if(files==null)return null;
        new File(rd2).mkdirs();int tot=files.size(),cnt=0;
        for(Map.Entry<String,Object> e:files.entrySet()){Map<String,Object> info=Json.obj(e.getValue());String type=Json.str(info,"type");cnt++;
            File dest=new File(rd2+File.separator+e.getKey().replace("/",File.separator));
            if("directory".equals(type))dest.mkdirs();
            else if("file".equals(type)){Map<String,Object> raw=Json.sub(Json.sub(info,"downloads"),"raw");if(raw==null)continue;
                if(cnt%40==0||cnt==tot)dlg.status("Java "+mv+" ("+cnt+"/"+tot+")");Net.download(Json.str(raw,"url"),dest,Json.num(raw,"size"),null);
                if(Boolean.TRUE.equals(info.get("executable")))dest.setExecutable(true,false);}
            else if("link".equals(type)){String tgt=Json.str(info,"target");if(tgt!=null&&!"windows".equals(osName())){dest.getParentFile().mkdirs();
                try{Files.deleteIfExists(dest.toPath());Files.createSymbolicLink(dest.toPath(),Path.of(tgt));}catch(Exception ignored){}}}}
        String jb=findJB(rd2);if(jb!=null)System.out.println("[JRE] Installed Java "+mv);return jb;
    }catch(Exception e){System.err.println("[JRE] Failed: "+e.getMessage());return null;}}
    static String findJB(String d){File f=new File(d,"jre.bundle/Contents/Home/bin/java".replace("/",File.separator));if(f.isFile())return f.getAbsolutePath();
        f=new File(d,"bin"+File.separator+"java");if(f.isFile())return f.getAbsolutePath();f=new File(d,"bin"+File.separator+"java.exe");if(f.isFile())return f.getAbsolutePath();return null;}
    static String sv(String a,Map<String,String> v){String r=a;for(Map.Entry<String,String> e:v.entrySet())r=r.replace(e.getKey(),e.getValue());return r;}
    static int jMaj(){String v=System.getProperty("java.version","1.8");if(v.startsWith("1."))v=v.substring(2);int d=v.indexOf('.');if(d>0)v=v.substring(0,d);int h=v.indexOf('-');if(h>0)v=v.substring(0,h);try{return Integer.parseInt(v);}catch(Exception e){return 8;}}
    static boolean arAllow(Map<String,Object> ao){List<Object> r=Json.subarr(ao,"rules");if(r==null)return true;boolean a=false;for(Object ro:r){Map<String,Object> rl=Json.obj(ro);if(rl==null)continue;
        if(Json.sub(rl,"features")!=null)continue;Map<String,Object> os=Json.sub(rl,"os");if(os!=null){String n=Json.str(os,"name");if(n!=null&&!osMatches(n))continue;
        String ar=Json.str(os,"arch");if(ar!=null&&"x86".equals(ar)&&(System.getProperty("os.arch").contains("64")||System.getProperty("os.arch").contains("aarch64")))continue;}
        a="allow".equals(Json.str(rl,"action"));}return a;}
    static boolean rulesAllow(Map<String,Object> lib){List<Object> r=Json.subarr(lib,"rules");if(r==null)return true;boolean a=false;for(Object ro:r){Map<String,Object> rl=Json.obj(ro);
        Map<String,Object> os=Json.sub(rl,"os");if(os!=null){String n=Json.str(os,"name");if(n!=null&&!osMatches(n))continue;
        String ar=Json.str(os,"arch");if(ar!=null&&"x86".equals(ar)&&(System.getProperty("os.arch").contains("64")||System.getProperty("os.arch").contains("aarch64")))continue;}
        a="allow".equals(Json.str(rl,"action"));}return a;}
    static void extractZip(File z,File dd){try(ZipInputStream zi=new ZipInputStream(new FileInputStream(z))){ZipEntry e;while((e=zi.getNextEntry())!=null){
        if(e.isDirectory())continue;String n=e.getName();if(n.startsWith("META-INF"))continue;File o=new File(dd,n);o.getParentFile().mkdirs();
        try(FileOutputStream fo=new FileOutputStream(o)){byte[] b=new byte[4096];int nr;while((nr=zi.read(b))!=-1)fo.write(b,0,nr);}}}catch(IOException e){System.err.println("Extract fail: "+z.getName());}}
}
