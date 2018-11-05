package moonstone;

import org.eclipse.swt.widgets.Composite;

/**
 * Created by florian on 1/16/17.
 */
public interface ParentDelegate {
    Composite getParent();

    void onFocus(Composite sender);

    boolean close();
}
