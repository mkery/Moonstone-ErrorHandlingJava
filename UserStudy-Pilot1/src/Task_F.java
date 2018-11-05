import Environment.*;

public class Task_F implements ITask_F {

    /*
     * You have 2 min to identify and correct bad practices/problems in these
     * examples, which might make it harder to determine the cause of an
     * exception, if encountered in production.
     */

    Logger logger;

    /**
     * ExampleA is in a critical code path and should therefore never throw
     * exceptions.
     */
    public void exampleA() {
        A a = new A();
        try {
            a.actionA();
            a.actionD();
            a.actionC();
            a.actionA();
            a.actionD();
            a.actionC();
            a.actionA();
            a.actionD();
            a.actionC();
        } catch (ExceptionA e) {
            System.out.println("A");

        } catch (SomeOtherException e) {
           System.out.println("SomeOtherException" + e);
        }

    }

    public void exampleB(int arg) throws Exception {
        A a = new A();
        try {
            a.actionA();
            a.actionD();
            a.actionC();

            if (arg > 2)
                // Caller has to deal with this
                throw new Exception("Cannot execute with arg > 2");
            a.functionB(arg);
        } finally {
            a.close();
        }
    }

    public void exampleC(A a) {
        try {
            a.actionD();
            int e = a.functionE();
            a.functionD(e);
            a.functionF(213, e);
        } catch (ExceptionA e) {
            // Something about object a is not right, probably an earlier
            // problem
            throw new IllegalStateException("Object a is not ready to be processed", e);
        }
    }

    public void exampleD(A a) {
        try {
            a.actionA();
            a.functionD(a.functionE());
            a.actionA();
        } catch (ExceptionA e) {
            // Fatal problem, some caller has to deal with this
            throw new IllegalStateException("Object a is not ready to be processed", e);
        }
    }

}
