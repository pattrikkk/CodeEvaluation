package Zadanie2;

public class Label {
    private String title;
    private String name;
    private String surname;

    public Label(String t, String n, String s) {
        if (t.length() < 1) t = "titled"; title = t;
        if (n.length() < 1) n = "Anonymous"; name = n;
        if (s.length() < 1) s = "Anonymous"; surname = s;
    }

    public Label(String n, String s) {
        this("",n,s);
        title = "";
    }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public String getLabel() {
        return title+". "+ name+" "+surname;
    }
}
