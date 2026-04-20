import enigma.console.Console;
import enigma.console.TextAttributes;

/**
 * ConsoleUtils.java
 * Provides missing String printing and screen clearing utility methods
 * for the Enigma console.
 */
public class ConsoleUtils {

    // Clears the screen by filling it with space characters
    public static void clearScreen(Console cn) {
        for (int i = 0; i < 100; i++) { // Console width was set to 100 in GameEngine
            for (int j = 0; j < 30; j++) { // Console height was set to 30 in GameEngine
                cn.getTextWindow().output(i, j, ' ');
            }
        }
        cn.getTextWindow().setCursorPosition(0, 0);
    }

    // Prints a String by outputting each character individually
    public static void printString(Console cn, int x, int y, String text) {
        for (int i = 0; i < text.length(); i++) {
            // Guard against exceeding console boundaries
            if (x + i < 100) {
                cn.getTextWindow().output(x + i, y, text.charAt(i));
            }
        }
    }

    // Prints a colored String by outputting each character individually
    public static void printString(Console cn, int x, int y, String text, TextAttributes attr) {
        for (int i = 0; i < text.length(); i++) {
            if (x + i < 100) {
                cn.getTextWindow().output(x + i, y, text.charAt(i), attr);
            }
        }
    }
}