package Zadanie1;

public class ReferenceClass {
    private int rows, cols;
    private int x, y;
    char border;

    public ReferenceClass(int r, int c, int xx, int yy, char b) {
        if (r < 5) rows = 10; else rows = r;
        if (c < 5) cols = 10; else cols = c;
        if (xx <= 1 || xx >= cols) x = 3; else x = xx;
        if (yy <= 1 || yy >= rows) y = 2; else y = yy;
        border = b;
    }

    public String left() {
        if (x > 2) {
            x--;
            return "OK";
        }
        return "Au!";
    }

    public String right() {
        if (x < cols - 1) {
            x++;
            return "OK";
        }
        return "Au!";
    }

    public String up() {
        if (y > 2) {
            y--;
            return "OK";
        }
        return "Au!";
    }

    public String down() {
        if (y < rows - 1) {
            y++;
            return "OK";
        }
        return "Au!";
    }

    public String getInfo() {
        String full = "", empty= "", result;
        for (int i = 1; i <= cols - 2;i++) {
            full += border;
            empty += " ";
        }
        full = full + border + border;
        empty = border + empty + border;
        result = full + "\n";
        for(int i = 2; i < rows; i++) {
            if (i == y) {
                result += empty.substring(0,x-1) + "o" + empty.substring(x)+ "\n";
            } else { result = result + empty + "\n"; }
        }
        result += full;
        return result;
    }

    public String getSpider() {
        return "["+x+","+y+"]";
    }

    public String getDimensions() {
        return rows + " x " + cols;
    }

}
