import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;
import java.util.Random;

/**
 * Robot.java
 * Represents the enemies in the game.
 * 50% chance to be Targeted (Red) - seeks nearest logic symbol.
 * 50% chance to be Random (Green) - moves randomly.
 */
public class Robot {
    private int x, y;
    private boolean isTargeted; // true = Red (Targeted), false = Green (Random)
    private boolean isAlive = true;

    private Console cn;
    private Random rnd = new Random();

    private TextAttributes colorTargeted = new TextAttributes(Color.RED, Color.BLACK);
    private TextAttributes colorRandom = new TextAttributes(Color.GREEN, Color.BLACK);
//buraya bakılacak
    public Robot(Console cn, int startX, int startY) {
        this.cn = cn;
        this.x = startX;
        this.y = startY;
        this.isTargeted = rnd.nextBoolean(); // 50% chance
    }

    public Robot(Console cn, int startX, int startY, boolean isTargeted) {
        this.cn = cn;
        this.x = startX;
        this.y = startY;
        this.isTargeted = isTargeted;
    }

    public void move(Item[] items, int itemCount, Robot[] robots, int robotCount) {
        if (!isAlive) return;

        int nextX = x;
        int nextY = y;

        boolean targetedMod = rnd.nextBoolean();

        if (targetedMod) {
            // moving towards nearest logical symbol
            int closeDist = Integer.MAX_VALUE;
            int targetX = -1;
            int targetY = -1;

            for (int i = 0; i < itemCount; i++) {
                char type = items[i].getType();

                // only logical symbols
                if (type != '@' && type != ' ') {
                    int dist = Math.abs(x - items[i].getX()) + Math.abs(y - items[i].getY());

                    if (dist < closeDist) {
                        closeDist = dist;
                        targetX = items[i].getX();
                        targetY = items[i].getY();
                    }
                }
            }

            if (targetX != -1) {
                int dx = 0;
                int dy = 0;

                if (targetX > x) dx = 1;
                else if (targetX < x) dx = -1;

                if (targetY > y) dy = 1;
                else if (targetY < y) dy = -1;

                // try first x direc then y direc
                int[][] tries = {
                        {dx, 0},
                        {0, dy}
                };

                for (int[] t : tries) {
                    if (t[0] == 0 && t[1] == 0) continue;

                    if (isValidMove(x + t[0], y + t[1], robots, robotCount)) {
                        nextX = x + t[0];
                        nextY = y + t[1];
                        break;
                    }
                }


            }

        } else {
            // random movement
            int dir = rnd.nextInt(4);

            if (dir == 0) nextX = x + 1;
            else if (dir == 1) nextY = y + 1;
            else if (dir == 2) nextX = x - 1;
            else if (dir == 3) nextY = y - 1;

            if (!isValidMove(nextX, nextY, robots, robotCount)) {
                nextX = x;
                nextY = y;
            }
        }

        // erase old loc
        if (x != nextX || y != nextY) {
            erase();
            x = nextX;
            y = nextY;
        }

        draw(targetedMod);
    }

    private boolean isValidMove(int nx, int ny, Robot[] robots, int robotCount) {
        if (nx < 0 || nx >= Maze.COLS || ny < 0 || ny >= Maze.ROWS) return false;
        if (Maze.map[ny][nx] == '#') return false;

        // Check collision with other alive robots
        for (int i = 0; i < robotCount; i++) {
            if (robots[i] != this && robots[i].isAlive() && robots[i].getX() == nx && robots[i].getY() == ny) {
                return false;
            }
        }
        return true;
    }

    public void draw(boolean targetedThisTurn) {
        if (isAlive) {
            if (targetedThisTurn) {
                cn.getTextWindow().output(x, y, 'X', colorTargeted);
            } else {
                cn.getTextWindow().output(x, y, 'X', colorRandom);
            }
        }
    }

    public void draw() {
        if (isAlive) {
            cn.getTextWindow().output(x, y, 'X', colorRandom);
        }
    }

    public void erase() {
        cn.getTextWindow().output(x, y, ' ');
    }

    public void die() {
        isAlive = false;
        erase();
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isAlive() { return isAlive; }
}