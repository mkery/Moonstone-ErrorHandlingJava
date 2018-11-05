package Environment;

import java.util.Collections;
import java.util.List;

/**
 * Created by florian on 11/15/16.
 */
public class Connection implements AutoCloseable {
	public List<JSON> retrieveData(Parameter parameter) throws NotFoundException {
		return Collections.emptyList();
	}

	public void close() {
		/* some code */
	}

	public void setTimeout(int interval) {
		/* some code */
	}
}
