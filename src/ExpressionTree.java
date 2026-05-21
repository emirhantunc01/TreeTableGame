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

    // Tree cursor starts at the root (1)
    private int cursor = 1;

    private Console cn;

    // Colors
    private TextAttributes colorDefault = new TextAttributes(Color.WHITE, Color.BLACK);
    private TextAttributes colorCursor = new TextAttributes(Color.GREEN, Color.BLACK);

    // X and Y coordinates for each node index (1-31) for rendering.
    // Adjust these values visually according to the console screen.
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
        // Fill the tree with space characters
        for (int i = 1; i <= 31; i++) {
            tree[i] = ' ';
        }
    }

    // --- CURSOR MOVEMENTS ---

    // W: Parent, A: Left child, D: Right child. (Player loses 1 point)
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
            return true; // Movement successful (-1 point)
        }
        return false;
    }

    // Automatically moves the cursor to the next empty slot after a symbol is placed
    public void autoMoveCursor() {
        for (int i = 1; i <= 31; i++) {
            if (tree[i] == ' ') {
                cursor = i;
                break;
            }
        }
    }

    // --- ITEM PLACEMENT AND RETRIEVAL ---

    public boolean placeSymbol(char symbol) {
        if (tree[cursor] == ' ') {
            tree[cursor] = symbol;
            autoMoveCursor();
            return true;
        }
        return false; // Slot is full
    }

    public boolean placeSymbolInFirstEmptySlot(char symbol) {
        for (int i = 1; i <= 31; i++) {
            if (tree[i] == ' ') {
                tree[i] = symbol;
                autoMoveCursor();
                return true;
            }
        }
        return false; // Tree is full
    }

    public char takeSymbol() {
        char symbol = tree[cursor];
        if (symbol != ' ') {
            tree[cursor] = ' ';
            return symbol; // Success; item returns to backpack (-2 point)
        }
        return ' '; // Slot is already empty
    }

    // --- TREE VALIDATION ---

    // Called when the F key is pressed.
    // Returns false on invalid tree (-10 point), true on valid tree (proceeds to Table Screen)
    public boolean finishTree() {
        int varCount = 0;
        boolean hasDepth3Node = false;

        for (int i = 1; i <= 31; i++) {
            char c = tree[i];
            if (c != ' ') {
                // Depth 3 means nodes at indices 4-7 (or deeper: 8-31)
                if (i >= 4) hasDepth3Node = true;
                if (c == 'A' || c == 'B' || c == 'C' || c == 'D' ||
                        c == 'a' || c == 'b' || c == 'c' || c == 'd') {
                    varCount++;
                }
            }
        }

        // Rule 1: Minimum 3 variables
        if (varCount < 3) return false;

        // Rule 2: Minimum depth is 3.
        // Depth 3 means we must have at least one node in indices 4 to 31.
        if (!hasDepth3Node) return false;

        return true;
    }

    // Calculates the tree's score (node count * 10)
    public int calculateTreeScore() {
        int count = 0;
        for (int i = 1; i <= 31; i++) {
            if (tree[i] != ' ') count++;
        }
        return count * 10;
    }

    // --- INFIX AND POSTFIX STRING BUILDERS ---

    public String getInfix() {
        return buildInfix(1);
    }

    private String buildInfix(int index) {
        if (index > 31 || tree[index] == ' ') return "";

        boolean isLeaf = !hasChild(index);

        if (isLeaf) {
            return String.valueOf(tree[index]);
        } else if (tree[index] == '~') {
            String operand = buildInfix(firstChildIndex(index));
            return "~(" + operand + ")";
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

    /**
     * Gets all non-empty sub-expressions' postfix forms (sorted by node index)
     * Returns array where index i contains the postfix of the subtree rooted at node i
     */
    public String[] getAllSubexpressionsPostfix() {
        String[] subexpressions = new String[32]; // Index 0 unused, 1-31 for nodes
        for (int i = 1; i <= 31; i++) {
            if (tree[i] != ' ') {
                subexpressions[i] = buildPostfix(i);
            } else {
                subexpressions[i] = "";
            }
        }
        return subexpressions;
    }

    public String[] getTableExpressionPostfixes() {
        String[] expressions = new String[32];
        collectTableExpressionPostfixes(1, expressions, new int[] {0});
        return expressions;
    }

    public String[] getTableExpressionHeaders() {
        String[] expressions = new String[32];
        collectTableExpressionHeaders(1, expressions, new int[] {0});
        return expressions;
    }

    private void collectTableExpressionPostfixes(int index, String[] expressions, int[] count) {
        if (index > 31 || tree[index] == ' ') return;

        collectTableExpressionPostfixes(index * 2, expressions, count);
        collectTableExpressionPostfixes(index * 2 + 1, expressions, count);

        if (isOperator(tree[index]) && count[0] < expressions.length) {
            expressions[count[0]] = buildPostfix(index);
            count[0]++;
        }
    }

    private void collectTableExpressionHeaders(int index, String[] expressions, int[] count) {
        if (index > 31 || tree[index] == ' ') return;

        collectTableExpressionHeaders(index * 2, expressions, count);
        collectTableExpressionHeaders(index * 2 + 1, expressions, count);

        if (isOperator(tree[index]) && count[0] < expressions.length) {
            expressions[count[0]] = buildCompactInfix(index);
            count[0]++;
        }
    }

    private String buildCompactInfix(int index) {
        if (index > 31 || tree[index] == ' ') return "";
        if (!hasChild(index)) return String.valueOf(tree[index]);

        char op = tree[index];
        if (op == '~') {
            return "~(" + buildCompactInfix(firstChildIndex(index)) + ")";
        }

        String left = buildCompactInfix(index * 2);
        String right = buildCompactInfix(index * 2 + 1);

        if (isExpressionNode(index * 2)) left = "(" + left + ")";
        if (isExpressionNode(index * 2 + 1)) right = "(" + right + ")";

        return left + op + right;
    }

    private boolean hasChild(int index) {
        boolean hasLeft = index * 2 <= 31 && tree[index * 2] != ' ';
        boolean hasRight = index * 2 + 1 <= 31 && tree[index * 2 + 1] != ' ';
        return hasLeft || hasRight;
    }

    private int firstChildIndex(int index) {
        if (index * 2 <= 31 && tree[index * 2] != ' ') return index * 2;
        return index * 2 + 1;
    }

    private boolean isExpressionNode(int index) {
        return index <= 31 && tree[index] != ' ' && isOperator(tree[index]);
    }

    private boolean isOperator(char c) {
        return c == '~' || c == '^' || c == 'v' || c == '+' || c == '>' || c == '=';
    }

    // --- DRAW TO SCREEN ---

    public void draw() {
        // Screen clearing should be handled by GameEngine

        for (int i = 1; i <= 31; i++) {
            char symbol = (tree[i] == ' ') ? '.' : tree[i]; // Show empty slots as dots

            // Draw in green if cursor is on this index, otherwise white
            TextAttributes attr = (i == cursor) ? colorCursor : colorDefault;

            cn.getTextWindow().output(drawX[i], drawY[i], symbol, attr);

            // Optional: To draw tree branches (/, \), you can output static characters
            // between drawX and drawY positions.
        }

        // Print Infix and Postfix expressions at the bottom
        ConsoleUtils.printString(cn, 2, 20, "Infix   : " + getInfix());
        ConsoleUtils.printString(cn, 2, 21, "Postfix : " + getPostfix());
    }

    // Resets the tree to initial state (all empty slots)
    public void resetTree() {
        for (int i = 1; i <= 31; i++) {
            tree[i] = ' ';
        }
        cursor = 1;
    }
}
