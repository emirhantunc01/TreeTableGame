import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;
import java.util.Random;
import java.awt.event.KeyEvent;

/**
 * TableScreen.java
 * Handles the Truth Table generation, Postfix evaluation, and Karnaugh Map display.
 * Uses the custom Stack data structure from course materials.
 */
public class TableScreen {
    private Console cn;
    private String postfix;
    private int treeScore;

    // Truth table: 16 rows (0000 - 1111), 5 columns (A, B, C, D, Result)
    private int[][] truthTable = new int[16][5];

    // Random row indices to be hidden from the player (for the Result column)
    private boolean[] isHidden = new boolean[16];

    private Random rnd = new Random();

    // State tracking for game loop
    private boolean initialized = false;
    private boolean completed = false;
    private int currentQuestion = 0;
    private int[] hiddenRows = new int[4]; // Indices of hidden rows
    private int hiddenCount = 0;
    private int correctCount = 0;

    private TextAttributes colorNormal = new TextAttributes(Color.WHITE, Color.BLACK);
    private TextAttributes colorHidden = new TextAttributes(Color.YELLOW, Color.BLACK);
    private TextAttributes colorCorrect = new TextAttributes(Color.GREEN, Color.BLACK);
    private TextAttributes colorWrong = new TextAttributes(Color.RED, Color.BLACK);
    private TextAttributes colorArrow = new TextAttributes(Color.CYAN, Color.BLACK);

    public TableScreen(Console cn, String postfix, int treeScore) {
        this.cn = cn;
        this.postfix = postfix;
        this.treeScore = treeScore;
    }

    // Builds the truth table and draws it to screen (called once by GameEngine)
    public void init() {
        if (initialized) return;

        ConsoleUtils.clearScreen(cn);
        generateTruthTable();

        // Hide 4 random rows
        hiddenCount = 0;
        while (hiddenCount < 4) {
            int r = rnd.nextInt(16);
            if (!isHidden[r]) {
                isHidden[r] = true;
                hiddenRows[hiddenCount] = r;
                hiddenCount++;
            }
        }

        drawTable();
        ConsoleUtils.printString(cn, 2, 22, "Answer with 0 or 1 keys.", colorArrow);
        initialized = true;
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
     * Evaluates the Postfix expression using the Object-type Stack data structure taught in class.
     */
    private int evaluatePostfix(String expr, int valA, int valB, int valC, int valD) {
        // Initialize the Stack class from course materials
        Stack stack = new Stack(50); //

        for (int i = 0; i < expr.length(); i++) {
            char ch = expr.charAt(i);

            if (ch == ' ') continue;

            // Push variables onto the Stack (int values are auto-boxed to Integer, stored as Object)
            if (ch == 'A') stack.push(valA); //
            else if (ch == 'B') stack.push(valB);
            else if (ch == 'C') stack.push(valC);
            else if (ch == 'D') stack.push(valD);
            else if (ch == 'a') stack.push((valA == 0) ? 1 : 0);
            else if (ch == 'b') stack.push((valB == 0) ? 1 : 0);
            else if (ch == 'c') stack.push((valC == 0) ? 1 : 0);
            else if (ch == 'd') stack.push((valD == 0) ? 1 : 0);

                // When an operator is encountered, pop values as (Integer) from the Stack
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
        // Cast the final result as Integer and return it
        return (Integer) stack.pop(); //
    }

    /**
     * Called every game loop cycle. Processes the player's keyboard input.
     * @return true: Table screen is completed (can return to Maze)
     */
    public boolean update(int keypr, Player player) {
        if (!initialized) return false;
        if (completed) return true;

        if (currentQuestion < hiddenCount) {
            // Highlight the current question's row
            int row = hiddenRows[currentQuestion];
            int drawY = 4 + row;
            ConsoleUtils.printString(cn, 7, drawY, ">>", colorArrow);

            // Check the answer when 0 or 1 is pressed
            if (keypr == KeyEvent.VK_0 || keypr == KeyEvent.VK_NUMPAD0 ||
                keypr == KeyEvent.VK_1 || keypr == KeyEvent.VK_NUMPAD1) {

                int answer = (keypr == KeyEvent.VK_1 || keypr == KeyEvent.VK_NUMPAD1) ? 1 : 0;
                int correct = truthTable[row][4];

                // Clear the arrow marker
                ConsoleUtils.printString(cn, 7, drawY, "  ");

                if (answer == correct) {
                    correctCount++;
                    player.addScore(treeScore); // Correct answer: score equal to tree score
                    ConsoleUtils.printString(cn, 9, drawY, String.valueOf(answer), colorCorrect);
                } else {
                    player.addScore(-5); // Wrong answer penalty
                    ConsoleUtils.printString(cn, 9, drawY, String.valueOf(answer), colorWrong);
                    // Show the correct answer as well
                    ConsoleUtils.printString(cn, 11, drawY, "(" + correct + ")", colorNormal);
                }
                currentQuestion++;

                // If all questions answered, draw the Karnaugh map
                if (currentQuestion >= hiddenCount) {
                    drawKarnaughMap();
                    ConsoleUtils.printString(cn, 2, 22, "                                                  "); // Clear old message
                    ConsoleUtils.printString(cn, 2, 22, "Correct: " + correctCount + "/" + hiddenCount +
                        "  |  Tree Score: " + treeScore, colorNormal);
                    ConsoleUtils.printString(cn, 2, 24, "Press ENTER to return to Maze.", colorArrow);
                }
            }
        } else {
            // All questions answered, wait for ENTER
            if (keypr == KeyEvent.VK_ENTER) {
                completed = true;
                return true;
            }
        }
        return false;
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
    // Is the table screen completed?
    public boolean isCompleted() { return completed; }

    // Reset for a new round
    public void reset() {
        initialized = false;
        completed = false;
        currentQuestion = 0;
        correctCount = 0;
        hiddenCount = 0;
        for (int i = 0; i < 16; i++) isHidden[i] = false;
    }
}