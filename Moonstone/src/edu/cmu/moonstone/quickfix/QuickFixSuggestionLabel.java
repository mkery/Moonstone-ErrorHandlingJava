package edu.cmu.moonstone.quickfix;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;

/** The label to be used for quickfix suggestions in QuickFixDialog
 * 
 * @author Michael Puskas
 */
public class QuickFixSuggestionLabel 
{
	private CLabel suggestionLabel;

	QuickFixSuggestionLabel(Composite parent, String text)
	{
		suggestionLabel = new CLabel(parent, SWT.WRAP);
		suggestionLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		suggestionLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

		FontData fontData = suggestionLabel.getFont().getFontData()[0];
		Font font = new Font(parent.getDisplay(), new FontData(fontData.getName(), fontData
		    .getHeight(), SWT.ITALIC));
		suggestionLabel.setFont(font);

		suggestionLabel.setText(text);
	}
	
	public CLabel getLabel()
	{
		return suggestionLabel;
	}
}
