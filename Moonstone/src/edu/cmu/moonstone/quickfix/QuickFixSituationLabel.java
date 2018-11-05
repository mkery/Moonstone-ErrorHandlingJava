package edu.cmu.moonstone.quickfix;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/** The label to be used for situations that are displayed in BadCatchDialog
 * 
 * @author Michael Puskas
 */
public class QuickFixSituationLabel 
{
	private Label situationLabel;

	QuickFixSituationLabel(Composite parent, String text)
	{
		situationLabel = new Label(parent, SWT.WRAP);
		situationLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		situationLabel.setText(text);
		situationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}
	
	public Label getLabel()
	{
		return situationLabel;
	}
}
