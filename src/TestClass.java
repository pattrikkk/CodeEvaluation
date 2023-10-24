public class TestClass {
    private int a, b;
    private String c;

    public void setA(int aa) { a = aa; }
    public void setB(int bb) { b = bb; }
    public void setC(String cc, String dd) { c = dd; }
    public int getA() { return a; }
    public int getB() { return b; }

    public int sum() {	return a + b; }
    public int product() { return a * b; }
    public int difference() { return a - b; }
    public int quotient() {	if (b == 0) return -1; else return a / b; }

    public String getC() { return c; }

    public boolean bool() {	return true; }
}
