package Tutorial;

import Environment.*;

public class Tasks {

	public void TaskA() {
		A a = new A();
		try {
			a.actionA();
			a.actionB();
			a.actionC();
			a.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Throwable e) {

		} finally {
		    System.out.println("finally");
		}
	}

}
