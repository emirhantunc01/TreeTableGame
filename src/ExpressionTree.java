import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;

/**
 * ExpressionTree.java
 * Manages the Tree Screen where players form logic expressions.
 * Uses a 1-based array to represent a perfectly balanced binary tree up to depth 5 (31 nodes).
 */
public class ExpressionTree {
    // 1-based array for the binary tree (index 0 is unused)
    private char[] tree = new char[32];

    // Ağaç imleci (Tree cursor) başlangıçta kökte (1)
    private int cursor = 1;

    private Console cn;

    // Renkler
    private TextAttributes colorDefault = new TextAttributes(Color.WHITE, Color.BLACK);
    private TextAttributes colorCursor = new TextAttributes(Color.GREEN, Color.BLACK);

    // Ekrana çizim için her indeksin X ve Y koordinatları (1-31 arası)
    // Bu değerleri konsol ekranına göre görsel olarak ayarlayabilirsin.
    private int[] drawX = {0,
            40, // 1 (Root)
            20, 60, // 2, 3
            10, 30, 50, 70, // 4, 5, 6, 7
            5, 15, 25, 35, 45, 55, 65, 75, // 8-15
            3, 7, 13, 17, 23, 27, 33, 37, 43, 47, 53, 57, 63, 67, 73, 77 // 16-31
    };
    private int[] drawY = {0,
            2, // 1 (Root)
            5, 5, // 2, 3
            8, 8, 8, 8, // 4, 5, 6, 7
            11, 11, 11, 11, 11, 11, 11, 11, // 8-15
            14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14 // 16-31
    };

    public ExpressionTree(Console cn) {
        this.cn = cn;
        // Ağacı boşluk karakteriyle doldur
        for (int i = 1; i <= 31; i++) {
            tree[i] = ' ';
        }
    }

    // --- İMLEÇ HAREKETLERİ (Cursor Movements) ---

    // W: Parent, A: Left child, D: Right child. (Player'dan -1 puan düşülecek)
    public boolean moveCursor(char key) {
        int nextCursor = cursor;

        if ((key == 'w' || key == 'W') && cursor > 1) {
            nextCursor = cursor / 2; // Parent
        } else if ((key == 'a' || key == 'A') && (cursor * 2) <= 31) {
            nextCursor = cursor * 2; // Left child
        } else if ((key == 'd' || key == 'D') && (cursor * 2 + 1) <= 31) {
            nextCursor = cursor * 2 + 1; // Right child
        }

        if (nextCursor != cursor) {
            cursor = nextCursor;
            return true; // Hareket başarılı (-1 point)
        }
        return false;
    }

    // Sembol yerleştirildiğinde imleci otomatik olarak sıradaki boş slota taşır
    public void autoMoveCursor() {
        for (int i = 1; i <= 31; i++) {
            if (tree[i] == ' ') {
                cursor = i;
                break;
            }
        }
    }

    // --- EŞYA YERLEŞTİRME VE ALMA ---

    public boolean placeSymbol(char symbol) {
        if (tree[cursor] == ' ') {
            tree[cursor] = symbol;
            autoMoveCursor();
            return true;
        }
        return false; // Slot dolu
    }

    public char takeSymbol() {
        char symbol = tree[cursor];
        if (symbol != ' ') {
            tree[cursor] = ' ';
            return symbol; // Başarılı, eşya çantaya dönecek (-2 point)
        }
        return ' '; // Slot zaten boş
    }

    // --- AĞAÇ DOĞRULAMA (Validation) ---

    // F tuşuna basıldığında çağrılır. 
    // Hatalıysa false (-10 point), doğruysa true (Table Screen'e geçer)
    public boolean finishTree() {
        int varCount = 0;
        int maxIndex = 0;

        for (int i = 1; i <= 31; i++) {
            char c = tree[i];
            if (c != ' ') {
                maxIndex = i;
                if (c == 'A' || c == 'B' || c == 'C' || c == 'D' ||
                        c == 'a' || c == 'b' || c == 'c' || c == 'd') {
                    varCount++;
                }
            }
        }

        // Kural 1: Minimum 3 variables
        if (varCount < 3) return false;

        // Kural 2: Minimum depth is 3. 
        // Depth 3 means we must have at least one node in indices 4 to 7.
        if (maxIndex < 4) return false;

        return true;
    }

    // Ağacın puanını hesaplar (Düğüm sayısı * 10)
    public int calculateTreeScore() {
        int count = 0;
        for (int i = 1; i <= 31; i++) {
            if (tree[i] != ' ') count++;
        }
        return count * 10;
    }

    // --- INFIX VE POSTFIX YAZDIRMA ---

    public String getInfix() {
        return buildInfix(1);
    }

    private String buildInfix(int index) {
        if (index > 31 || tree[index] == ' ') return "";

        boolean isLeaf = (index * 2 > 31 || (tree[index * 2] == ' ' && tree[index * 2 + 1] == ' '));

        if (isLeaf) {
            return String.valueOf(tree[index]);
        } else {
            String left = buildInfix(index * 2);
            String right = buildInfix(index * 2 + 1);
            return "(" + left + " " + tree[index] + " " + right + ")";
        }
    }

    public String getPostfix() {
        return buildPostfix(1);
    }

    private String buildPostfix(int index) {
        if (index > 31 || tree[index] == ' ') return "";

        String left = buildPostfix(index * 2);
        String right = buildPostfix(index * 2 + 1);

        String result = "";
        if (!left.isEmpty()) result += left + " ";
        if (!right.isEmpty()) result += right + " ";
        result += tree[index];

        return result;
    }

    // --- EKRANA ÇİZİM ---

    public void draw() {
        // Ekranı temizleme işlemi GameEngine tarafında yapılmalı

        for (int i = 1; i <= 31; i++) {
            char symbol = (tree[i] == ' ') ? '.' : tree[i]; // Boş yerleri nokta ile göster

            // Eğer imleç bu indeksin üzerindeyse yeşil, değilse beyaz çiz
            TextAttributes attr = (i == cursor) ? colorCursor : colorDefault;

            cn.getTextWindow().output(drawX[i], drawY[i], symbol, attr);

            // Opsiyonel: Ağaç dallarını (/, \) çizmek istersen drawX ve drawY 
            // aralarına statik karakterler basabilirsin.
        }

        // Infix ve Postfix metinlerini aşağıya yazdır
        ConsoleUtils.printString(cn, 2, 20, "Infix   : " + getInfix());
        ConsoleUtils.printString(cn, 2, 21, "Postfix : " + getPostfix());
    }
}