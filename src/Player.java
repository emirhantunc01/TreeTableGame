import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
 * Player.java
 * Represents the player-controlled entity (P).
 * Handles movement logic, storage mode switching (Tree/Backpack),
 * tracks the last facing direction for fireballs, and manages a standard array backpack.
 */
public class Player {
    public static final int INITIAL_MAX_HP = 100;

    private int x, y;
    private int life = INITIAL_MAX_HP;
    private int score = 0;

    // Last facing direction of the player (for shooting fireballs)
    // Initially facing right (dx=1, dy=0)
    private int lastDx = 1;
    private int lastDy = 0;

    // Storage Mode: true = Tree, false = Backpack
    private boolean storageModeTree = true;

    // Standard array for the backpack (maximum 8 items)
    private char[] backpack = new char[8];
    private int backpackCount = 0;

    private Console cn;
    private Maze maze;
    private ExpressionTree tree; // ExpressionTree reference (for placing symbols)

    private TextAttributes colorPlayer = new TextAttributes(Color.GREEN, Color.BLACK);


    public Player(Console cn, Maze maze) {
        this.cn = cn;
        this.maze = maze;

        Random rnd = new Random();

        do {
            x = rnd.nextInt(Maze.COLS - 2) + 1;
            y = rnd.nextInt(Maze.ROWS - 2) + 1;
        } while (Maze.map[y][x] == '#');
    }

    public void move(int key) {
        int dx = 0;
        int dy = 0;

        if (key == KeyEvent.VK_LEFT) dx = -1;
        else if (key == KeyEvent.VK_RIGHT) dx = 1;
        else if (key == KeyEvent.VK_UP) dy = -1;
        else if (key == KeyEvent.VK_DOWN) dy = 1;

        // Return if an invalid key was pressed
        if (dx == 0 && dy == 0) return;

        // Update player's last movement direction
        lastDx = dx;
        lastDy = dy;

        erase();

        if (isValidMove(x + dx, y + dy)) {
            x += dx;
            y += dy;
        }

        draw();
    }

    public void draw() {
        cn.getTextWindow().output(x, y, 'P', colorPlayer);
    }

    public void erase() {
        cn.getTextWindow().output(x, y, ' ');
    }

    private boolean isValidMove(int targetX, int targetY) {
        if (targetX < 0 || targetX >= Maze.COLS || targetY < 0 || targetY >= Maze.ROWS)
            return false;
        if (Maze.map[targetY][targetX] == '#')
            return false;
        return true;
    }

    // Called when 'M' key is pressed to toggle storage mode
    public void toggleStorageMode() {
        storageModeTree = !storageModeTree;
    }

    // Logic for adding a collected item to the backpack or tree
    public boolean collectSymbol(char symbol) {
        // adds symbols to tree
        if (storageModeTree) {
            if (tree != null) {
                return tree.placeSymbol(symbol);
            }
            return false;
        }

        // add symbols to backpack
        if (addToBackpack(symbol)) {
            return true;
        }

        // if backpack is full, add symbols to the first empty tree slot automatically
        if (tree != null) {
            return tree.placeSymbolInFirstEmptySlot(symbol);
        }

        return false;
    }
    public Character takeFromBackpack() {
        if (backpackCount == 0) return null;
        char c = backpack[backpackCount - 1];
        backpackCount--;
        return c;
    }

    public boolean isBackpackFull() {
        return backpackCount >= backpack.length;
    }

    // Getters & Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getLife() { return life; }
    public int getScore() { return score; }
    public boolean isStorageModeTree() { return storageModeTree; }

    // Direction methods called by the fireball class
    public int getLastDx() { return lastDx; }
    public int getLastDy() { return lastDy; }

    public void addScore(int points) { score += points; }
    public void takeDamage(int amount) { life -= amount; }

    // Sets the ExpressionTree reference (called by GameEngine)
    public void setExpressionTree(ExpressionTree tree) { this.tree = tree; }

    // --- Backpack Access Methods ---
    public char[] getBackpack() { return backpack; }
    public int getBackpackCount() { return backpackCount; }

    // Removes the item at the given index from the backpack and shifts the rest
    public char removeFromBackpack(int index) {
        if (index < 0 || index >= backpackCount) return ' ';
        char removed = backpack[index];
        for (int i = index; i < backpackCount - 1; i++) {
            backpack[i] = backpack[i + 1];
        }
        backpackCount--;
        backpack[backpackCount] = ' '; // Clear the last slot
        return removed;
    }

    // Add an item to the backpack
    public boolean addToBackpack(char symbol) {
        if (backpackCount < backpack.length) {
            backpack[backpackCount] = symbol;
            backpackCount++;
            return true;
        }
        return false; // Backpack is full
    }

    // Clears the backpack (empties all items)
    public void clearBackpack() {
        for (int i = 0; i < backpack.length; i++) {
            backpack[i] = ' ';
        }
        backpackCount = 0;
    }
}
