package c_Understanding;

import Environment.*;

/**
 * Your team thinks this code's control flow is hard to understand. Your boss
 * assigns you to make sense of the following method.
 *
 * Label all places where the execution flow might jump DUE TO AN exception with
 * “// can jump to line XX” or “// leaves method”.
 */

public class Calculator {

	int calculate(A a, boolean example) throws CalculationException {
		int value = 0;

		if (example) // Place comments like so:
			throw new AssertionError("Example"); // can jump to line 42

		if (a.functionE() > 10) {
			try {
				for (C c : a.getAllC()) {
					try {
						a.actionA();
						value += c.evaluate(a);
						a.actionC();
						a.actionD();
						value += c.evaluate(a);
					} catch (ExceptionA e) {
						throw new SomeOtherException(e);
					}
				}
			} catch (VeryNiceException e) {
				throw new CalculationException("Calculation failed", e);
			}
		} else {
			throw new CalculationException("Precondition not met", null);
		}

		return value;
	}

}
