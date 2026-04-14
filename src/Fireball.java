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

    // Aktif ateş toplarını tutacağımız standart diziler
    private int[] fbX = new int[MAX_FIREBALLS];
    private int[] fbY = new int[MAX_FIREBALLS];
    private int[] fbDx = new int[MAX_FIREBALLS];
    private int[] fbDy = new int[MAX_FIREBALLS];
    private boolean[] active = new boolean[MAX_FIREBALLS];

    private int packedCount = 0; // Oyuncunun topladığı cephane
    private Console cn;
    private TextAttributes colorFireball = new TextAttributes(Color.RED, Color.BLACK);

    public Fireball(Console cn) {
        this.cn = cn;
    }

    public void addPacked() { packedCount++; }
    public int getPackedCount() { return packedCount; }

    // Oyuncu ateş ettiğinde çağrılacak metot
    public boolean fire(int startX, int startY, int dx, int dy) {
        if (packedCount <= 0 || (dx == 0 && dy == 0)) return false;

        // Boş bir ateş topu slotu bul
        for (int i = 0; i < MAX_FIREBALLS; i++) {
            if (!active[i]) {
                fbX[i] = startX + dx;
                fbY[i] = startY + dy;
                fbDx[i] = dx;
                fbDy[i] = dy;
                active[i] = true;
                packedCount--; // Cephaneyi düş
                return true;
            }
        }
        return false;
    }

    // Oyun döngüsünde (GameEngine) her time unit'te çağrılacak metot
    public void update(Robot[] robots, int robotCount, Player p) {
        for (int i = 0; i < MAX_FIREBALLS; i++) {
            if (active[i]) {
                // Eski pozisyonu sil
                cn.getTextWindow().output(fbX[i], fbY[i], ' ');

                // İlerlet
                fbX[i] += fbDx[i];
                fbY[i] += fbDy[i];

                // Duvarlara veya sınırları çarpma kontrolü (Nesne içinden geçemez)
                if (fbX[i] < 0 || fbX[i] >= Maze.COLS || fbY[i] < 0 || fbY[i] >= Maze.ROWS || Maze.map[fbY[i]][fbX[i]] == '#') {
                    active[i] = false;
                    continue;
                }

                // Robotlarla çarpışma kontrolü
                for (int j = 0; j < robotCount; j++) {
                    if (robots[j].isAlive() && robots[j].getX() == fbX[i] && robots[j].getY() == fbY[i]) {
                        robots[j].die();
                        p.addScore(50); // Oyuncuya 50 puan ekle
                    }
                }
                // Not: Proje dokümanı "One fireball can destroy many robots" dediği için
                // robotu vurduktan sonra active[i] = false YAPMIYORUZ. İlerlemeye devam ediyor.

                // Yeni pozisyonu çiz
                if (active[i]) {
                    cn.getTextWindow().output(fbX[i], fbY[i], 'o', colorFireball);
                }
            }
        }
    }
}