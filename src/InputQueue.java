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

    // Circular Queue variables from course notes
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

        // Constructor from course notes (Slide 14)
        elements = new Character[QUEUE_CAPACITY];
        rear = -1;
        front = 0;

        // Fill the queue completely at game start
        for (int i = 0; i < QUEUE_CAPACITY; i++) {
            enqueue(generateRandomElement());
        }
    }

    // --- CIRCULAR QUEUE METHODS FROM COURSE NOTES ---

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

            // Game logic: when one leaves, a new one is immediately generated and enqueued
            // This keeps the queue at 10 elements at all times.
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

    // --- GAME-SPECIFIC METHODS ---

    private char generateRandomElement() {
        int roll = rnd.nextInt(10);
        if (roll < 7) {
            int symbolIndex = rnd.nextInt(logicSymbols.length);
            return logicSymbols[symbolIndex];
        } else if (roll < 9) {
            return '@';
        } else {
            // 50% chance for Red (Targeted) or Green (Random) robot
            return rnd.nextBoolean() ? 'R' : 'G';
        }
    }

    public void draw(int drawX, int drawY) {
        ConsoleUtils.printString(cn, drawX, drawY - 1, "Input Queue");
        ConsoleUtils.printString(cn, drawX, drawY,     "-----------");

        if (isEmpty()) return;

        // When drawing the circular queue, iterate from front up to size elements
        int current = front;
        for (int i = 0; i < size(); i++) {
            char c = elements[current];
            TextAttributes color;

            if (c == '@') color = colorFireball;
            else if (c == 'R') {
                color = colorRobot; // RED - targeted robot
                c = 'X';
            } else if (c == 'G') {
                color = new enigma.console.TextAttributes(java.awt.Color.GREEN, java.awt.Color.BLACK); // GREEN - random robot
                c = 'X';
            }
            else color = colorSymbol;

            cn.getTextWindow().output(drawX + 4, drawY + 1 + i, c, color);

            current = (current + 1) % elements.length;
        }

        ConsoleUtils.printString(cn, drawX, drawY + QUEUE_CAPACITY + 1, "-----------");
    }
}