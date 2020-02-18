import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * PWM Game
 * 
 * @author Leonardo Ono (ono.leo@gmail.com);
 */
public class View extends Canvas implements KeyListener {

    private static final Font FONT = new Font("arial", Font.BOLD, 30);
    private static final Font FONT2 = new Font("arial", Font.BOLD, 16);

    private static final int SCREEN_WIDTH = 800, SCREEN_HEIGHT = 600;
    private BufferStrategy bs;
    private boolean running;
    
    private boolean pressing;
    private boolean pressingConsumed;
    private double x;
    private double y;
    
    private final List<Boolean> pulse = new ArrayList<>();
    private final List<Integer> ys = new ArrayList<>();
    private final List<Integer> guide = new ArrayList<>();
    private double tolerance = 100;
    
    private boolean started;
    private boolean gameOver;
    private boolean restart = false;
    
    private int frame;
    
    private final List<Score> scores = new ArrayList<>();
    
    private int totalScore = 0;

    /**
     * Score class.
     */
    private static class Score {

        private static final Composite[] ALPHA_COMPS = new Composite[256];
        private boolean visible = false;
        private double x;
        private double y;
        private double vy;
        private double vx;
        private int point;
        private int frame;
        private int alpha;
        private String message;
        private static final Font FONT = new Font("arial", Font.BOLD, 30);

        static {
            for (int i = 0; i < 256; i++) {
                ALPHA_COMPS[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / 255f);
            }
        }

        public Score() {
        }

        public boolean isFree() {
            return !visible;
        }

        public void show(double x, double y, int point) {
            this.x = x;
            this.y = y;
            this.vy = -1;
            this.vx = -1;
            visible = true;
            frame = 0;
            this.point = point;
            alpha = 255;
            if (point < 0) {
                message = "Ouch!!! ";
            }
            else if (point > 90) {
                message = "Excellent!!! ";
            }
            else if (point > 75) {
                message = "Very good!! ";
            }
            else if (point > 50) {
                message = "Nice! ";
            }
            else {
                message = "";
            }
        }

        public void update() {
            if (!visible) {
                return;
            }

            frame++;
            y += vy;
            vy -= 0.15;
            x += vx;
            vx -= (1 * Math.random() - 0.5);
            alpha -= 5;
            if (alpha < 0) {
                alpha = 0;
                visible = false;
            }
        }

        public void draw(Graphics2D g) {
            if (!visible) {
                return;
            }
            Composite oc = g.getComposite();
            g.setComposite(ALPHA_COMPS[alpha]);
            g.setFont(FONT);
            g.setColor(new Color((int) (Integer.MAX_VALUE * Math.random())));
            if (point > 0) {
                g.drawString(message + "" + point + "pts", (int) x, (int) y);
            }
            else {
                g.drawString(message, (int) x, (int) y);
            }
            g.setComposite(oc);
        }

    }
    
    public void start() {
        addKeyListener(this);
        createGuide();
        createScores();
        
        createBufferStrategy(2);
        bs = getBufferStrategy();
        running = true;
        new Thread(() -> {
            while (running) {
                update();
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                draw(g);
                g.dispose();
                bs.show();
                
                try {
                    Thread.sleep(1000 / 60);
                } catch (InterruptedException ex) {
                }
            }
        }).start();
        reset();
    }

    private void createScores() {
        for (int i = 0; i < 10; i++) {
            scores.add(new Score());
        }
    }
    
    private void createGuide() {
        guide.clear();
        for (int i = 0; i < 800; i++) {
            guide.add(300);
        }
    }
    
    public synchronized void reset() {
        x = 400;
        y = 300 - 45;
        gameOver = false;
        frame = 0;
        totalScore = 0;
        ys.clear();
        pulse.clear();
        createGuide();
        started = false;
        tolerance = 100;
        restart = false;
    }

    private void addY(int yadd) {
        ys.add(yadd);
        if (ys.size() > 400) {
            ys.remove(0);
        }
    }

    private void addPulse(boolean on) {
        pulse.add(on);
        if (pulse.size() > 400) {
            pulse.remove(0);
        }
    }
    
    private void generateNextGuide() {
        double scale = Math.min(frame, 1200) / 1200.0;
        
        double k = 0;
        //if (frame > 500) {
        //    double hk = 50 * (Math.min(1000, frame - 500) / 1000.0);
        //    k = hk * Math.sin(Math.sqrt(frame * 1));
        //}
        
        double k2 = 0;
        if (frame > 2000) {
            double hk2 = 50 * (Math.min(1000, frame - 2000) / 1000.0);
            k2 = hk2 * Math.cos(frame * 0.05);
        }
        
        double k3 = 200 * Math.sin(frame * 0.01);
        int gy = (int) (300 + scale * (k3 + k2 - k));
        guide.add(gy);
        guide.remove(0);
    }
    
