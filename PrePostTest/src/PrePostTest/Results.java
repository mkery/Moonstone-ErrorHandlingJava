package PrePostTest;

import Environment.ImportantException;
import Environment.SeriousException;

public class Results {
    private boolean whiteFails = false;

    private final StringBuilder sb = new StringBuilder();
    
    public static void main(String[] args) {

        Results r;
        r = new Results();
        try { r.calculate(); } catch (Throwable e) {}
        r.print();

        r = new Results();
        r.whiteFails = true;
        try { r.calculate(); } catch (Throwable e) {}
        r.print();
    }

    private void print() {
        System.out.format("%b\nk = %s\n", whiteFails, sb);
    }

    private void log(String s) {
        if (sb.length() > 0) sb.append(" – ");
        sb.append(s);
    }

void calculate() throws Exception {
    log("start");
    try {
        try {
            log("start nested");
            white();
            log("end nested");
        } catch (ImportantException e) {
            log("nested catch");
            throw e;
        } finally {
            log("nested finally");
        }
    } catch(SeriousException e) {
        log("catch");
    } finally {
        log("finally");
    }
}
void white() throws SeriousException {
    if (whiteFails) throw new SeriousException();
    log("someCall");
}

}
