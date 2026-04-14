/**
 * ScoreNode.java
 * Represents a single node in the Doubly Linked List for the High Score Table.
 */
public class ScoreNode {
    private String name;
    private int score;
    private ScoreNode prev;
    private ScoreNode next;

    public ScoreNode(String name, int score) {
        this.name = name;
        this.score = score;
        this.prev = null;
        this.next = null;
    }

    public String getName() { return name; }
    public int getScore() { return score; }

    public ScoreNode getPrev() { return prev; }
    public void setPrev(ScoreNode prev) { this.prev = prev; }

    public ScoreNode getNext() { return next; }
    public void setNext(ScoreNode next) { this.next = next; }
}