    private void update() {
        if (restart) {
            restart = false;
            reset();
        }
        
        scores.forEach((score) -> {
            score.update();
        });

        if (!started) {
            return;
        }

        if (gameOver) {
            return;
        }

        frame++;
        
        generateNextGuide();
        
        if (pressing) {
            y = y + 4;
        }
        else {
            y = y - 4;
        }
        
        if (y > guide.get(400) + tolerance || y < guide.get(400) - tolerance) {
            showScorePoint(-1);
            gameOver = true;
        }
        
        if (isSpacePressedOnce()) {
            
            if (y > guide.get(400)) {
                showScorePoint(-1);
                gameOver = true;
            }
            else {
                showScorePoint();
            }
        }
        
        addY((int) y);
        addPulse(pressing);
        
        tolerance = Math.max(50, 100 - (frame / 10));
    }
    
    private void showScorePoint() {
        int point = (int) (100 * (1 - (Math.abs(guide.get(400) - y) / tolerance)));
        showScorePoint(point);
    }

    private void showScorePoint(int point) {
        for (Score score : scores) {
            if (score.isFree()) {
                score.show(400, y, point);
                break;
            }
        }
        if (point > 0) {
            totalScore += point;
        }
    }
    
    private void draw(Graphics2D g) {
        g.clearRect(0, 0, 800, 600);
        drawPulse(g);
        drawGuide(g);
        drawYs(g);
        if (gameOver) {
            g.setColor(new Color((int) (Integer.MAX_VALUE * Math.random())));
        }
        else {
            g.setColor(Color.WHITE);
        }
        g.fillOval((int) (x - 5), (int) (y - 5), 10, 10);
        
        scores.forEach((score) -> {
            score.draw(g);
        });
        
        g.setFont(FONT);
        g.setColor(Color.WHITE);
        g.drawString("by O.L.", 670, 580);

        if (!gameOver && !started) {
            g.drawString("PWM Game", 50, 50);
            if (((int) (System.nanoTime() * 0.00000001) % 7) > 1) {
                g.drawString("keep pressing SPACE key to start", 150, 380);
            }
            g.setFont(FONT2);
            g.drawString("Instructions:", 50, 80);
            g.drawString("* keep pressing the SPACE key to "
                    + "move the ball down", 50, 100);
            
            g.drawString("* keep releasing the SPACE key to "
                    + "move the ball up", 50, 120);
            
            g.drawString("* you must cross the yellow line every "
                    + "time you press or release the space key", 50, 140);
            
            g.drawString("* you cannot cross the gray line", 50, 160);
            
            g.drawString("* the closer to the yellow line is where you change "
                    + "the ball's direction, the higher is the point", 50, 180);
        }
        else if (!gameOver) {
            g.drawString("SCORE: " + totalScore, 50, 50);
            g.drawString("Distance: " + frame, 500, 50);
        }
        else {
            g.drawString("YOUR SCORE: " + totalScore 
                    + " / Distance: " + frame, 50, 50);
            
            g.drawString("Press ENTER to try again", 50, 100);
            if ((frame & 0b11111) == 0b10000) {
                showScorePoint(-1);
            }
        }
    }
    
    private void drawYs(Graphics2D g) {
        g.setColor(Color.WHITE);
        int xh = 400;
        for (int i = ys.size() - 1; i > 0; i--) {
            int ya = ys.get(i);
            int yb = ys.get(i - 1);
            g.drawLine(xh, ya, xh - 1, yb);
            xh--;
        }
    }

    private void drawPulse(Graphics2D g) {
        g.setColor(Color.BLUE);
        int xh = 400;
        for (int i = pulse.size() - 1; i > 0; i--) {
            Boolean ya = pulse.get(i);
            Boolean yb = pulse.get(i - 1);
            g.drawLine(xh, ya ? 500 : 550, xh - 1, yb ? 500 : 550);
            xh--;
        }
    }
            
    private void drawGuide(Graphics2D g) {
        for (int i = 0; i < guide.size() - 1; i++) {
            int ya = guide.get(i);
            //g.setColor(Color.GRAY);
            //g.drawLine(i, (int) (ya - tolerance), i, (int) (ya + tolerance));
            g.setColor(Color.DARK_GRAY);
            g.drawLine(i, (int) (ya - tolerance), i, (int) (ya - tolerance - 1));
            g.drawLine(i, (int) (ya + tolerance), i, (int) (ya + tolerance + 1));
            g.setColor(Color.YELLOW);
            g.drawLine(i, ya - 1, i, ya + 1);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private boolean isSpacePressedOnce() {
        if (pressing && !pressingConsumed) {
            pressingConsumed = true;
            return true;
        }
        return false;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            pressing = true;
            if (!gameOver && !started) {
                started = true;
            }
        }
        
        if (gameOver && e.getKeyCode() == KeyEvent.VK_ENTER) {
            restart = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            pressing = false;
            pressingConsumed = false;

            if (y < guide.get(400)) {
                showScorePoint(-1);
                gameOver = true;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            View view = new View();
            view.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
            view.setBackground(Color.BLACK);
            JFrame frame1 = new JFrame();
            frame1.setTitle("Java PWM Game");
            frame1.getContentPane().add(view);
            frame1.setResizable(false);
            frame1.pack();
            frame1.setLocationRelativeTo(null);
            frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame1.setVisible(true);
            view.start();
            view.requestFocus();
        });
    }
    
}
