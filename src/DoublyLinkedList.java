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

    // Inserts score in descending order at the appropriate position
    public void insert(String name, int score) {
        ScoreNode existing = findNodeByName(name);
        if (existing != null) {
            if (score <= existing.getScore()) {
                return;
            }
            removeNode(existing);
        }

        ScoreNode newNode = new ScoreNode(name, score);

        // If the list is empty
        if (head == null) {
            head = newNode;
            tail = newNode;
            return;
        }

        // If the new score is the largest (insert at head)
        if (score >= head.getScore()) {
            newNode.setNext(head);
            head.setPrev(newNode);
            head = newNode;
            return;
        }

        // Find the correct position for insertion or appending at the end
        ScoreNode current = head;
        while (current.getNext() != null && current.getNext().getScore() > score) {
            current = current.getNext();
        }

        // Insert to the right of the current node
        newNode.setNext(current.getNext());
        if (current.getNext() != null) {
            current.getNext().setPrev(newNode);
        } else {
            tail = newNode; // Appended at the end
        }
        current.setNext(newNode);
        newNode.setPrev(current);
    }

    private ScoreNode findNodeByName(String name) {
        ScoreNode current = head;
        while (current != null) {
            if (current.getName().equals(name)) {
                return current;
            }
            current = current.getNext();
        }
        return null;
    }

    private void removeNode(ScoreNode node) {
        ScoreNode previous = node.getPrev();
        ScoreNode next = node.getNext();

        if (previous != null) {
            previous.setNext(next);
        } else {
            head = next;
        }

        if (next != null) {
            next.setPrev(previous);
        } else {
            tail = previous;
        }

        node.setPrev(null);
        node.setNext(null);
    }

    // Loads data from "highscore.txt"
    public void loadFromFile(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                // Example line: "Tarkan Bulut 728" or "Irmak Yol 412"
                // Split: text before the last space is the name, after is the score
                int lastSpaceIndex = line.lastIndexOf(' ');
                if (lastSpaceIndex != -1) {
                    String name = line.substring(0, lastSpaceIndex).trim();
                    int score = Integer.parseInt(line.substring(lastSpaceIndex + 1).trim());
                    insert(name, score); // Inserts into the list in descending order
                }
            }
            br.close();
        } catch (Exception e) {
            // If the file doesn't exist or can't be read, start with an empty list.
            // Previously default entries were inserted here; removed so highscore
            // reflects actual saved data. Leave the list empty and let the game
            // create/append entries on game over.
        }
    }

    // Renders the list to the screen
    public void display(Console cn, int startX, int startY) {
        ScoreNode current = head;
        int y = startY;

        ConsoleUtils.printString(cn, startX, y++, "--- HIGH SCORE TABLE ---");

        while (current != null) {
            // Format name and score with alignment
            String line = String.format("%-15s %5d", current.getName(), current.getScore());
            ConsoleUtils.printString(cn, startX, y++, line);
            current = current.getNext();
        }
    }

    // Saves the list back to "highscore.txt" in descending order
    public void saveToFile(String filename) {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(filename));
            ScoreNode current = head;
            while (current != null) {
                pw.println(current.getName() + " " + current.getScore());
                current = current.getNext();
            }
            pw.close();
        } catch (Exception e) {
            System.out.println("Could not save high score file: " + e.getMessage());
        }
    }
}
