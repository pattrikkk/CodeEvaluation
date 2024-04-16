import java.util.Arrays;

public class ReferenceClass {
    private int a, b;
    private String c;

    static int staticInt = 0;

    public ReferenceClass(String c, int a, int b) {
        this.c = c;
        this.a = a;
        this.b = b;
    }

    static void setStaticInt() {
        staticInt++;
    }

    static int getStaticInt() {
        return staticInt;
    }

    public void setA(int aa) { a = aa; }
    public void setB(int bb) { b = bb; }
    public void setC(String cc, String dd) { c = dd; }
    public int getA() { return a; }
    public int getB() { return b; }

    public int sum() {	return a + b; }
    public int product() { return a * b; }
    public int difference() { return a - b; }
    public int quotient() {	if (b == 0) return -1; else return a / b; }

    public int modulo() { if (b == 0) return -1; else return a % b; }

    public String getC() { return c; }

    public boolean bool() {	return true; }
}