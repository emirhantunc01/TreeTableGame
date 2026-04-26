import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;

/**
 * Fireball.java
 * Manages the active fireballs ('o') shot by the player.
 * Fireballs move step-by-step, destroy robots instantly, and stop at walls.
 */
public class Fireball {
    public static final int MAX_FIREBALLS = 50;

    // Standard arrays to hold active fireballs
    private int[] fbX = new int[MAX_FIREBALLS];
    private int[] fbY = new int[MAX_FIREBALLS];
    private int[] fbDx = new int[MAX_FIREBALLS];
    private int[] fbDy = new int[MAX_FIREBALLS];
    private boolean[] active = new boolean[MAX_FIREBALLS];

    private int packedCount = 0; // Ammunition collected by the player
    private Console cn;
    private TextAttributes colorFireball = new TextAttributes(Color.RED, Color.BLACK);

    public Fireball(Console cn) {
        this.cn = cn;
    }

    public void addPacked() { packedCount++; }
    public int getPackedCount() { return packedCount; }

    // Called when the player fires a fireball
    public boolean fire(int startX, int startY, int dx, int dy) {
        if (packedCount <= 0 || (dx == 0 && dy == 0)) return false;

        // Find an empty fireball slot
        for (int i = 0; i < MAX_FIREBALLS; i++) {
            if (!active[i]) {
                fbX[i] = startX + dx;
                fbY[i] = startY + dy;
                fbDx[i] = dx;
                fbDy[i] = dy;
                active[i] = true;
                packedCount--; // Consume ammo
                return true;
            }
        }
        return false;
    }

    // Called every time unit in the game loop (GameEngine)
    public void update(Robot[] robots, int robotCount, Player p) {
        for (int i = 0; i < MAX_FIREBALLS; i++) {
            if (active[i]) {
                // Erase old position
                cn.getTextWindow().output(fbX[i], fbY[i], ' ');

                // Advance
                fbX[i] += fbDx[i];
                fbY[i] += fbDy[i];

                // Boundary and wall collision check (cannot pass through objects)
                if (fbX[i] < 0 || fbX[i] >= Maze.COLS || fbY[i] < 0 || fbY[i] >= Maze.ROWS || Maze.map[fbY[i]][fbX[i]] == '#') {
                    active[i] = false;
                    continue;
                }

                // Robot collision check
                for (int j = 0; j < robotCount; j++) {
                    if (robots[j].isAlive() && robots[j].getX() == fbX[i] && robots[j].getY() == fbY[i]) {
                        robots[j].die();
                        p.addScore(50); // Award player 50 points
                    }
                }
                // Note: Project docs say "One fireball can destroy many robots", so
                // we do NOT set active[i] = false after hitting a robot. It keeps moving.

                // Draw at new position
                if (active[i]) {
                    cn.getTextWindow().output(fbX[i], fbY[i], 'o', colorFireball);
                }
            }
        }
    }
}