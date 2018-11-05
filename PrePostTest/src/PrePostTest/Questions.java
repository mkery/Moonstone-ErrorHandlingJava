package PrePostTest;

import java.util.ArrayList;
import java.util.List;

import Environment.A;
import Environment.ImportantException;
import Environment.Logger;
import Environment.SeriousException;

public class Questions {
    
Logger Emergency = new Logger();

    void strikeA() {
A a = new A();
try {
    a.actionA();
    a.actionB();
    a.actionC();
    a.actionD();
} catch (ImportantException e) {
    e.printStackTrace();
}
    }
    
    void strikeB() {
A a = new A();
try {
    a.actionD();
    a.actionC();
    a.actionB();
    a.actionA();
} catch (SeriousException e) {
    Emergency.log(e);
} catch (ImportantException e) {
    e.printStackTrace();
} finally {
    System.out.println("finally");
}
    }
    
    void strikeC() {
A a = new A();
try {
    try {
        a.actionA();
        a.actionB();
        a.actionC();
    } catch (SeriousException e) {
        Emergency.log(e);
    }
    a.actionD();
} catch (ImportantException e) {
    e.printStackTrace();
}
    }
    
    void strikeD() {
A a = new A();
try {
    try {
        a.actionC(
                a.actionD());
        a.actionB();
    } catch (ImportantException e) {
        System.out.println(e.getClass().getName() + " occurred!");
        throw e;
    } finally {
        System.out.println("finally inner");
    }
} catch (SeriousException e) {
    Emergency.log(e);
} catch (ImportantException e) {
    e.printStackTrace();
} finally {
    System.out.println("finally outer");
}
    }

}
