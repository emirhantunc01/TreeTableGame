import enigma.console.Console;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * DoublyLinkedList.java
 * Manages the high scores in descending order.
 */
public class DoublyLinkedList {
    private ScoreNode head;
    private ScoreNode tail;

    public DoublyLinkedList() {
        head = null;
        tail = null;
    }

    // Skoru her zaman büyükten küçüğe doğru (descending) uygun aralığa ekler
    public void insert(String name, int score) {
        ScoreNode newNode = new ScoreNode(name, score);

        // Liste boşsa
        if (head == null) {
            head = newNode;
            tail = newNode;
            return;
        }

        // Yeni skor en büyükse (Başa ekleme)
        if (score >= head.getScore()) {
            newNode.setNext(head);
            head.setPrev(newNode);
            head = newNode;
            return;
        }

        // Araya veya sona ekleme için doğru yeri bul
        ScoreNode current = head;
        while (current.getNext() != null && current.getNext().getScore() > score) {
            current = current.getNext();
        }

        // current düğümünün sağına ekle
        newNode.setNext(current.getNext());
        if (current.getNext() != null) {
            current.getNext().setPrev(newNode);
        } else {
            tail = newNode; // Sona eklendi
        }
        current.setNext(newNode);
        newNode.setPrev(current);
    }

    // "highscore.txt" dosyasından verileri okur
    public void loadFromFile(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                // Örnek satır: "Tarkan Bulut 728" veya "Irmak Yol 412"
                // Son boşluktan sonrasını skor, öncesini isim olarak ayır
                int lastSpaceIndex = line.lastIndexOf(' ');
                if (lastSpaceIndex != -1) {
                    String name = line.substring(0, lastSpaceIndex).trim();
                    int score = Integer.parseInt(line.substring(lastSpaceIndex + 1).trim());
                    insert(name, score); // Azalan sırada listeye ekler
                }
            }
            br.close();
        } catch (Exception e) {
            // Dosya yoksa veya okunamazsa varsayılan listeyi oluştur
            insert("Irmak Yol", 412);
            insert("Tarkan Bulut", 728);
            insert("Ali Deniz", 56);
            insert("Deniz Toprak", 190);
        }
    }

    // Ekrana Çizdirme
    public void display(Console cn, int startX, int startY) {
        ScoreNode current = head;
        int y = startY;

        ConsoleUtils.printString(cn, startX, y++, "--- HIGH SCORE TABLE ---");

        while (current != null) {
            // İsim ve skor hizalaması için formatlama
            String line = String.format("%-15s %5d", current.getName(), current.getScore());
            ConsoleUtils.printString(cn, startX, y++, line);
            current = current.getNext();
        }
    }
}