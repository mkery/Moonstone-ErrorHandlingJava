import Environment.*;

public class Task_G {

    /**
     * Labell all places where control flow might diverge with A, B.
     */
    // TODO: Example

    void calculate(A a) throws CalculationException {
        int value = 0;
        if (a.functionE() > 10) {
            try {
                for (C c : a.getAllC()) {
                    try {
                        a.actionA();
                        value += c.evaluate(a);
                        a.actionC();
                        //A
                        a.actionD();
                        //B
                        value += c.evaluate(a);
                    } // B
                    catch (ExceptionA e) {
                        throw new SomeOtherException(e);
                    }
                }
            } //A
            catch (VeryNiceException e) {
                throw new CalculationException("Calculation failed", e);
            }
        } else {
            throw new CalculationException("Precondition not met", null);
        }
    }

}
