import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Main class <p></p>
 * Creates and manages the canvas and <p></p>
 * Ball object
 */
class Ball {
    private int x, y, dx, dy;
    private final int DIAMETER = 20;
    private boolean inHole = false;
    private final Color color;
    private final BallCanvas canvas;

    /**
     *
     * @param canvas filling board
     * @param color passing color red/blue in instance
     */
    public Ball(BallCanvas canvas, Color color) {
        this.canvas = canvas;
        this.color = color;
        this.x = (int) (Math.random() * (canvas.getWidth() - DIAMETER));
        this.y = (int) (Math.random() * (canvas.getHeight() - DIAMETER));
        this.dx = Math.random() > 0.5 ? 1 : -1;
        this.dy = Math.random() > 0.5 ? 1 : -1;
    }

    public void move() {
        if (inHole) return;
        x += dx;
        y += dy;

        if (x <= 0 || x >= canvas.getWidth() - DIAMETER) dx = -dx;
        if (y <= 0 || y >= canvas.getHeight() - DIAMETER) dy = -dy;

        if (Math.abs(x - canvas.getWidth() / 2) < 10 && Math.abs(y - canvas.getHeight() / 2) < 10) {
            inHole = true;
        }
    }

    public boolean isGone() {
        return inHole;
    }

    public void draw(Graphics g) {
        if (!inHole) {
            g.setColor(color);
            g.fillOval(x, y, DIAMETER, DIAMETER);
        }
    }
}

class BallThread extends Thread {
    private final Ball ball;
    private final BallCanvas canvas;

    public BallThread(Ball ball, BallCanvas canvas, int priority) {
        this.ball = ball;
        this.canvas = canvas;
        setPriority(priority);
    }

    public void run() {
        while (!ball.isGone()) {
            ball.move();
            canvas.repaint();
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        canvas.removeGoneBalls();
    }
}

class BallCanvas extends JPanel {
    private final List<Ball> balls = new ArrayList<>();
    private final JLabel label;
    private int inHoleCount = 0;

    public BallCanvas(JLabel label) {
        this.label = label;
    }

    public synchronized void addBall(Color color, int BallCount) {
        for (int i = 0; i < BallCount; i++) {
            Ball ball = new Ball(this, color);
            balls.add(ball);
            new BallThread(ball, this, color == Color.RED ? Thread.MAX_PRIORITY : Thread.MIN_PRIORITY).start();
        }
    }

    public synchronized void removeGoneBalls() {
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball ball = it.next();
            if (ball.isGone()) {
                it.remove();
                inHoleCount++;
                label.setText("Balls in hole: " + inHoleCount);
            }
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Ball ball : balls) {
            ball.draw(g);
        }
    }
}

/**
 * Constructs UI with labels and action buttons
 */
public class SnookerSimulation {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Snooker Simulation");
        JLabel label = new JLabel("Balls in hole: 0");
        BallCanvas canvas = new BallCanvas(label);

        JButton addRed = new JButton("Add Red Ball");
        addRed.addActionListener(e -> canvas.addBall(Color.RED, 1));

        JButton addBlue = new JButton("Add Blue Ball");
        addBlue.addActionListener(e -> canvas.addBall(Color.BLUE, 1));

        JButton runBlueBallThreads = new JButton("Add 500 blue balls");
        runBlueBallThreads.addActionListener(e ->
                canvas.addBall(Color.BLUE,
                        500));   // For testing purpose

        JPanel panel = new JPanel();
        panel.add(addRed);
        panel.add(addBlue);
        panel.add(runBlueBallThreads);
        panel.add(label);

        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

