import enigma.console.Console;
import enigma.console.TextAttributes;

/**
 * ConsoleUtils.java
 * Enigma konsolunun eksik olan String yazdırma ve ekran temizleme metotlarını
 * tamamlar.
 */
public class ConsoleUtils {

    // Ekranı boşluk karakterleriyle doldurarak temizler
    public static void clearScreen(Console cn) {
        for (int i = 0; i < 100; i++) { // GameEngine'de konsol genişliğini 100 belirlemiştik
            for (int j = 0; j < 30; j++) { // Konsol yüksekliğini 30 belirlemiştik
                cn.getTextWindow().output(i, j, ' ');
            }
        }
        cn.getTextWindow().setCursorPosition(0, 0);
    }

    // String (Metin) yazdırmak için karakterleri tek tek ekrana basar
    public static void printString(Console cn, int x, int y, String text) {
        for (int i = 0; i < text.length(); i++) {
            // Konsol sınırlarını aşmamak için ufak bir kontrol
            if (x + i < 100) {
                cn.getTextWindow().output(x + i, y, text.charAt(i));
            }
        }
    }

    // Renkli String (Metin) yazdırmak için
    public static void printString(Console cn, int x, int y, String text, TextAttributes attr) {
        for (int i = 0; i < text.length(); i++) {
            if (x + i < 100) {
                cn.getTextWindow().output(x + i, y, text.charAt(i), attr);
            }
        }
    }
}