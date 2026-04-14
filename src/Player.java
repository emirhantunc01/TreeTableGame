import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;
import java.awt.event.KeyEvent;

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

    // Oyuncunun en son baktığı yön (Ateş topu fırlatmak için)
    // Başlangıçta sağa (dx=1, dy=0) bakıyor olarak varsayalım
    private int lastDx = 1;
    private int lastDy = 0;

    // Depolama Modu: true ise Tree (Ağaç), false ise Backpack (Çanta)
    private boolean storageModeTree = true;

    // Çanta için standart dizi kullanımı (Maksimum 8 eşya)
    private char[] backpack = new char[8];
    private int backpackCount = 0;

    private Console cn;
    private Maze maze;

    private TextAttributes colorPlayer = new TextAttributes(Color.GREEN, Color.BLACK);

    public Player(Console cn, Maze maze) {
        this.cn = cn;
        this.maze = maze;

        // Başlangıçta rastgele boş bir koordinat bul
        x = 5;
        y = 5;
        while (y < Maze.ROWS - 1 && Maze.map[y][x] == '#') {
            x++;
            if (x >= Maze.COLS - 1) {
                x = 1;
                y++;
            }
        }
    }

    public void move(int key) {
        int dx = 0;
        int dy = 0;

        if (key == KeyEvent.VK_LEFT) dx = -1;
        else if (key == KeyEvent.VK_RIGHT) dx = 1;
        else if (key == KeyEvent.VK_UP) dy = -1;
        else if (key == KeyEvent.VK_DOWN) dy = 1;

        // Geçersiz bir tuşa basıldıysa çık
        if (dx == 0 && dy == 0) return;

        // Oyuncunun son hareket yönünü GÜNCELLE
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

    // 'M' tuşuna basıldığında modu değiştirir
    public void toggleStorageMode() {
        storageModeTree = !storageModeTree;
    }

    // Toplanan eşyayı çantaya veya ağaca ekleme mantığı
    public boolean collectSymbol(char symbol) {
        if (storageModeTree) {
            // TODO: Ağaç (Tree) sınıfına ekleme yapılacak
            return true;
        } else {
            if (backpackCount < backpack.length) {
                backpack[backpackCount] = symbol;
                backpackCount++;
                return true;
            } else {
                // Proje kuralı: Çanta doluysa otomatik olarak Tree'ye yerleştirilir
                // TODO: Ağaç (Tree) sınıfına ekleme yapılacak
                return true;
            }
        }
    }

    // Getters & Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getLife() { return life; }
    public int getScore() { return score; }
    public boolean isStorageModeTree() { return storageModeTree; }

    // Ateş topu sınıfından çağrılacak yön metotları
    public int getLastDx() { return lastDx; }
    public int getLastDy() { return lastDy; }

    public void addScore(int points) { score += points; }
    public void takeDamage(int amount) { life -= amount; }
}