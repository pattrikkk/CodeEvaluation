public class TestClass {
    private int a, b;

    public void setA(int aa) { a = aa; }
    public void setB(int bb) { b = bb; }
    public int getA() { return a; }
    public int getB() { return b; }

    public int sum() {	return a + b; }
    public int product() { return a * b; }
    public int difference() { return a - b; }
    public int quotient() {	if (b == 0) return -1; else return a / b; }

    public boolean bool() {	return true; }
}
