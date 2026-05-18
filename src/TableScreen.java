import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;
import java.util.Random;
import java.awt.event.KeyEvent;

/**
 * TableScreen.java
 * Handles the Truth Table generation with ALL sub-expressions, postfix evaluation, and Karnaugh Map display.
 */
public class TableScreen {
    private Console cn;
    private String postfix;
    private int treeScore;
    private String[] allSubexpressions; // All postfix sub-expressions from tree

    // Truth table: 16 rows (0000 - 1111), columns for (A, B, C, D + all sub-expressions)
    // Let's limit to reasonable number: ABCD(4) + max 8 sub-expression columns
    private int[][] truthTable = new int[16][12]; // 4 variables + up to 8 expressions
    private int expressionCount = 0; // How many sub-expression columns we have

    // Which rows and which columns are hidden
    private boolean[][] isHidden = new boolean[16][12];
    private int[] hiddenRows = new int[4];
    private int hiddenCount = 0;

    private Random rnd = new Random();

    // State tracking for game loop
    private boolean initialized = false;
    private boolean completed = false;
    private int currentQuestion = 0; // Which hidden cell we're on
    private int correctCount = 0;
    private int totalQuestions = 0; // Total hidden cells to fill

    private TextAttributes colorNormal = new TextAttributes(Color.WHITE, Color.BLACK);
    private TextAttributes colorHidden = new TextAttributes(Color.YELLOW, Color.BLACK);
    private TextAttributes colorCorrect = new TextAttributes(Color.GREEN, Color.BLACK);
    private TextAttributes colorWrong = new TextAttributes(Color.RED, Color.BLACK);
    private TextAttributes colorArrow = new TextAttributes(Color.CYAN, Color.BLACK);

    public TableScreen(Console cn, String postfix, int treeScore, String[] allSubexpressions) {
        this.cn = cn;
        this.postfix = postfix;
        this.treeScore = treeScore;
        this.allSubexpressions = allSubexpressions;
    }

