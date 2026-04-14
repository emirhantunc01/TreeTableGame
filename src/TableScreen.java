import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;
import java.util.Random;

/**
 * TableScreen.java
 * Handles the Truth Table generation, Postfix evaluation, and Karnaugh Map display.
 * Uses the custom Stack data structure from course materials.
 */
public class TableScreen {
    private Console cn;
    private String postfix;
    private int treeScore;

    // Doğruluk tablosu: 16 satır (0000 - 1111), 5 sütun (A, B, C, D, Result)
    private int[][] truthTable = new int[16][5];

    // Oyuncudan gizlenecek rastgele satır indeksleri (Result sütunu için)
    private boolean[] isHidden = new boolean[16];

    private Random rnd = new Random();

    private TextAttributes colorNormal = new TextAttributes(Color.WHITE, Color.BLACK);
    private TextAttributes colorHidden = new TextAttributes(Color.YELLOW, Color.BLACK);

    public TableScreen(Console cn, String postfix, int treeScore) {
        this.cn = cn;
        this.postfix = postfix;
        this.treeScore = treeScore;
    }

    public void start(Player player) {
        ConsoleUtils.clearScreen(cn);

        generateTruthTable();

        int hiddenCount = 0;
        while (hiddenCount < 4) {
            int r = rnd.nextInt(16);
            if (!isHidden[r]) {
                isHidden[r] = true;
                hiddenCount++;
            }
        }

        drawTable();
        playTableGame(player);
        drawKarnaughMap();

        ConsoleUtils.printString(cn, 2, 26, "Simplified expression: ", colorHidden);
    }

    private void generateTruthTable() {
        for (int i = 0; i < 16; i++) {
            int a = (i / 8) % 2;
            int b = (i / 4) % 2;
            int c = (i / 2) % 2;
            int d = i % 2;

            truthTable[i][0] = a;
            truthTable[i][1] = b;
            truthTable[i][2] = c;
            truthTable[i][3] = d;

            truthTable[i][4] = evaluatePostfix(postfix, a, b, c, d);
        }
    }

    /**
     * Slaytlarda öğretilen Object tipli Stack veri yapısını kullanarak Postfix ifadeyi çözer.
     */
    private int evaluatePostfix(String expr, int valA, int valB, int valC, int valD) {
        // Dersteki Stack sınıfını başlatıyoruz
        Stack stack = new Stack(50); //

        for (int i = 0; i < expr.length(); i++) {
            char ch = expr.charAt(i);

            if (ch == ' ') continue;

            // Değişkenleri Stack'e pushla (int değerler otomatik olarak Integer nesnesine çevrilip Object olarak saklanır)
            if (ch == 'A') stack.push(valA); //
            else if (ch == 'B') stack.push(valB);
            else if (ch == 'C') stack.push(valC);
            else if (ch == 'D') stack.push(valD);
            else if (ch == 'a') stack.push((valA == 0) ? 1 : 0);
            else if (ch == 'b') stack.push((valB == 0) ? 1 : 0);
            else if (ch == 'c') stack.push((valC == 0) ? 1 : 0);
            else if (ch == 'd') stack.push((valD == 0) ? 1 : 0);

                // Operatörler geldiğinde Stack'ten (Integer) olarak Cast edip popla
            else if (ch == '~') {
                int op1 = (Integer) stack.pop(); //
                stack.push((op1 == 0) ? 1 : 0);
            }
            else {
                int op2 = (Integer) stack.pop(); //
                int op1 = (Integer) stack.pop(); //

                if (ch == '^') stack.push((op1 == 1 && op2 == 1) ? 1 : 0);
                else if (ch == 'v') stack.push((op1 == 1 || op2 == 1) ? 1 : 0);
                else if (ch == '+') stack.push((op1 != op2) ? 1 : 0);
                else if (ch == '>') stack.push((op1 == 1 && op2 == 0) ? 0 : 1);
                else if (ch == '=') stack.push((op1 == op2) ? 1 : 0);
            }
        }
        // En son sonucu Integer olarak Cast edip dönüyoruz
        return (Integer) stack.pop(); //
    }

    private void playTableGame(Player player) {
        for (int i = 0; i < 16; i++) {
            if (isHidden[i]) {
                int drawY = 4 + i;
                cn.getTextWindow().setCursorPosition(25, drawY);
                // TODO: Klavyeden cevap okuma mantığı eklenecek
            }
        }
    }

    public void drawTable() {
        ConsoleUtils.printString(cn, 2, 2, "ABCD | Result");
        ConsoleUtils.printString(cn, 2, 3, "-----+-------");

        for (int i = 0; i < 16; i++) {
            String abcd = "" + truthTable[i][0] + truthTable[i][1] + truthTable[i][2] + truthTable[i][3];
            ConsoleUtils.printString(cn, 2, 4 + i, abcd + " | ");

            if (isHidden[i]) {
                ConsoleUtils.printString(cn, 9, 4 + i, "?", colorHidden);
            } else {
                ConsoleUtils.printString(cn, 9, 4 + i, String.valueOf(truthTable[i][4]), colorNormal);
            }
        }
    }

    public void drawKarnaughMap() {
        int[] rowGray = {0, 1, 3, 2};
        int[] colGray = {0, 1, 3, 2};

        int startX = 30;
        int startY = 4;

        ConsoleUtils.printString(cn, startX, startY, "   CD");
        ConsoleUtils.printString(cn, startX, startY+1, "AB    00  01  11  10");
        ConsoleUtils.printString(cn, startX, startY+2, "    +---+---+---+---+");

        for (int r = 0; r < 4; r++) {
            int ab = rowGray[r];
            String abStr = (ab < 2) ? "0"+ab : ((ab==3)?"11":"10");
            ConsoleUtils.printString(cn, startX, startY+3+r*2, " " + abStr + " |");

            for (int c = 0; c < 4; c++) {
                int cd = colGray[c];
                int index = ab * 4 + cd;
                int result = truthTable[index][4];

                ConsoleUtils.printString(cn, startX + 6 + c*4, startY+3+r*2, String.valueOf(result));
                ConsoleUtils.printString(cn, startX + 8 + c*4, startY+3+r*2, "|");
            }
            ConsoleUtils.printString(cn, startX, startY+4+r*2, "    +---+---+---+---+");
        }
    }
}