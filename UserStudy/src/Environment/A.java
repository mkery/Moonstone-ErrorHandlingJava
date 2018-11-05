package Environment;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by florian on 11/15/16.
 */
public class A implements AutoCloseable {
	public void destroy() {
		/* some code */
	}

	public void actionA() {
		/* some code */
	}

	public void actionB() throws VeryNiceException {
		/* some code */
	}

	public void actionC() throws SomeOtherException {
		/* some code */
	}

	public void actionD() throws ExceptionA {
		/* some code */
	}

	public int functionA(int arg) {
		return 2 * arg;
	}

	public int functionB(int arg) throws VeryNiceException {
		return 2 * arg;
	}

	public int functionC(int ignored, int arg) throws SomeOtherException {
		return 2 * arg;
	}

	public int functionD(int arg) throws ExceptionA {
		return 2 * arg;
	}

	public int functionE() {
		return 5;
	}

	public int functionF(int ignored, int arg) {
		return 2 * arg;
	}

	@Override
	public void close() {
		/* some code */
	}

	public boolean isConsistent() {
		return false;
	}

	public Collection<C> getAllC() {
		return Collections.emptyList();
	}

}
