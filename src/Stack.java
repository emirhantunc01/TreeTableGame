/**
 * Stack.java
 * Implementation based on CME1212 Algorithms and Programming II course materials.
 */
public class Stack {
    private int top;
    private Object[] elements; // [cite: 1330]

    public Stack(int capacity) {
        elements = new Object[capacity]; // [cite: 1332]
        top = -1; // [cite: 1333]
    }

    public void push(Object data) {
        if (isFull())
            System.out.println("Stack overflow"); // [cite: 1337]
        else {
            top++; // [cite: 1339]
            elements[top] = data; // [cite: 1340]
        }
    }

    public Object pop() {
        if (isEmpty()) {
            System.out.println("Stack is empty"); // [cite: 1346]
            return null; // [cite: 1347]
        } else {
            Object retData = elements[top]; // [cite: 1351]
            top--; // [cite: 1351]
            return retData; // [cite: 1354]
        }
    }

    public Object peek() {
        if (isEmpty()) {
            System.out.println("Stack is empty"); // [cite: 1361]
            return null; // [cite: 1362]
        } else
            return elements[top]; // [cite: 1365]
    }

    public boolean isEmpty() {
        return (top == -1); // [cite: 1368]
    }

    public boolean isFull() {
        return (top + 1 == elements.length); // [cite: 1372]
    }

    public int size() {
        return top + 1; // [cite: 1374]
    }
}