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

    // --- Oyun Ekranları ve Bileşenleri ---
    private Maze maze;
    private Player player;
    private InputQueue inputQueue;
    private ExpressionTree treeScreen;
    private Fireball fireballManager;

    // --- Oyun İçi Varlıklar (Standart Diziler) ---
    private Item[] items = new Item[100];
    private int itemCount = 0;

    private Robot[] robots = new Robot[50];
    private int robotCount = 0;

    // --- Zamanlama ve Durum Yönetimi ---
    private int timeUnit = 0; // Her döngüde 1 artar (1 birim = 100ms)
    private int seconds = 0;
    private int currentScreen = 1; // 1: Maze, 2: Tree, 3: Table
    private boolean isGameOver = false;

    // Klavye girdisi için
    private int keypr = 0;
    private Random rnd = new Random();

    public GameEngine() throws Exception {
        cn = Enigma.getConsole("Tree & Table", 100, 30, 20);

        // Klavye dinleyicisini ayarla
        cn.getTextWindow().addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) { keypr = e.getKeyCode(); }
            public void keyReleased(KeyEvent e) {}
            public void keyTyped(KeyEvent e) {}
        });

        // Bileşenleri Başlat
        maze = Maze.loadFromFile("maze.txt");
        player = new Player(cn, maze);
        inputQueue = new InputQueue(cn);
        treeScreen = new ExpressionTree(cn);
        fireballManager = new Fireball(cn);

        // Kural: Oyun başında kuyruktaki ilk 10 eleman labirente eklenir
        for (int i = 0; i < 10; i++) {
            insertElementFromQueue();
        }

        // Ana oyun döngüsünü başlat
        run();
    }

    private void run() throws InterruptedException {
        while (!isGameOver) {

            // Ekran değiştirme tuşları (1, 2, 3)
            if (keypr == KeyEvent.VK_1) { currentScreen = 1; ConsoleUtils.clearScreen(cn); keypr = 0; }
            if (keypr == KeyEvent.VK_2) { currentScreen = 2; ConsoleUtils.clearScreen(cn); keypr = 0; }
            if (keypr == KeyEvent.VK_3) { currentScreen = 3; ConsoleUtils.clearScreen(cn); keypr = 0; }

            if (currentScreen == 1) {
                updateMazeScreen();
                Thread.sleep(100); // 1 Time Unit = 100 ms
            }
            else if (currentScreen == 2) {
                updateTreeScreen();
                Thread.sleep(100); // Tree ekranında zaman akmaz ama girdi bekleriz
            }
            else if (currentScreen == 3) {
                updateTableScreen();
                Thread.sleep(100);
            }
        }

        // Oyun Bitti Ekranı (High Score Table çağrılacak)
        ConsoleUtils.clearScreen(cn);
        ConsoleUtils.printString(cn, 40, 15, "GAME OVER!");
        ConsoleUtils.printString(cn, 35, 17, "Final Score: " + player.getScore());
        // Oyun Bitti Ekranı
        ConsoleUtils.clearScreen(cn);
        ConsoleUtils.printString(cn, 40, 10, "GAME OVER!");
        ConsoleUtils.printString(cn, 38, 12, "Final Score: " + player.getScore());

        // --- High Score Table Entegrasyonu ---
        DoublyLinkedList highScoreTable = new DoublyLinkedList();
        highScoreTable.loadFromFile("highscore.txt");

        // Oyuncunun skorunu listeye ekle (İsmini şimdilik Player1 yapıyoruz, istersen konsoldan Scanner ile isim isteyebilirsin)
        highScoreTable.insert("Player1", player.getScore());

        // Tabloyu ekrana çiz
        highScoreTable.display(cn, 35, 15);
    }

    // ==========================================
    // MAZE SCREEN (LABİRENT EKRANI) MANTIĞI
    // ==========================================
    private void updateMazeScreen() {
        timeUnit++;
        if (timeUnit % 10 == 0) seconds++; // Her 10 birim 1 saniye (1000ms)

        // 1. Çizim (Sadece bir kere çizilip sonra güncellenir)
        if (timeUnit == 1) {
            maze.draw(cn);
            for (int i = 0; i < itemCount; i++) items[i].draw();
            for (int i = 0; i < robotCount; i++) robots[i].draw();
        }

        // 2. Oyuncu Hareketleri ve Girdileri (Her 1 time unit)
        if (keypr != 0) {
            if (keypr == KeyEvent.VK_SPACE) {
                // Ateş Topu fırlat
                fireballManager.fire(player.getX(), player.getY(), player.getLastDx(), player.getLastDy());
            } else if (keypr == KeyEvent.VK_M) {
                // Çanta Modu Değiştir
                player.toggleStorageMode();
            } else {
                // Yön Tuşları
                player.move(keypr);
                checkItemCollection();
            }
            keypr = 0;
        }
        player.draw(); // Oyuncuyu hep en üstte tut

        // 3. Ateş Topu Hareketi (Her 1 time unit)
        fireballManager.update(robots, robotCount, player);

        // 4. Robot Hareketleri (Her 4 time unit)
        if (timeUnit % 4 == 0) {
            for (int i = 0; i < robotCount; i++) {
                robots[i].move(items, itemCount, robots, robotCount);
            }
        }

        // 5. Yeni Girdi Ekleme (Her 2 saniyede bir = 20 time unit)
        if (timeUnit % 20 == 0) {
            insertElementFromQueue();
        }

        // 6. Yakın Dövüş Hasarı (Neighbor Harming - Her 1 time unit)
        checkNeighborDamage();

        // 7. Arayüzü (HUD) ve Girdi Kuyruğunu Çiz
        drawHUD();
        inputQueue.draw(50, 2);

        // Ölüm Kontrolü
        if (player.getLife() <= 0) {
            isGameOver = true;
        }
    }

    // ==========================================
    // TREE SCREEN (AĞAÇ EKRANI) MANTIĞI
    // ==========================================
    private void updateTreeScreen() {
        treeScreen.draw();

        if (keypr != 0) {
            if (keypr == KeyEvent.VK_W || keypr == KeyEvent.VK_A || keypr == KeyEvent.VK_D) {
                if (treeScreen.moveCursor((char)keypr)) player.addScore(-1); // Ceza puanı
            }
            else if (keypr == KeyEvent.VK_T) {
                // TODO: Çantadan (Backpack) alıp ağaca koyma
            }
            else if (keypr == KeyEvent.VK_R) {
                // Ağaçtan alıp çantaya koyma
                char c = treeScreen.takeSymbol();
                if (c != ' ') {
                    player.addScore(-2); // Ceza puanı
                    // TODO: Player'ın çantasına geri ekle
                }
            }
            else if (keypr == KeyEvent.VK_F) {
                if (treeScreen.finishTree()) {
                    player.addScore(treeScreen.calculateTreeScore());
                    currentScreen = 3; // Table Screen'e geç
                    ConsoleUtils.clearScreen(cn);
                } else {
                    player.addScore(-10); // Hatalı ağaç cezası
                }
            }
            keypr = 0;
        }
    }

    // ==========================================
    // TABLE SCREEN (TABLO EKRANI) MANTIĞI
    // ==========================================
    private void updateTableScreen() {
        ConsoleUtils.printString(cn, 5, 5, "Truth Table and Karnaugh Map Screen (In Progress...)");
        // TODO: Doğruluk tablosu ve Karnaugh haritası mantığı buraya gelecek.
    }

    // ==========================================
    // YARDIMCI METOTLAR
    // ==========================================

    // Kuyruktan eleman çekip labirentte rastgele boş bir yere koyar
    private void insertElementFromQueue() {
        Character dequeued = inputQueue.dequeue();
        if (dequeued == null) return; // Kuyruk boşsa işlem yapma
        char type = dequeued;

        int rx, ry;
        do {
            rx = rnd.nextInt(Maze.COLS - 2) + 1;
            ry = rnd.nextInt(Maze.ROWS - 2) + 1;
        } while (Maze.map[ry][rx] == '#' || hasEntityAt(rx, ry)); // Duvar veya başka obje var mı?

        if (type == 'X') {
            if (robotCount < robots.length) {
                robots[robotCount] = new Robot(cn, rx, ry);
                robotCount++;
            }
        } else {
            if (itemCount < items.length) {
                items[itemCount] = new Item(rx, ry, type, cn);
                itemCount++;
            }
        }
    }

    // Bir koordinatta obje olup olmadığını kontrol eder (üst üste binmeyi engeller)
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

    // Oyuncu bir eşyanın üzerine geldi mi?
    private void checkItemCollection() {
        for (int i = 0; i < itemCount; i++) {
            if (items[i].getType() != ' ' && items[i].getX() == player.getX() && items[i].getY() == player.getY()) {

                if (items[i].getType() == '@') {
                    fireballManager.addPacked();
                } else {
                    player.collectSymbol(items[i].getType());
                }

                items[i] = new Item(-1, -1, ' ', cn); // Toplanan eşyayı oyundan sil (Dummy item yap)
            }
        }
    }

    // Robotlar oyuncuya temas ediyor mu? (Hasar kontrolü)
    private void checkNeighborDamage() {
        int px = player.getX();
        int py = player.getY();

        for (int i = 0; i < robotCount; i++) {
            if (robots[i].isAlive()) {
                int rx = robots[i].getX();
                int ry = robots[i].getY();

                // 4 yönlü komşuluk kontrolü (Çapraz hariç)
                int diffX = Math.abs(px - rx);
                int diffY = Math.abs(py - ry);

                if ((diffX == 1 && diffY == 0) || (diffX == 0 && diffY == 1)) {
                    player.takeDamage(5); // Proje kuralı: 1 time unit'te 5 life points hasar
                }
            }
        }
    }

    // Sağ alttaki bilgi ekranı (Heads Up Display)
    private void drawHUD() {
        ConsoleUtils.printString(cn, 50, 15, "Time     :  " + seconds + "  ");
        ConsoleUtils.printString(cn, 50, 16, "Score    :  " + player.getScore() + "  ");
        ConsoleUtils.printString(cn, 50, 17, "Life     :  " + player.getLife() + "  ");
        ConsoleUtils.printString(cn, 50, 18, "Fireball :  " + fireballManager.getPackedCount() + "  ");
        ConsoleUtils.printString(cn, 50, 19, "Storage  :  " + (player.isStorageModeTree() ? "Tree    " : "Backpack"));
    }
}