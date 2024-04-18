package Zadanie1;

public class Spider {
    private int rows;
    private int columns;
    private int x;
    private int y;
    private char boundary;

    public Spider(int r, int c, int xx, int yy, char b) {
        if (r < 5) rows = 10; else rows = r;
        if (c < 5) columns = 10; else columns = c;
        if (xx <= 1 || xx >= columns) x = 3; else x = xx;
        if (yy <= 1 || yy >= rows) y = 2; else y = yy;
        boundary = b;
    }



    public String right() {
        if (x < columns - 1) {
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

    public String getSpider() {
        return "[" + x + "," + y + "]";
    }

    public String getDimensions() {
        return rows + " x " + columns;
    }

    public String getInfo() {
        String full = "", empty= "", result;
        for (int i = 1; i <= columns - 2;i++) {
            full += boundary;
            empty += " ";
        }
        full = full + boundary + boundary;
        empty = boundary + empty + boundary;
        result = full + "\n";
        for(int i = 2; i < rows; i++) {
            if (i == y) {
                result += empty.substring(0,x-1) + "o" + empty.substring(x)+ "\n";
            } else { result = result + empty + "\n"; }
        }
        result += full;
        return result;
    }

    public void setValues(int rows, int columns, int x, int y, char boundaryChar) {
        this.rows = rows < 5 ? 10 : rows;
        this.columns = columns < 5 ? 10 : columns;
        this.x = (x <= 0 || x > this.rows) ? 3 : x;
        this.y = (y <= 0 || y > this.columns) ? 2 : y;
        this.boundary = boundary;
    }
}
