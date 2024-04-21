package Zadanie3;
public class ReferenceClass {
    private int max;
    private int akt;
    private boolean movement;
    private boolean opened;

    public ReferenceClass(int m) {
        max = m;
        akt = 0;
        movement = false;
        opened = true;
        if (max < 10) max = 10;
    }

    public ReferenceClass(int cur, int mx) {
        if (cur < 0 || mx < 0 || mx < cur) throw new IllegalArgumentException("<0 or too much");
        max = mx;
        akt = cur;
        movement = false;
        opened = true;
    }

    void closeDoor() { opened = false;}
    void openDoor() { opened = true;}
    void move() { if (!opened) movement = true; }
    void stop() { movement = false;  }

    void exiting(int n) {
        if (n <=0 ) return;
        if (opened) {
            if (akt >= n) akt -= n;
            else {
                akt = 0;
                throw new IllegalArgumentException("empty bus");
            }
        }
    }

    void boarding(int n) {
        if (n <=0 ) return;
        if (opened) {
            if (akt + n <= max) akt +=n;
            else {
                n = n + akt - max;
                akt = max;
                throw new IllegalArgumentException("full bus " + n + " more");
            }
        }
    }

    public String getInfo() {
        return "" + akt + "/" + max + " " + opened + "/" + movement;
    }

}
