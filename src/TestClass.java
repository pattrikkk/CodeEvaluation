import java.util.Arrays;

public class TestClass {
    private int a, b;
    private String c;
    private int[] d;

    public TestClass(String c, int a, int b, int[] d) {
        this.c = c;
        this.a = a;
        this.b = b;
        this.d = d;
    }

    public void setA(int aa) { a = aa; }
    public void setB(int bb) { b = bb; }
    public void setC(String cc, String dd) { c = dd; }
    public int getA() { return a; }
    public int getB() { return b; }
    public int[] getD() { return d; }
    public String getDString() { return Arrays.toString(d); }

    public int sum() {	return a + b; }
    public int product() { return a * b; }
    public int difference() { return a - b; }
    public int quotient() {	if (b == 0) return -1; else return a / b; }

    public String getC() { return c; }

    public boolean bool() {	return true; }
}
