import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
 * TableScreen.java
 * Handles truth-table generation, question slots, and Karnaugh Map display.
 */
public class TableScreen {
    private static final int ROW_COUNT = 16;
    private static final int VARIABLE_COLS = 4;
    private static final int MAX_EXPRESSION_COLUMNS = 8;
    private static final int TOTAL_COLS = VARIABLE_COLS + MAX_EXPRESSION_COLUMNS;

    private static final int TABLE_LEFT = 1;
    private static final int TABLE_TOP = 7;
    private static final int CONTENT_LEFT = TABLE_LEFT + 2;
    private static final int STATUS_Y = 6;
    private static final int MAX_CONTENT_WIDTH = 94;

    private Console cn;
    private String postfix;
    private int treeScore;
    private String[] allSubexpressions;
    private String[] allExpressionHeaders;

    private int[][] truthTable = new int[ROW_COUNT][TOTAL_COLS];
    private int expressionCount = 0;
    private String[] expressionPostfixes = new String[MAX_EXPRESSION_COLUMNS];
    private String[] expressionHeaders = new String[MAX_EXPRESSION_COLUMNS];

    private boolean[][] isHidden = new boolean[ROW_COUNT][TOTAL_COLS];
    private int[] hiddenQuestionRows = new int[MAX_EXPRESSION_COLUMNS];

    private int[] columnWidths = new int[1 + MAX_EXPRESSION_COLUMNS];
    private int[] expressionOffsets = new int[MAX_EXPRESSION_COLUMNS];
    private int tableContentWidth = 0;

    private Random rnd = new Random();

    private boolean initialized = false;
    private boolean completed = false;
    private int currentQuestion = 0;
    private int correctCount = 0;
    private int totalQuestions = 0;

    private TextAttributes colorNormal = new TextAttributes(Color.WHITE, Color.BLACK);
    private TextAttributes colorHidden = new TextAttributes(Color.YELLOW, Color.BLACK);
    private TextAttributes colorCorrect = new TextAttributes(Color.GREEN, Color.BLACK);
    private TextAttributes colorWrong = new TextAttributes(Color.RED, Color.BLACK);
    private TextAttributes colorArrow = new TextAttributes(Color.CYAN, Color.BLACK);

    public TableScreen(Console cn, String postfix, int treeScore, String[] allSubexpressions) {
        this(cn, postfix, treeScore, allSubexpressions, null);
    }

    public TableScreen(Console cn, String postfix, int treeScore,
                       String[] allSubexpressions, String[] allExpressionHeaders) {
        this.cn = cn;
        this.postfix = postfix;
        this.treeScore = treeScore;
        this.allSubexpressions = allSubexpressions;
        this.allExpressionHeaders = allExpressionHeaders;
    }

    // Builds the truth table and draws it to screen (called once by GameEngine).
    public void init() {
        if (initialized) return;

        resetHiddenCells();
        configureExpressionColumns();
        generateTruthTable();
        hideOneCellPerExpressionColumn();

        drawTable();
        printQuestionStatus();
        initialized = true;
    }

    private void configureExpressionColumns() {
        String[] postfixCandidates = new String[32];
        String[] headerCandidates = new String[32];
        int candidateCount = 0;

        if (allSubexpressions != null) {
            for (int i = 0; i < allSubexpressions.length && candidateCount < postfixCandidates.length; i++) {
                String expr = allSubexpressions[i];
                if (expr == null || expr.isEmpty() || !containsOperator(expr)) continue;

                postfixCandidates[candidateCount] = expr;
                headerCandidates[candidateCount] = expressionHeaderAt(i, expr);
                candidateCount++;
            }
        }

        if (candidateCount == 0 && postfix != null && !postfix.isEmpty()) {
            postfixCandidates[candidateCount] = postfix;
            headerCandidates[candidateCount] = "Result";
            candidateCount++;
        }

        int start = Math.max(0, candidateCount - MAX_EXPRESSION_COLUMNS);
        expressionCount = 0;
        for (int i = start; i < candidateCount; i++) {
            expressionPostfixes[expressionCount] = postfixCandidates[i];
            expressionHeaders[expressionCount] = compactHeader(headerCandidates[i]);
            expressionCount++;
        }

        calculateColumnLayout();
    }

    private String expressionHeaderAt(int index, String fallback) {
        if (allExpressionHeaders != null &&
                index < allExpressionHeaders.length &&
                allExpressionHeaders[index] != null &&
                !allExpressionHeaders[index].isEmpty()) {
            return allExpressionHeaders[index];
        }
        return fallback;
    }

    private String compactHeader(String header) {
        if (header == null || header.isEmpty()) return "Result";
        return header.replace(" ", "");
    }

