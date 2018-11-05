package moonstone;

import org.eclipse.swt.widgets.Composite;

/**
 * Created by florian on 1/16/17.
 */
public abstract class AbstractComposite extends Composite {

    protected ParentDelegate parentDelegate;

    public AbstractComposite(int style, ParentDelegate parentDelegate) {
        super(parentDelegate.getParent(), style);
        this.parentDelegate = parentDelegate;
    }

    public void setParentDelegate(ParentDelegate parentDelegate) {
        this.parentDelegate = parentDelegate;
        setParent(parentDelegate.getParent());
        requestLayout();
    }
}
