import enigma.console.Console;
import enigma.console.TextAttributes;
import java.awt.Color;
import java.util.Random;

/**
 * InputQueue.java
 * Circular Queue implementation based on course materials.
 * Manages the 10-element input queue displayed on the right side of the screen.
 */
public class InputQueue {
    public static final int QUEUE_CAPACITY = 10;

    // Ders notlarındaki Circular Queue değişkenleri
    private int front;
    private int rear;
    private Character[] elements;

    private final char[] logicSymbols = {
            'A', 'B', 'C', 'D',
            'a', 'b', 'c', 'd',
            '~', '^', 'v', '+', '>', '='
    };

    private Console cn;
    private Random rnd = new Random();

    private TextAttributes colorSymbol = new TextAttributes(Color.YELLOW, Color.BLACK);
    private TextAttributes colorFireball = new TextAttributes(Color.CYAN, Color.BLACK);
    private TextAttributes colorRobot = new TextAttributes(Color.RED, Color.BLACK);

    public InputQueue(Console cn) {
        this.cn = cn;

        // Ders notlarındaki Constructor (Slayt 14)
        elements = new Character[QUEUE_CAPACITY];
        rear = -1;
        front = 0;

        // Oyun başladığında kuyruğu tamamen doldur
        for (int i = 0; i < QUEUE_CAPACITY; i++) {
            enqueue(generateRandomElement());
        }
    }

    // --- DERS NOTLARINDAKİ CIRCULAR QUEUE METOTLARI ---

    public boolean isEmpty() {
        return elements[front] == null;
    }

    public boolean isFull() {
        return (front == (rear + 1) % elements.length &&
                elements[front] != null &&
                elements[rear] != null);
    }

    public void enqueue(Character data) {
        if (isFull()) {
            System.out.println("Queue overflow");
        } else {
            rear = (rear + 1) % elements.length;
            elements[rear] = data;
        }
    }

    public Character dequeue() {
        if (isEmpty()) {
            System.out.println("Queue is empty");
            return null;
        } else {
            Character retData = elements[front];
            elements[front] = null;
            front = (front + 1) % elements.length;

            // Oyun mantığı: Biri çıktığında anında yenisi üretilip kuyruğa eklenir
            // Böylece kuyruk her zaman 10 elemanlı kalır.
            enqueue(generateRandomElement());

            return retData;
        }
    }

    public Character peek() {
        if (isEmpty()) {
            System.out.println("Queue is empty");
            return null;
        } else {
            return elements[front];
        }
    }

    public int size() {
        if (elements[front] == null) {
            return 0;
        } else {
            if (rear >= front)
                return rear - front + 1;
            else
                return elements.length - (front - rear) + 1;
        }
    }

    // --- OYUN ÖZEL METOTLARI ---

    private char generateRandomElement() {
        int roll = rnd.nextInt(10);
        if (roll < 7) {
            int symbolIndex = rnd.nextInt(logicSymbols.length);
            return logicSymbols[symbolIndex];
        } else if (roll < 9) {
            return '@';
        } else {
            return 'X';
        }
    }

    public void draw(int drawX, int drawY) {
        ConsoleUtils.printString(cn, drawX, drawY - 1, "Input Queue");
        ConsoleUtils.printString(cn, drawX, drawY,     "-----------");

        if (isEmpty()) return;

        // Circular Queue ekrana çizdirilirken front'tan başlayıp size kadar dönmeliyiz
        int current = front;
        for (int i = 0; i < size(); i++) {
            char c = elements[current];
            TextAttributes color;

            if (c == '@') color = colorFireball;
            else if (c == 'X') color = colorRobot;
            else color = colorSymbol;

            cn.getTextWindow().output(drawX + 4, drawY + 1 + i, c, color);

            current = (current + 1) % elements.length;
        }

        ConsoleUtils.printString(cn, drawX, drawY + QUEUE_CAPACITY + 1, "-----------");
    }
}