    private boolean containsOperator(String expr) {
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '~' || c == '^' || c == 'v' || c == '+' || c == '>' || c == '=') {
                return true;
            }
        }
        return false;
    }

    private void calculateColumnLayout() {
        columnWidths[0] = 4; // ABCD
        for (int i = 0; i < expressionCount; i++) {
            int headerLength = expressionHeaders[i].length();
            columnWidths[i + 1] = Math.max(3, Math.min(18, headerLength));
        }

        while (calculateContentWidth() > MAX_CONTENT_WIDTH && canShrinkColumns()) {
            int widestIndex = 1;
            for (int i = 2; i <= expressionCount; i++) {
                if (columnWidths[i] > columnWidths[widestIndex]) {
                    widestIndex = i;
                }
            }
            columnWidths[widestIndex]--;
        }

        tableContentWidth = calculateContentWidth();

        int offset = columnWidths[0] + 3;
        for (int i = 0; i < expressionCount; i++) {
            expressionOffsets[i] = offset;
            offset += columnWidths[i + 1] + 3;
        }
    }

    private int calculateContentWidth() {
        int width = columnWidths[0];
        for (int i = 0; i < expressionCount; i++) {
            width += 3 + columnWidths[i + 1];
        }
        return width;
    }

    private boolean canShrinkColumns() {
        for (int i = 1; i <= expressionCount; i++) {
            if (columnWidths[i] > 3) return true;
        }
        return false;
    }

    /**
     * Evaluates the Postfix expression using the Object-type Stack data structure taught in class.
     */
    private int evaluatePostfix(String expr, int valA, int valB, int valC, int valD) {
        Stack stack = new Stack(50);

        for (int i = 0; i < expr.length(); i++) {
            char ch = expr.charAt(i);

            if (ch == ' ') continue;

            if (ch == 'A') stack.push(valA);
            else if (ch == 'B') stack.push(valB);
            else if (ch == 'C') stack.push(valC);
            else if (ch == 'D') stack.push(valD);
            else if (ch == 'a') stack.push((valA == 0) ? 1 : 0);
            else if (ch == 'b') stack.push((valB == 0) ? 1 : 0);
            else if (ch == 'c') stack.push((valC == 0) ? 1 : 0);
            else if (ch == 'd') stack.push((valD == 0) ? 1 : 0);
            else if (ch == '~') {
                if (stack.isEmpty()) return 0;
                int op1 = (Integer) stack.pop();
                stack.push((op1 == 0) ? 1 : 0);
            } else {
                if (stack.isEmpty()) return 0;
                int op2 = (Integer) stack.pop();
                if (stack.isEmpty()) return 0;
                int op1 = (Integer) stack.pop();

                if (ch == '^') stack.push((op1 == 1 && op2 == 1) ? 1 : 0);
                else if (ch == 'v') stack.push((op1 == 1 || op2 == 1) ? 1 : 0);
                else if (ch == '+') stack.push((op1 != op2) ? 1 : 0);
                else if (ch == '>') stack.push((op1 == 1 && op2 == 0) ? 0 : 1);
                else if (ch == '=') stack.push((op1 == op2) ? 1 : 0);
            }
        }

        if (stack.isEmpty()) return 0;
        return (Integer) stack.pop();
    }

    private void generateTruthTable() {
        for (int i = 0; i < ROW_COUNT; i++) {
            int a = (i / 8) % 2;
            int b = (i / 4) % 2;
            int c = (i / 2) % 2;
            int d = i % 2;

            truthTable[i][0] = a;
            truthTable[i][1] = b;
            truthTable[i][2] = c;
            truthTable[i][3] = d;

            for (int exprIdx = 0; exprIdx < expressionCount; exprIdx++) {
                truthTable[i][VARIABLE_COLS + exprIdx] =
                        evaluatePostfix(expressionPostfixes[exprIdx], a, b, c, d);
            }
        }
    }

    private void hideOneCellPerExpressionColumn() {
        totalQuestions = 0;
        for (int exprIdx = 0; exprIdx < expressionCount; exprIdx++) {
            int row = rnd.nextInt(ROW_COUNT);
            int col = VARIABLE_COLS + exprIdx;

            isHidden[row][col] = true;
            hiddenQuestionRows[exprIdx] = row;
            totalQuestions++;
        }
    }

    /**
     * Called every game loop cycle. Processes the player's keyboard input.
     * @return true: Table screen is completed (can return to Maze)
     */
    public boolean update(int keypr, Player player) {
        if (!initialized) return false;
        if (completed) return true;

        if (keypr == KeyEvent.VK_K && currentQuestion >= totalQuestions) {
            drawKarnaughMap();
            return false;
        }

        if (currentQuestion < totalQuestions) {
            if (isAnswerKey(keypr)) {
                answerCurrentQuestion(keypr, player);
            }
            return false;
        }

        if (keypr == KeyEvent.VK_ENTER) {
            completed = true;
            return true;
        }

        return false;
    }

    private boolean isAnswerKey(int keypr) {
        return keypr == KeyEvent.VK_0 || keypr == KeyEvent.VK_NUMPAD0 ||
               keypr == KeyEvent.VK_1 || keypr == KeyEvent.VK_NUMPAD1;
    }

    private void answerCurrentQuestion(int keypr, Player player) {
        int exprIdx = currentQuestion;
        int row = hiddenQuestionRows[exprIdx];
        int col = VARIABLE_COLS + exprIdx;

        int answer = (keypr == KeyEvent.VK_1 || keypr == KeyEvent.VK_NUMPAD1) ? 1 : 0;
        int correct = truthTable[row][col];

        if (answer == correct) {
            correctCount++;
            player.addScore(3);
        } else {
            player.addScore(-2);
        }

        isHidden[row][col] = false;
        currentQuestion++;

        drawTable();

        if (currentQuestion >= totalQuestions) {
            printStatus("All done. Correct: " + correctCount + "/" + totalQuestions +
                    "  Tree: " + treeScore + "  Press K for Karnaugh Map or ENTER to return.", colorArrow);
        } else {
            TextAttributes resultColor = (answer == correct) ? colorCorrect : colorWrong;
            drawCellValue(row, exprIdx, String.valueOf(answer), resultColor);
            if (answer == correct) {
                printStatus("Correct. " + nextQuestionText(), colorCorrect);
            } else {
                printStatus("Wrong. Correct value was " + correct + ". " + nextQuestionText(), colorWrong);
            }
        }
    }

    private String nextQuestionText() {
        int exprIdx = currentQuestion;
        int row = hiddenQuestionRows[exprIdx];
        return "Question " + (currentQuestion + 1) + "/" + totalQuestions +
                "  ABCD=" + abcdForRow(row) + "  " + expressionHeaders[exprIdx];
    }

    private void printQuestionStatus() {
        if (currentQuestion < totalQuestions) {
            printStatus("Fill yellow cells with 0 or 1. " + nextQuestionText(), colorArrow);
        }
    }

    public void drawTable() {
        ConsoleUtils.clearScreen(cn);
        drawIntro();

        drawBorder(TABLE_TOP);
        printInside(TABLE_TOP + 1, buildHeaderLine());
        printInside(TABLE_TOP + 2, buildSeparatorLine());

        for (int row = 0; row < ROW_COUNT; row++) {
            if (row > 0 && row % 4 == 0) {
                printInside(rowY(row) - 1, "");
            }
            printInside(rowY(row), buildDataRow(row));
            drawHiddenCellsForRow(row);
        }

        drawBorder(rowY(ROW_COUNT - 1) + 1);
    }

    private void drawIntro() {
        ConsoleUtils.printString(cn, 3, 0, "--- (3) Table Screen ---", colorNormal);
        ConsoleUtils.printString(cn, 5, 2,
                "- After the tree screen, the truth table of the expression is computed.", colorNormal);
        ConsoleUtils.printString(cn, 5, 3,
                "- Each expression column has one random yellow question slot.", colorNormal);
        ConsoleUtils.printString(cn, 5, 4,
                "- Correct answers are +3 points; wrong answers are -2 points.", colorNormal);
    }

    private String buildHeaderLine() {
        String line = fitCell("ABCD", columnWidths[0]);
        for (int i = 0; i < expressionCount; i++) {
            line += " | " + fitCell(expressionHeaders[i], columnWidths[i + 1]);
        }
        return line;
    }

    private String buildSeparatorLine() {
        String line = repeat('-', columnWidths[0]);
        for (int i = 0; i < expressionCount; i++) {
            line += "-+-" + repeat('-', columnWidths[i + 1]);
        }
        return line;
    }

    private String buildDataRow(int row) {
        String line = fitCell(abcdForRow(row), columnWidths[0]);
        for (int exprIdx = 0; exprIdx < expressionCount; exprIdx++) {
            int col = VARIABLE_COLS + exprIdx;
            String value = isHidden[row][col] ? "?" : String.valueOf(truthTable[row][col]);
            line += " | " + centerCell(value, columnWidths[exprIdx + 1]);
        }
        return line;
    }

    private String abcdForRow(int row) {
        return "" + truthTable[row][0] + truthTable[row][1] + truthTable[row][2] + truthTable[row][3];
    }

    private int rowY(int row) {
        return TABLE_TOP + 3 + row + (row / 4);
    }

    private void drawHiddenCellsForRow(int row) {
        for (int exprIdx = 0; exprIdx < expressionCount; exprIdx++) {
            int col = VARIABLE_COLS + exprIdx;
            if (isHidden[row][col]) {
                drawCellValue(row, exprIdx, "?", colorHidden);
            }
        }
    }

    private void drawCellValue(int row, int exprIdx, String value, TextAttributes color) {
        int width = columnWidths[exprIdx + 1];
        int x = CONTENT_LEFT + expressionOffsets[exprIdx] + centerOffset(width, value.length());
        ConsoleUtils.printString(cn, x, rowY(row), value, color);
    }

    private int centerOffset(int width, int textLength) {
        return Math.max(0, (width - textLength) / 2);
    }

    private void drawBorder(int y) {
        ConsoleUtils.printString(cn, TABLE_LEFT, y, "+" + repeat('-', tableContentWidth + 2) + "+");
    }

    private void printInside(int y, String line) {
        ConsoleUtils.printString(cn, TABLE_LEFT, y,
                "| " + padRight(line, tableContentWidth) + " |");
    }

    private void printStatus(String text, TextAttributes color) {
        ConsoleUtils.printString(cn, 2, STATUS_Y, repeat(' ', 96));
        ConsoleUtils.printString(cn, 2, STATUS_Y, fitCell(text, 96), color);
    }

    private String fitCell(String text, int width) {
        if (text.length() <= width) return padRight(text, width);
        if (width <= 1) return text.substring(0, width);
        return text.substring(0, width - 1) + "~";
    }

    private String centerCell(String text, int width) {
        if (text.length() > width) return fitCell(text, width);
        int leftPad = (width - text.length()) / 2;
        int rightPad = width - text.length() - leftPad;
        return repeat(' ', leftPad) + text + repeat(' ', rightPad);
    }

    private String padRight(String text, int width) {
        if (text.length() >= width) return text;
        return text + repeat(' ', width - text.length());
    }

    private String repeat(char c, int count) {
        String result = "";
        for (int i = 0; i < count; i++) {
            result += c;
        }
        return result;
    }

    /**
     * Converts Gray code to Binary (2-bit).
     * Gray: 0->00(0), 1->01(1), 3->11(2), 2->10(3)
     */
    private int grayToBinary(int gray) {
        return gray ^ (gray >> 1);
    }

    public void drawKarnaughMap() {
        ConsoleUtils.clearScreen(cn);

        int[] rowGray = {0, 1, 3, 2};
        int[] colGray = {0, 1, 3, 2};

        int startX = 5;
        int startY = 6;
        int lastExprCol = VARIABLE_COLS + expressionCount - 1;

        ConsoleUtils.printString(cn, 3, 0, "--- Karnaugh Map ---", colorNormal);
        ConsoleUtils.printString(cn, 3, 2, "Expression: " + expressionHeaders[expressionCount - 1], colorNormal);
        ConsoleUtils.printString(cn, 3, 4, "Correct: " + correctCount + "/" + totalQuestions, colorNormal);

        ConsoleUtils.printString(cn, startX, startY, "   CD");
        ConsoleUtils.printString(cn, startX, startY + 1, "AB    00  01  11  10");
        ConsoleUtils.printString(cn, startX, startY + 2, "    +---+---+---+---+");

        for (int r = 0; r < 4; r++) {
            int abBinary = grayToBinary(rowGray[r]);
            int abA = (abBinary >> 1) & 1;
            int abB = abBinary & 1;

            ConsoleUtils.printString(cn, startX, startY + 3 + r * 2, " " + abA + abB + " |");

            for (int c = 0; c < 4; c++) {
                int cdBinary = grayToBinary(colGray[c]);
                int cdC = (cdBinary >> 1) & 1;
                int cdD = cdBinary & 1;

                int index = abA * 8 + abB * 4 + cdC * 2 + cdD;
                int result = truthTable[index][lastExprCol];

                ConsoleUtils.printString(cn, startX + 5 + c * 4, startY + 3 + r * 2, " " + result + " |");
            }
            ConsoleUtils.printString(cn, startX, startY + 4 + r * 2, "    +---+---+---+---+");
        }

        ConsoleUtils.printString(cn, 3, 25, "Press ENTER to return to Maze.", colorArrow);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void reset() {
        initialized = false;
        completed = false;
        currentQuestion = 0;
        correctCount = 0;
        totalQuestions = 0;
        resetHiddenCells();
    }

    private void resetHiddenCells() {
        for (int i = 0; i < hiddenQuestionRows.length; i++) {
            hiddenQuestionRows[i] = -1;
        }
        for (int i = 0; i < ROW_COUNT; i++) {
            for (int j = 0; j < TOTAL_COLS; j++) {
                isHidden[i][j] = false;
            }
        }
    }
}
