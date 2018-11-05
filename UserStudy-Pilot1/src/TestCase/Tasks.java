package TestCase;
import Environment.*;

public class Tasks {

    // Problem: Cleanup not executed for exception
	public void TaskA() {
		A a = new A();
		try {
			a.actionA();
			a.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Throwable e) {
			
		}
	}

	// Problem: Use after free
	@SuppressWarnings("badcatch")
	public void TaskB() {
		A a = new A();
		try {
			a.actionA();
			try {
				try {
					a.actionB(); a.actionC(); // may contain Nuts // may throw SomeOtherException, VeryNiceException
					a.actionB(); a.actionB(); // may throw VeryNiceException, VeryNiceException
				} catch (VeryNiceException e) {
					throw e;
				} finally {
					System.out.println("Always");
				}
				
				int b = 1;
				try {
					a.functionD(a.functionC(b = 123, a.functionA(a.functionB(22)))); // may throw SomeOtherException, ExceptionA, VeryNiceException
				}
				catch (NumberFormatException e) {
				    
				}
				finally {
					System.out.println("Always");
				}
			} catch (SomeOtherException e2) {
				throw new Exception("Invalid", e2);
			}
			try { a.actionC(); } catch (Exception e) {}
			a.actionB();
			a.actionC();
			a.destroy();
			a.actionA();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Problem: Implicitly catches C as B
	public void TaskC() {
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
		} catch (VeryNiceException e) {
            System.out.println("B");
		}

	}
	
}
