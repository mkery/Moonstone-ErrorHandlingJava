package Environment;

import java.util.Collection;

/**
 * Created by florian on 11/15/16.
 */
public class A implements AutoCloseable {
    public void destroy() {

    }

    public void actionA() {

    }

    public void actionB() throws VeryNiceException {

    }

    public void actionC() throws SomeOtherException {

    }

    public void actionD() throws ExceptionA {

    }
    
    public int functionA(int arg) {
        return 2 * arg;
    }

    public int functionB(int arg) throws VeryNiceException {
        return 2 * arg;
    }

    public int functionC(int irgnored, int arg) throws SomeOtherException {
        return 2 * arg;
    }

    public int functionD(int arg) throws ExceptionA {
        return 2 * arg;
    }
    
    public int functionE() {
        return 5;
    }
    
    public int functionF(int irgnored, int arg) {
        return 2 * arg;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    public boolean isConsistent() {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection<C> getAllC() {
        // TODO Auto-generated method stub
        return null;
    }
    
}
