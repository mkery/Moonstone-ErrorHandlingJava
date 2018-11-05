package moonstone;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
/***
 * 
 * Work in progress! Not really in use. This is the kind of class where you'd implement auto-complete. TODO
 *
 */
public class CompletionProposalComputer implements IJavaCompletionProposalComputer {
	private boolean debug = false;

	public CompletionProposalComputer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void sessionStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sessionEnded() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext contextIn,
			IProgressMonitor monitor) {		

		if(debug)
			System.out.println("=== Beginning of computeCompletionProposals");
		
		ArrayList<ICompletionProposal> proposals = new ArrayList<>();
		
		//Find the expected type
		//contextIn.
		JavaContentAssistInvocationContext context = (JavaContentAssistInvocationContext) contextIn;
		/*CompletionContext cc = context.getCoreContext();
		String str = new String(cc.getToken());
		System.out.println("CompeltionContext.getToken(): " + str);
		context.getViewer().setTextColor(new Color(null,50,20,254));
		IType expType = context.getExpectedType();		
		if (expType == null) {
			System.out.println("=== The expected type is null");
		}
		else
			System.out.println("=== The expected type is "+expType);*/
		
		ASTNode ast = EditorUtility.parseASTFocused(context.getCompilationUnit(), context.getInvocationOffset());
		//System.out.println("=== Exception check: "+EditorUtility.inException(ast, new Region(context.getInvocationOffset(), context.getInvocationOffset())));

		
		//Grabs the image to display in the content assist box
		URL iconURL = CompletionProposalComputer.class.getResource("/icon/moonstone.png");
		Image moonstoneIcon = (Image) ImageDescriptor.createFromURL(iconURL).createResource(Display.getCurrent());
		
		//This create a small pop-up next to the inserted proposal
		IContextInformation contextInfo = new ContextInformation("contextDisplayString", "message that appears once added to the code");
		
		proposals.add(new CompletionProposal("5; //Here's a comment", context.getInvocationOffset(), 0, "5; //Here's a comment".length(), moonstoneIcon, "text that appears in the content assist", contextInfo, "additional descritpive text"));  

		//Grab all the text to the left of the cursor when the content assist is fired.
		String textToTheLeft = "";
		try {
			textToTheLeft = getCurrentLine(context);
		} catch (Exception e) { //ironic bad error handling
			e.printStackTrace();
		}

		if(debug)
		{
			System.out.println("=== Text to the left of the cursor: " + textToTheLeft);
			System.out.println("=== Returning proposals");
		}
		
		return proposals;
	}

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
			IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}
	
	  
	/** 
	 * Extract text from current line up to the cursor position 
	 *  
	 * @param context 
	 *            content assist context 
	 * @return current line data  
	 * @throws org.eclipse.jface.text.BadLocationException 
	 */  
	protected String getCurrentLine(final ContentAssistInvocationContext context) throws org.eclipse.jface.text.BadLocationException {  
		IDocument document = context.getDocument();  
		int lineNumber = document.getLineOfOffset(context.getInvocationOffset());  
		IRegion lineInformation = document.getLineInformation(lineNumber);  
	
		return document.get(lineInformation.getOffset(), context.getInvocationOffset() - lineInformation.getOffset());  
	}  

}