    // Builds the truth table and draws it to screen (called once by GameEngine)
    public void init() {
        if (initialized) return;

        ConsoleUtils.clearScreen(cn);

        // Extract non-empty sub-expressions from allSubexpressions
        for (int i = 1; i <= 31; i++) {
            if (allSubexpressions[i] != null && !allSubexpressions[i].isEmpty()) {
                if (expressionCount < 8) { // Limit to 8 sub-expressions for display
                    expressionCount++;
                }
            }
        }

        generateTruthTable();

        // Hide 4 random rows (for player to fill ALL columns of those rows)
        hiddenCount = 0;
        totalQuestions = 0;
        while (hiddenCount < 4) {
            int r = rnd.nextInt(16);
            boolean alreadyHidden = false;
            for (int h = 0; h < hiddenCount; h++) {
                if (hiddenRows[h] == r) {
                    alreadyHidden = true;
                    break;
                }
            }
            if (!alreadyHidden) {
                hiddenRows[hiddenCount] = r;
                // Mark all expression columns as hidden for this row
                for (int col = 4; col < 4 + expressionCount; col++) {
                    isHidden[r][col] = true;
                    totalQuestions++;
                }
                hiddenCount++;
            }
        }

        drawTable();
        ConsoleUtils.printString(cn, 2, 25, "Fill the YELLOW cells. Answer with 0 or 1 keys.", colorArrow);
        initialized = true;
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
                if (stack.isEmpty()) return 0; // Error: not enough operands
                int op1 = (Integer) stack.pop(); //
                stack.push((op1 == 0) ? 1 : 0);
            }
            else {
                if (stack.isEmpty()) return 0; // Error: not enough operands
                int op2 = (Integer) stack.pop(); //
                if (stack.isEmpty()) return 0; // Error: not enough operands
                int op1 = (Integer) stack.pop(); //

                if (ch == '^') stack.push((op1 == 1 && op2 == 1) ? 1 : 0);
                else if (ch == 'v') stack.push((op1 == 1 || op2 == 1) ? 1 : 0);
                else if (ch == '+') stack.push((op1 != op2) ? 1 : 0);
                else if (ch == '>') stack.push((op1 == 1 && op2 == 0) ? 0 : 1);
                else if (ch == '=') stack.push((op1 == op2) ? 1 : 0);
            }
        }
        // Cast the final result as Integer and return it
        if (stack.isEmpty()) return 0; // Error: no result
        return (Integer) stack.pop(); //
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

            // Evaluate all sub-expressions for this row
            int exprIdx = 0;
            for (int j = 1; j <= 31 && exprIdx < expressionCount; j++) {
                if (allSubexpressions[j] != null && !allSubexpressions[j].isEmpty()) {
                    truthTable[i][4 + exprIdx] = evaluatePostfix(allSubexpressions[j], a, b, c, d);
                    exprIdx++;
                }
            }
        }
    }

    /**
     * Called every game loop cycle. Processes the player's keyboard input.
     * @return true: Table screen is completed (can return to Maze)
     */
    public boolean update(int keypr, Player player) {
        if (!initialized) return false;
        if (completed) return true;

        if (keypr == KeyEvent.VK_K) {
            // Show Karnaugh map
            drawKarnaughMap();
            ConsoleUtils.printString(cn, 2, 24, "                                                  ");
            ConsoleUtils.printString(cn, 2, 24, "Karnaugh Map. Correct: " + correctCount + "/" + totalQuestions, colorNormal);
            ConsoleUtils.printString(cn, 2, 25, "Press ENTER to return to Maze.", colorArrow);
            return false;
        }

        // Find next hidden cell to fill
        if (currentQuestion < totalQuestions) {
            // Iterate through all rows and columns to find the currentQuestion-th hidden cell
            int cellsFound = 0;
            for (int r = 0; r < 16 && cellsFound <= currentQuestion; r++) {
                for (int c = 4; c < 4 + expressionCount && cellsFound <= currentQuestion; c++) {
                    if (isHidden[r][c]) {
                        if (cellsFound == currentQuestion) {
                            // This is the cell to answer
                            int drawY = 4 + r;
                            int colWidth = 7;
                            int drawX = 8 + (c - 4) * colWidth;

                            // Highlight with arrow
                            ConsoleUtils.printString(cn, drawX, drawY, ">>", colorArrow);

                            // Check answer when 0 or 1 pressed
                            if (keypr == KeyEvent.VK_0 || keypr == KeyEvent.VK_NUMPAD0 ||
                                keypr == KeyEvent.VK_1 || keypr == KeyEvent.VK_NUMPAD1) {

                                int answer = (keypr == KeyEvent.VK_1 || keypr == KeyEvent.VK_NUMPAD1) ? 1 : 0;
                                int correct = truthTable[r][c];

                                // Clear arrow
                                ConsoleUtils.printString(cn, drawX, drawY, "  ");

                                if (answer == correct) {
                                    correctCount++;
                                    player.addScore(3);
                                    ConsoleUtils.printString(cn, drawX, drawY, String.valueOf(answer), colorCorrect);
                                } else {
                                    player.addScore(-2);
                                    ConsoleUtils.printString(cn, drawX, drawY, String.valueOf(answer), colorWrong);
                                    ConsoleUtils.printString(cn, drawX + 3, drawY, "(" + correct + ")", colorNormal);
                                }

                                isHidden[r][c] = false;
                                currentQuestion++;

                                if (currentQuestion >= totalQuestions) {
                                    drawTable(); // Redraw to show all filled cells
                                    ConsoleUtils.printString(cn, 2, 24, "                                                  ");
                                    ConsoleUtils.printString(cn, 2, 24, "All done! Correct: " + correctCount + "/" + totalQuestions +
                                        "  |  Tree: " + treeScore, colorNormal);
                                    ConsoleUtils.printString(cn, 2, 25, "Press K for Karnaugh Map or ENTER to return to Maze.", colorArrow);
                                }
                            }
                            return false;
                        }
                        cellsFound++;
                    }
                }
            }
        } else {
            // All answered, wait for input
            if (keypr == KeyEvent.VK_ENTER) {
                completed = true;
                return true;
            }
        }
        return false;
    }

    public void drawTable() {
        // Draw header: ABCD | expr1 | expr2 | ...
        String header = "ABCD |";
        int colX = 8; // Start position for expressions

        int exprIdx = 0;
        for (int j = 1; j <= 31 && exprIdx < expressionCount; j++) {
            if (allSubexpressions[j] != null && !allSubexpressions[j].isEmpty()) {
                String expr = allSubexpressions[j];
                // Keep expression short  (max 6 chars)
                if (expr.length() > 6) expr = expr.substring(0, 6);
                header = header + expr + "|";
                exprIdx++;
            }
        }
        ConsoleUtils.printString(cn, 2, 2, header);

        // Separator line
        String sep = "-----+";
        for (int i = 0; i < expressionCount; i++) {
            sep += "------+";
        }
        ConsoleUtils.printString(cn, 2, 3, sep);

        // Draw rows with values
        for (int i = 0; i < 16; i++) {
            String abcd = "" + truthTable[i][0] + truthTable[i][1] + truthTable[i][2] + truthTable[i][3];
            String row = abcd + " | ";

            for (int col = 4; col < 4 + expressionCount; col++) {
                if (isHidden[i][col]) {
                    row += "?  |";
                } else {
                    row += truthTable[i][col] + "  |";
                }
            }
            ConsoleUtils.printString(cn, 2, 4 + i, row);
        }
    }

    /**
     * Converts Gray code to Binary (2-bit)
     * Gray: 0->00(0), 1->01(1), 3->11(2), 2->10(3)
     */
    private int grayToBinary(int gray) {
        return gray ^ (gray >> 1);
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
            int abGray = rowGray[r];
            int abBinary = grayToBinary(abGray);
            int abA = (abBinary >> 1) & 1;
            int abB = abBinary & 1;

            String abStr = "" + abA + abB;
            ConsoleUtils.printString(cn, startX, startY+3+r*2, " " + abStr + " |");

            for (int c = 0; c < 4; c++) {
                int cdGray = colGray[c];
                int cdBinary = grayToBinary(cdGray);
                int cdC = (cdBinary >> 1) & 1;
                int cdD = cdBinary & 1;

                // Calculate truth table index: ABCD in binary
                int index = abA * 8 + abB * 4 + cdC * 2 + cdD;
                // Get the last (result) column
                int result = truthTable[index][4 + expressionCount - 1];

                ConsoleUtils.printString(cn, startX + 5 + c*4, startY+3+r*2, " " + result + " |");
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
        totalQuestions = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 12; j++) {
                isHidden[i][j] = false;
            }
        }
    }
}
