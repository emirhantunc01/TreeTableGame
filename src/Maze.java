import java.io.BufferedReader;
import java.io.FileReader;
import enigma.console.Console;

/**
 * Maze.java
 * Handles the game's map structure, boundaries, and validation.
 * Supports loading layouts from a text file and ensuring that
 * all empty areas are fully connected via a Flood-Fill algorithm.
 */
public class Maze {
    // Dimensions updated for the Tree Table project
    public static final int ROWS = 21;
    public static final int COLS = 45;
    public static char[][] map = new char[ROWS][COLS];

    public Maze() {
        resetMap();
    }

    public static void resetMap() {
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                if (y == 0 || y == ROWS - 1 || x == 0 || x == COLS - 1) {
                    map[y][x] = '#';
                } else {
                    map[y][x] = ' ';
                }
            }
        }
    }

    // --- Load from File ---
    public static Maze loadFromFile(String path) throws Exception {
        resetMap(); // Ensure map is clean before loading
        Maze maze = new Maze();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        int row = 0;

        while ((line = br.readLine()) != null && row < ROWS) {
            for (int col = 0; col < Math.min(line.length(), COLS); col++) {
                map[row][col] = line.charAt(col);
            }
            row++;
        }
        br.close();

        if (row == 0) throw new Exception("File is empty or unreadable!");
        return maze;
    }

    // --- Validation ---
    public String validate() {
        // Outer border check
        for (int x = 0; x < COLS; x++) {
            if (map[0][x] != '#')
                return "Error: Top border missing! (row 0, col " + x + ")";
            if (map[ROWS - 1][x] != '#')
                return "Error: Bottom border missing! (row " + (ROWS - 1) + ", col " + x + ")";
        }
        for (int y = 0; y < ROWS; y++) {
            if (map[y][0] != '#')
                return "Error: Left border missing! (row " + y + ", col 0)";
            if (map[y][COLS - 1] != '#')
                return "Error: Right border missing! (row " + y + ", col " + (COLS - 1) + ")";
        }

        // Connectivity check
        if (!isConnected()) {
            return "Error: There are unreachable empty areas in the maze! All empty spaces must be connected.";
        }

        return null; // Valid
    }

    // --- Flood-Fill Connectivity Check (Array based) ---
    public boolean isConnected() {
        boolean[][] visited = new boolean[ROWS][COLS];

        // Find the first empty cell
        int startY = -1, startX = -1;
        int totalEmpty = 0;

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                if (map[y][x] != '#') {
                    totalEmpty++;
                    if (startY == -1) {
                        startY = y;
                        startX = x;
                    }
                }
            }
        }

        if (totalEmpty == 0)
            return true; // Completely wall — considered valid

        // Flood-fill using standard arrays
        int[] arrY = new int[ROWS * COLS];
        int[] arrX = new int[ROWS * COLS];
        int index = 0;
        int reachable = 0;

        arrY[index] = startY;
        arrX[index] = startX;
        index++;
        visited[startY][startX] = true;

        int[] dy = { -1, 1, 0, 0 };
        int[] dx = { 0, 0, -1, 1 };

        while (index > 0) {
            index--;
            int cy = arrY[index];
            int cx = arrX[index];
            reachable++;

            for (int d = 0; d < 4; d++) {
                int ny = cy + dy[d];
                int nx = cx + dx[d];
                if (ny >= 0 && ny < ROWS && nx >= 0 && nx < COLS
                        && !visited[ny][nx] && map[ny][nx] != '#') {
                    visited[ny][nx] = true;
                    arrY[index] = ny;
                    arrX[index] = nx;
                    index++;
                }
            }
        }

        return reachable == totalEmpty;
    }

    // --- Draw to Enigma Console ---
    public void draw(Console cn) {
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                cn.getTextWindow().output(x, y, map[y][x]);
            }
        }
    }
}