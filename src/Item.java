import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;

/**
 * Item.java
 * Represents collectible logic symbols and packed fireballs on the map.
 */
public class Item {
    private int x, y;
    private char type; // 'A', 'B', 'a', '~', '^', 'v', '+', '>', '=', '@'
    private Console cn;

    private static TextAttributes colorSymbol = new TextAttributes(Color.YELLOW, Color.BLACK);
    private static TextAttributes colorFireball = new TextAttributes(Color.CYAN, Color.BLACK);

    public Item(int x, int y, char type, Console cn) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.cn = cn;
    }

    public void draw() {
        if (type == '@') {
            // Paketlenmiş Ateş Topu (Packed Fireball)
            cn.getTextWindow().output(x, y, type, colorFireball);
        } else {
            // Mantık Sembolleri (Logic Symbols)
            cn.getTextWindow().output(x, y, type, colorSymbol);
        }
    }

    public void erase() {
        cn.getTextWindow().output(x, y, ' ');
    }

    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public char getType() { return type; }
}