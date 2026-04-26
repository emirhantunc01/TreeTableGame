import enigma.console.Console;
import enigma.core.Enigma;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

/**
 * GameEngine.java
 * The core loop of the Tree & Table game.
 * Manages timing (1 tick = 100ms), game states (Maze, Tree, Table), 
 * and entity updates.
 */
public class GameEngine {
    private Console cn;

    // --- Game Screens and Components ---
    private Maze maze;
    private Player player;
    private InputQueue inputQueue;
    private ExpressionTree treeScreen;
    private Fireball fireballManager;
    private TableScreen tableScreen; // Truth table screen
    private boolean needsRedraw = false; // Redraw flag when screen changes

    // --- In-Game Entities (Standard Arrays) ---
    private Item[] items = new Item[100];
    private int itemCount = 0;

    private Robot[] robots = new Robot[50];
    private int robotCount = 0;

    // --- Timing and State Management ---
    private int timeUnit = 0; // Increments each loop cycle (1 unit = 100ms)
    private int seconds = 0;
    private int currentScreen = 1; // 1: Maze, 2: Tree, 3: Table
    private boolean isGameOver = false;

    // For keyboard input
    private int keypr = 0;
    private Random rnd = new Random();

    public GameEngine() throws Exception {
        cn = Enigma.getConsole("Tree & Table", 100, 30, 20);

        // Set up keyboard listener
        cn.getTextWindow().addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) { keypr = e.getKeyCode(); }
            public void keyReleased(KeyEvent e) {}
            public void keyTyped(KeyEvent e) {}
        });

        // Initialize components
        maze = Maze.loadFromFile("maze.txt");
        player = new Player(cn, maze);
        inputQueue = new InputQueue(cn);
        treeScreen = new ExpressionTree(cn);
        fireballManager = new Fireball(cn);

        // Give the Player a reference to the ExpressionTree (for symbol collection)
        player.setExpressionTree(treeScreen);

        // Rule: First 10 elements from the queue are placed in the maze at game start
        for (int i = 0; i < 10; i++) {
            insertElementFromQueue();
        }

        // Start the main game loop
        run();
    }

    private void run() throws InterruptedException {
        while (!isGameOver) {

            // Screen switching keys (1, 2, 3)
            if (keypr == KeyEvent.VK_1) { currentScreen = 1; ConsoleUtils.clearScreen(cn); needsRedraw = true; keypr = 0; }
            if (keypr == KeyEvent.VK_2) { currentScreen = 2; ConsoleUtils.clearScreen(cn); keypr = 0; }
            if (keypr == KeyEvent.VK_3) { currentScreen = 3; ConsoleUtils.clearScreen(cn); keypr = 0; }

            if (currentScreen == 1) {
                updateMazeScreen();
                Thread.sleep(100); // 1 Time Unit = 100 ms
            }
            else if (currentScreen == 2) {
                updateTreeScreen();
                Thread.sleep(100); // Tree screen doesn't advance time, but waits for input
            }
            else if (currentScreen == 3) {
                updateTableScreen();
                Thread.sleep(100);
            }
        }

        // Game Over Screen (High Score Table will be called)
        ConsoleUtils.clearScreen(cn);
        ConsoleUtils.printString(cn, 40, 15, "GAME OVER!");
        ConsoleUtils.printString(cn, 35, 17, "Final Score: " + player.getScore());
        // Game Over Screen
        ConsoleUtils.clearScreen(cn);
        ConsoleUtils.printString(cn, 40, 10, "GAME OVER!");
        ConsoleUtils.printString(cn, 38, 12, "Final Score: " + player.getScore());

        // --- High Score Table Integration ---
        DoublyLinkedList highScoreTable = new DoublyLinkedList();
        highScoreTable.loadFromFile("highscore.txt");

        // Add player's score to the list (name is "Player1" for now)
        highScoreTable.insert("Player1", player.getScore());

        // Draw the table to the screen
        highScoreTable.display(cn, 35, 15);
    }

    // ==========================================
    // MAZE SCREEN LOGIC
    // ==========================================
    private void updateMazeScreen() {
        timeUnit++;
        if (timeUnit % 10 == 0) seconds++; // Every 10 units = 1 second (1000ms)

        // 1. Draw (first time and when returning after a screen change)
        if (timeUnit == 1 || needsRedraw) {
            needsRedraw = false;
            maze.draw(cn);
            for (int i = 0; i < itemCount; i++) items[i].draw();
            for (int i = 0; i < robotCount; i++) robots[i].draw();
        }

        // 2. Player Movement and Input (every 1 time unit)
        if (keypr != 0) {
            if (keypr == KeyEvent.VK_SPACE) {
                // Fire fireball
                fireballManager.fire(player.getX(), player.getY(), player.getLastDx(), player.getLastDy());
            } else if (keypr == KeyEvent.VK_M) {
                // Toggle Storage Mode
                player.toggleStorageMode();
            } else {
                // Arrow Keys
                player.move(keypr);
                checkItemCollection();
            }
            keypr = 0;
        }
        player.draw(); // Keep player always on top

        // 3. Fireball Movement (every 1 time unit)
        fireballManager.update(robots, robotCount, player);

        // 4. Robot Movement (every 4 time units)
        if (timeUnit % 4 == 0) {
            for (int i = 0; i < robotCount; i++) {
                robots[i].move(items, itemCount, robots, robotCount);
            }
        }

        // 5. Add New Input (every 2 seconds = 20 time units)
        if (timeUnit % 20 == 0) {
            insertElementFromQueue();
        }

        // 6. Melee Damage Check (Neighbor Harming - every 1 time unit)
        checkNeighborDamage();

        // 7. Draw HUD and Input Queue
        drawHUD();
        inputQueue.draw(50, 2);

        // Death check
        if (player.getLife() <= 0) {
            isGameOver = true;
        }
    }

    // ==========================================
    // TREE SCREEN LOGIC
    // ==========================================
    private void updateTreeScreen() {
        treeScreen.draw();
        drawBackpackOnTreeScreen(); // Show backpack contents

        if (keypr != 0) {
            if (keypr == KeyEvent.VK_W || keypr == KeyEvent.VK_A || keypr == KeyEvent.VK_D) {
                if (treeScreen.moveCursor((char)keypr)) player.addScore(-1); // Penalty point
            }
            else if (keypr == KeyEvent.VK_T) {
                // Take last item from Backpack and place it into the tree
                if (player.getBackpackCount() > 0) {
                    char symbol = player.removeFromBackpack(player.getBackpackCount() - 1);
                    if (!treeScreen.placeSymbol(symbol)) {
                        // If placement fails, return item to backpack
                        player.addToBackpack(symbol);
                    }
                }
            }
            else if (keypr == KeyEvent.VK_R) {
                // Remove from tree and put in backpack
                char c = treeScreen.takeSymbol();
                if (c != ' ') {
                    player.addScore(-2); // Penalty point
                    if (!player.addToBackpack(c)) {
                        // If backpack is full, return item to tree
                        treeScreen.placeSymbol(c);
                    }
                }
            }
            else if (keypr == KeyEvent.VK_F) {
                if (treeScreen.finishTree()) {
                    int treeScore = treeScreen.calculateTreeScore();
                    player.addScore(treeScore);
                    // Create and initialize the TableScreen
                    tableScreen = new TableScreen(cn, treeScreen.getPostfix(), treeScore);
                    tableScreen.init();
                    currentScreen = 3; // Switch to Table Screen
                } else {
                    player.addScore(-10); // Penalty for invalid tree
                }
            }
            keypr = 0;
        }
    }

    // ==========================================
    // TABLE SCREEN LOGIC
    // ==========================================
    private void updateTableScreen() {
        if (tableScreen == null) {
            // Pressed 3 directly before the tree was completed
            ConsoleUtils.printString(cn, 5, 5, "No expression tree submitted.");
            ConsoleUtils.printString(cn, 5, 7, "Press 2 to go to Tree Screen first.");
            if (keypr != 0) keypr = 0;
            return;
        }

        // Update TableScreen; return to Maze if completed
        if (tableScreen.update(keypr, player)) {
            currentScreen = 1;
            needsRedraw = true;
            ConsoleUtils.clearScreen(cn);
            tableScreen = null; // Reset for the next round
        }
        keypr = 0;
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    // Dequeues an element and places it at a random empty spot in the maze
    private void insertElementFromQueue() {
        Character dequeued = inputQueue.dequeue();
        if (dequeued == null) return; // Do nothing if queue is empty
        char type = dequeued;

        int rx, ry;
        do {
            rx = rnd.nextInt(Maze.COLS - 2) + 1;
            ry = rnd.nextInt(Maze.ROWS - 2) + 1;
        } while (Maze.map[ry][rx] == '#' || hasEntityAt(rx, ry)); // Wall or another entity?

        if (type == 'R' || type == 'G') {
            if (robotCount < robots.length) {
                robots[robotCount] = new Robot(cn, rx, ry, type == 'R');
                robots[robotCount].draw(); // MUST draw the newly created robot immediately
                robotCount++;
            }
        } else {
            if (itemCount < items.length) {
                items[itemCount] = new Item(rx, ry, type, cn);
                items[itemCount].draw(); // MUST draw the newly created item immediately
                itemCount++;
            }
        }
    }

    // Checks whether an entity exists at a given coordinate (prevents overlapping)
    private boolean hasEntityAt(int x, int y) {
        if (player.getX() == x && player.getY() == y) return true;
        for (int i = 0; i < itemCount; i++) {
            if (items[i].getType() != ' ' && items[i].getX() == x && items[i].getY() == y) return true;
        }
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].isAlive() && robots[i].getX() == x && robots[i].getY() == y) return true;
        }
        return false;
    }

    // Checks whether the player stepped on an item
    // Düzeltilmiş checkItemCollection() metodu:
    private void checkItemCollection() {
        for (int i = 0; i < itemCount; i++) {
            if (items[i].getType() != ' ' && items[i].getX() == player.getX() && items[i].getY() == player.getY()) {

                if (items[i].getType() == '@') {
                    fireballManager.addPacked();
                } else {
                    player.collectSymbol(items[i].getType());
                }

                items[i].erase(); // ← EKLE: Önce ekrandan sil
                items[i] = new Item(-1, -1, ' ', cn); // Sonra dummy item yap
            }
        }
    }

    // Checks whether any robot is adjacent to the player (damage check)
    private void checkNeighborDamage() {
        int px = player.getX();
        int py = player.getY();

        for (int i = 0; i < robotCount; i++) {
            if (robots[i].isAlive()) {
                int rx = robots[i].getX();
                int ry = robots[i].getY();

                // 4-directional adjacency check (no diagonals)
                int diffX = Math.abs(px - rx);
                int diffY = Math.abs(py - ry);

                if ((diffX == 1 && diffY == 0) || (diffX == 0 && diffY == 1)) {
                    player.takeDamage(5); // Project rule: 5 life points damage per 1 time unit
                }
            }
        }
    }

    // Right-side info panel (Heads Up Display)
    private void drawHUD() {
        ConsoleUtils.printString(cn, 50, 15, "Time     :  " + seconds + "  ");
        ConsoleUtils.printString(cn, 50, 16, "Score    :  " + player.getScore() + "  ");
        ConsoleUtils.printString(cn, 50, 17, "Life     :  " + player.getLife() + "  ");
        ConsoleUtils.printString(cn, 50, 18, "Fireball :  " + fireballManager.getPackedCount() + "  ");
        ConsoleUtils.printString(cn, 50, 19, "Storage  :  " + (player.isStorageModeTree() ? "Tree    " : "Backpack"));
    }

    // Shows backpack contents and controls on the tree screen
    private void drawBackpackOnTreeScreen() {
        ConsoleUtils.printString(cn, 2, 24, "Backpack [" + player.getBackpackCount() + "/8]: ");
        char[] bp = player.getBackpack();
        for (int i = 0; i < player.getBackpackCount(); i++) {
            ConsoleUtils.printString(cn, 18 + i * 2, 24, String.valueOf(bp[i]));
        }
        // Show empty slots as dots
        for (int i = player.getBackpackCount(); i < 8; i++) {
            ConsoleUtils.printString(cn, 18 + i * 2, 24, ".");
        }
        ConsoleUtils.printString(cn, 2, 25, "Score: " + player.getScore() + "  Life: " + player.getLife() + "    ");
        ConsoleUtils.printString(cn, 2, 27, "T:Place  R:Take  F:Finish  W/A/D:Move  M:Mode");
    }
}