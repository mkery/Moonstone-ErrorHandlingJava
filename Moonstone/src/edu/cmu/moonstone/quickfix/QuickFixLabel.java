package edu.cmu.moonstone.quickfix;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

/** Creates a clickable label to be used as a button in the QuickFixDialog
 * 
 * @author Michael Puskas
 */
public class QuickFixLabel
{
	private CLabel quickLabel;

	public QuickFixLabel(Composite parent, String text, Image arrow, Color highlightColor)
	{
		this(parent, SWT.NONE, SWT.COLOR_TRANSPARENT, text, arrow, highlightColor);
	}

	public QuickFixLabel(final Composite parent, int style, int color, String text, Image arrow, final Color highlightColor)
	{
		quickLabel = new CLabel(parent, style);
		quickLabel.setBackground(parent.getDisplay().getSystemColor(color));
		quickLabel.setText(text);
		if (arrow != null)
			quickLabel.setImage(arrow);

		quickLabel.addMouseTrackListener(new MouseTrackAdapter()
				{
					@Override
					public void mouseEnter(MouseEvent e)
					{
						quickLabel.setBackground(highlightColor);
					}

					@Override
					public void mouseExit(MouseEvent e)
					{
						quickLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					}
				});
	}

	public CLabel getLabel()
	{
		return quickLabel;
	}
}
