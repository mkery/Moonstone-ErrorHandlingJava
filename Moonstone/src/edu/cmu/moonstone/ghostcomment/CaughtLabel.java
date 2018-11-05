package edu.cmu.moonstone.ghostcomment;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/** The label to be used for the caught exceptions displayed in the blue section of a GhostDialog
 * 
 * @author Michael Puskas
 */
public class CaughtLabel 
{
	private Label caughtLabel;

	CaughtLabel(Composite parent, String caughtText, Color highlightBlue, Color darkBlue)
	{
		caughtLabel = new Label(parent, SWT.WRAP);
		caughtLabel.setBackground(darkBlue);
		caughtLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true));
		caughtLabel.setText(caughtText);
		
		if (highlightBlue != darkBlue)
			caughtLabel.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseEnter(MouseEvent e) {
					if (!highlightBlue.isDisposed()) caughtLabel.setBackground(highlightBlue);
				}
	
				@Override
				public void mouseExit(MouseEvent e) {
					if (!darkBlue.isDisposed()) caughtLabel.setBackground(darkBlue);
				}
			});
	}
	
	public Label getLabel()
	{
		return caughtLabel;
	}
}
