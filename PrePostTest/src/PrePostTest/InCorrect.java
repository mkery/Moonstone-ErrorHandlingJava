package PrePostTest;

import Environment.A;
import Environment.DeveloperTeam;
import Environment.ImportantException;
import Environment.Logger;

public class InCorrect {
	
Logger Emergency = new Logger();

/** actionA does not throw, close is executed given assumptions */
void correctA() {
A a = new A();
try {
    a.actionA();
    a.close();
} catch (Exception e) {
    DeveloperTeam.notify(e);
}
}

/** close can throw exception */
void incorrectB() throws Exception {
A a = new A();
try {
    try {
        a.actionB();
    } catch (Exception e) {
        DeveloperTeam.notify(e);
    }
} finally {
	a.close();
}
}

/** close can throw exception, but is caught */
void correctC() {
A a = new A();
try {
    try {
        a.actionB();   
    } finally {
        try { a.close(); } 
        catch (Exception e) {}
    }
} catch(Exception e) {
    DeveloperTeam.notify(e);
}
}

/** close may be called twice */
void incorrectH() {
A a = new A();
try {
    try {
        a.actionB();
        a.close();
    } catch (Exception e) {
        DeveloperTeam.notify(e);
        a.close();
    }
} catch (Exception e) {
	DeveloperTeam.notify(e);
}
}

/** exception from implicit close is handled */
void correctE() {
try (A a = new A()) {
    a.actionA();
    a.actionB();
} catch (Exception e) {
    DeveloperTeam.notify(e);
}
}

/** close is executed twice */
void incorrectG() {
try (A a = new A()) {
    try {
        a.actionA();
        a.actionB();
    } finally {
    		a.close();
    }
} catch (Exception e) {
    DeveloperTeam.notify(e);
}
}
    
}
