package edu.cmu.moonstone.ghostcomment;

import edu.cmu.moonstone.markers.MarkerDef;
import edu.cmu.moonstone.markers.MyMarkerFactory;
import moonstone.ASTStatementVisitor;
import moonstone.AbstractComposite;
import moonstone.EditorUtility;
import moonstone.ParentDelegate;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The top-level dialog for our ghost comment implementation.
 * 
 * @author Michael Puskas
 */
public class GhostCommentControl extends AbstractComposite
{
	private final IDocument doc;
	
	private final Color highlightBlue;
	private final Color darkBlue;
	private final Color red;
	private final Composite skippedLabel;

	private final Map<Annotation, Position> highlights = new HashMap<>();

	private final CommentInfo commentInfo;
	private final ExceptionInfo exceptionInfo;
	private EditorDelegate editorDelegate;

	private final HighlightListener highlightListener;
	private final IAnnotationModelExtension modelEx;

	public GhostCommentControl(IEditorPart editorPart
			, CommentInfo commentInfo, ExceptionInfo exceptionInfo
			, ParentDelegate parentDelegate, EditorDelegate editorDelegate)
	{
		super(SWT.NONE, parentDelegate);
		setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = layout.verticalSpacing = 0;
		setLayout(layout);

		this.editorDelegate = editorDelegate;

		this.commentInfo = commentInfo;
		this.exceptionInfo = exceptionInfo;

		//Init colors
		darkBlue = new Color(getDisplay(), 128, 179, 196);
		red = new Color(getDisplay(), 251, 206, 177);
	    highlightBlue = new Color(getDisplay(), 219, 234, 241);

		//Initialize the annotation model
		ITextEditor editor = editorPart.getAdapter(ITextEditor.class);
		IDocumentProvider idp = editor.getDocumentProvider();
		IEditorInput editorInput = editor.getEditorInput();
		doc = idp.getDocument(editorInput);
		IAnnotationModel model = idp.getAnnotationModel(editorInput);
		modelEx = (IAnnotationModelExtension) model;

		//Remove potentially stale highlights
		MyMarkerFactory.removeStaleHighlights(model);

		//Retrieve the title text
		String titleText = getTitleText(exceptionInfo);

		//Create the title
		createTitle(this, titleText);

		//TODO: Hack / Quick and dirty
		if (exceptionInfo.getThrowSite().getNodeType() != ASTNode.TRY_STATEMENT) {
			//Create the accept button
			createAcceptButton(this);
		
			//Create the label with the red background
			skippedLabel = createSkippedLabel(this);
		} else skippedLabel = null;

		//Create the ScrolledComposite with the blue background
		createCaughtComp(this);

		// Register mouse events
		highlightListener = new HighlightListener();
		addHighlightListener(this);
	}
	
	public void addBrowser() {
		//Create the bottom browser that holds the javadoc info
		createBrowser(this);
	}

	private String getTitleText(ExceptionInfo exceptionInfo) {
		//Set up a list to hold the method declarations
		final String[] titleText = new String[] { null };

		//Visit the statement and add any MethodDeclarations in it
		this.exceptionInfo.getThrowSite().accept(new ASTVisitor()
		{
			@Override
			public boolean visit(MethodInvocation invok)
			{
				IMethodBinding methodBinding = invok.resolveMethodBinding();
				if(methodBinding != null)
					titleText[0] = String.format("When %s() throws %s:", methodBinding.getName(), exceptionInfo.getException().getName());

				return false;
			}

			@Override
			public boolean visit(ClassInstanceCreation create)
			{
				IMethodBinding constructorBinding = create.resolveConstructorBinding();
				if(constructorBinding != null)
					titleText[0] = String.format("When new %s() throws %s:", constructorBinding.getName(), exceptionInfo.getException().getName());

				return false;
			}

			@Override
			public boolean visit(ThrowStatement node) {
				if (node.getExpression().getNodeType() != ASTNode.SIMPLE_NAME) return false;
				SimpleName name = (SimpleName) node.getExpression();
				IBinding binding = name.resolveBinding();
				if (binding == null) return false;

				EditorUtility.acceptParents(exceptionInfo.getThrowSite(), new ASTStatementVisitor() {
					@Override
					public boolean visit(CatchClause catchClause) {
						if (!binding.isEqualTo(catchClause.getException().resolveBinding())) return true;
						titleText[0] = String.format("When %s %s is rethrown:", exceptionInfo.getException().getName(), binding.getName());
						return false;
					}
				});

				return false;
			}

			@Override
			public boolean visit(TryStatement node) {
				if (node.resources().isEmpty()) throw new AssertionError();
				titleText[0] = "When control leaves the body of this try-with-resources block,\nall resources are automatically cleaned up:";
				return false;
			}
		});
		return titleText[0] != null ? titleText[0] : String.format("When %s is thrown:", exceptionInfo.getException().getName());
	}

	private void addHighlightListener(Control control) {
		control.addMouseTrackListener(highlightListener);
		if (control instanceof Composite)
			for (Control child : ((Composite) control).getChildren())
				addHighlightListener(child);
	}

	private void removeHighlightListener(Control control) {
		if (control instanceof Composite)
			for (Control child : ((Composite) control).getChildren())
				removeHighlightListener(child);
		control.removeMouseTrackListener(highlightListener);
	}

	private void setCollapsed(Composite comp, boolean collapsed) {
		if (comp == null) return;
		GridData layoutData = (GridData) comp.getLayoutData();
		layoutData.exclude = collapsed;
		comp.setVisible(!collapsed);
		comp.requestLayout();
	}

	public void focus() {
		if (!highlights.isEmpty()) return;

		setCollapsed(skippedLabel, false);
		//setCollapsed(caughtComp, false);

		//Build the throw site hightlight
		createThrowSiteHighlight();

		//Build the exceptionInfoList
		createBlueHighlights();

		//Add the red and blue highlights
		createRedHighlights();

		//Add highlights to model
		modelEx.replaceAnnotations(null, highlights);
	}

	public void unfocus() {
		highlightListener.entered = false;
		if (highlights.isEmpty()) return;

		setCollapsed(skippedLabel, true);
		//setCollapsed(caughtComp, true);

		//Remove the highlights
		Annotation[] annotations = highlights.keySet().toArray(new Annotation[highlights.size()]);
		modelEx.replaceAnnotations(annotations, null);
		highlights.clear();
	}

	/** Creates the title label
	 * 
	 * @param container
	 * @param titleText
	 * @return Returns the label
	 */
	private void createTitle(Composite container, String titleText)
	{
		//Create the title label
		CLabel title = new CLabel(container, SWT.NONE);
		title.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		//Set the content of the title
		title.setText(titleText);
		
		setTitleLook(title);
	}

	/** Sets up the layout and look of the title
	 * 
	 * @param title
	 */
	private void setTitleLook(CLabel title)
	{
		//Create the formlayout data for the top left title
		title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		//Set the title to be bold
		FontData fontData = title.getFont().getFontData()[0];
		Font font = new Font(title.getDisplay(), new FontData(fontData.getName(), fontData
		    .getHeight(), SWT.BOLD));
		title.setFont(font);
	}
	
	/** Creates the "Accept Comment" label, which acts as a button
	 * 
	 * @param container
	 */
	private void createAcceptButton(Composite container)
	{
		if (!editorDelegate.allowAcceptComment() || !commentInfo.hasGhost()) return;

		//Add button for accepting the comment
		Button acceptButton = new Button(container, SWT.NONE);
		//acceptButton.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		acceptButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		
		//Set the text and hover text for the label
		acceptButton.setText("Persist comment");
		acceptButton.setToolTipText("Makes this comment permanent");
		acceptButton.setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));

		acceptButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ev) {
				super.widgetSelected(ev);

				int startOffset = commentInfo.getCommentOffset();
				try {
					doc.replace(startOffset, 0, commentInfo.getText());
				} catch (BadLocationException e) {
					throw new RuntimeException(e);
				}

				editorDelegate.acceptedComment(commentInfo);
				parentDelegate.close();
			}
		});
	}

	/** Creates the "Then the highlighted code is skipped" label
	 * 
	 * @param container
	 * @return Returns the label
	 */
	private Composite createSkippedLabel(Composite container)
	{
		Composite comp = new Composite(container, SWT.NONE);
		comp.setLayoutData(new GridData());
		comp.setBackground(red);
		comp.setLayout(new GridLayout());
		setCaughtCompLook(comp);

		//Create the skippedLabel
		CLabel skippedLabel = new CLabel(comp, SWT.NONE);
		skippedLabel.setBackground(red);

		//Set the content of the skippedLabel
		skippedLabel.setText("The highlighted code is skipped");
		skippedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		setCollapsed(comp, true);
		return comp;
	}


	/** Creates the "Then exception is caught on line x" label
	 * 
	 * @param container
	 * @return Returns the label
	 */
	private Composite createCaughtComp(Composite container)
	{
		Composite comp = new Composite(container, SWT.NONE);
		comp.setLayoutData(new GridData());
		comp.setBackground(darkBlue);
		comp.setLayout(new GridLayout());
		setCaughtCompLook(comp);

		int i = 0;
		for (ExceptionInfo.FlowStep flowStep : exceptionInfo.getFlowSteps()) {
			createCaughtLabel(comp, flowStep.getLineNumber(), String.format("%d.  %s", ++i, flowStep))
					.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

	    //setCollapsed(comp, true);
		return comp;
	}

	private Label createCaughtLabel(Composite child, final int lineNumber, String caughtText) {
		boolean jumpEnabled = lineNumber >= 0;
		Label caughtLabel = new CaughtLabel(child, caughtText, jumpEnabled ? highlightBlue : darkBlue, darkBlue).getLabel();
		if (jumpEnabled)
			caughtLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent event) //GoTo, close window
				{
					super.mouseUp(event);
	
					try {
						/* GoTo the lineNumber */
						//Get the offset and len of the line number
						IRegion lineInformation = doc.getLineInformation(lineNumber);
						editorDelegate.jump(lineInformation);
						parentDelegate.close();
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			});
		return caughtLabel;
	}

	/** Sets up the layout and look of the caughtComp
	 *
	 * @param caughtComp
	 */
	private void setCaughtCompLook(Control caughtComp)
	{
		//Create the formlayout data for the caughtComp
		caughtComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
	}

	/** Creates the text box on the bottom of the dialog
	 *
	 * @param container
	 */
	@SuppressWarnings({ "restriction", "deprecation" })
	private Browser createBrowser(Composite container)
	{
		//Create the browser on the bottom
		try
		{
			JavadocHover javadocHover = new JavadocHover();
			javadocHover.setEditor(editorDelegate.getEditor());
			
			ASTNode call = exceptionInfo.getCall();
			if (call == null) return null;
			IRegion region = javadocHover.getHoverRegion(getTextViewer(), call.getStartPosition());
			if (region == null) return null;		
			String hoverInfo = javadocHover.getHoverInfo(getTextViewer(), region);
			if (hoverInfo == null) return null;
			
			StringBuilder body = new StringBuilder(hoverInfo);
			int styleEnd = body.indexOf("</style>");
			body.insert(styleEnd, "body { background: #FFFFFF; }"); //overflow:hidden;

			//Set the browser to display the body text
			Browser browser = new Browser(container, SWT.NONE);
			browser.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
			browser.setText(body.toString());

			//Create the formlayout data for the browser
			browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			return browser;
		}
		catch(SWTError e)// | JavaModelException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String getNodeDebugString(ASTNode node) {
		return node.getClass().getSimpleName() + ": " + node.toString().replace(System.lineSeparator(), "");
	}

	/** Creates a red highlight for skipped line highlighting
	 */
	private void createRedHighlights()
	{
		/* Draw the red skipped line highlight */
		ASTNode throwSite = exceptionInfo.getThrowSite();
		
		//TODO: Hack / Quick and dirty
		if (throwSite.getNodeType() == ASTNode.TRY_STATEMENT) return;
		
		//Find the starting position
		int skippedStart = throwSite.getStartPosition() + throwSite.getLength();
		//Determine statement end
		StatementEndSearch skip = new StatementEndSearch(skippedStart).perform();

		ASTNode rootNode = EditorUtility.inMethodDeclaration(throwSite);
		if (rootNode == null) rootNode = throwSite.getRoot();

		//Get positions of skipped calls
		List<Position> skippedPositions = getSkippedCalls(rootNode, throwSite, skip.getStatementEnd());

		if (skip.wasEndOfLine()) {
			skippedStart++;
			/*ASTNode nextStatement = getNextStatement(throwSite);
			if (nextStatement != null) skippedStart = nextStatement.getStartPosition();*/
		}

		int endOffset = exceptionInfo.getCaughtStep().getBlock().getOffset();
		getSkippedPositions(rootNode, skippedStart, endOffset).forEach(skippedPositions::add);

		createRedHighlight(skippedPositions);
	}

	private Stream<Position> getSkippedPositions(ASTNode rootNode, int skippedStart, int skippedEnd) {
		SkipMap visitor = new SkipMap(skippedStart, skippedEnd);
		for (ExceptionInfo.FlowStep flowStep : exceptionInfo.getFlowSteps()) {
			Position position = flowStep.getBlock();
			visitor.excludeSkip(position.getOffset(), position.getOffset() + position.getLength());
		}
		//outerNode.accept(visitor);

		List<Comment> commentList = ((CompilationUnit) rootNode.getRoot()).getCommentList();
		for (Comment comment : commentList) {
			int startPosition = comment.getStartPosition();
			visitor.excludeSkip(startPosition, startPosition + comment.getLength());
		}
		
		visitor.excludeLinebreaks();
		return visitor.getPositions();
	}

	/** Determines skipped parts of the doc */
	private class SkipMap {
		private final TreeMap<Integer, Boolean> skipMap = new TreeMap<>();

		public SkipMap(int skippedStart, int skippedEnd) {
			// Trim whitespace
			try {
				while (skippedStart < skippedEnd && Character.isWhitespace(doc.getChar(skippedStart))) skippedStart++;
				while (skippedStart < skippedEnd && Character.isWhitespace(doc.getChar(skippedEnd - 1))) skippedEnd--;
			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}

			if (skippedStart < skippedEnd) {
				skipMap.put(skippedStart, true);
				skipMap.put(skippedEnd, false);
			}
		}

		public void excludeSkip(int start, int end) {
			// Expand to include as much whitespace as possible
			try {
				int docLength = doc.getLength();
				while (0 < start && Character.isWhitespace(doc.getChar(start - 1))) start--;
				while (end < docLength && Character.isWhitespace(doc.getChar(end))) end++;
			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
			
			if (start >= end) return;

			Entry<Integer, Boolean> beforeEntry = skipMap.lowerEntry(start);
			boolean anchorLeft = beforeEntry != null && beforeEntry.getValue();
			Entry<Integer, Boolean> afterEntry = skipMap.floorEntry(end);
			boolean anchorRight = afterEntry != null && afterEntry.getValue();
			
			skipMap.subMap(start, true, end, true).clear();
			
			if (anchorLeft) { 
				skipMap.put(start, false); 
			}
			if (anchorRight) {
				skipMap.put(end, true);
			}
		}
		
		public void excludeLinebreaks() {
			try {
				List<Position> positions = getPositions().collect(Collectors.toList());
				for (Position position : positions) {
					int startOffset = position.getOffset();
					int endOffset = startOffset + position.getLength();
					for (int i = startOffset; i < endOffset; i++) {
						if (doc.getChar(i) == '\n') excludeSkip(i, i);
					}
				}
			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
		}

		public Stream<Position> getPositions() {
			return skipMap.entrySet().stream()
					.filter(Map.Entry::getValue)
					.map(entry -> new Position(entry.getKey(), skipMap.higherKey(entry.getKey()) - entry.getKey()));
		}
	}

	private class StatementEndSearch {
		private int offset;
		private boolean endOfLine = true;
		private int statementEnd;

		public StatementEndSearch(int offset) {
			this.offset = offset;
		}

		public boolean wasEndOfLine() {
			return endOfLine;
		}

		public int getStatementEnd() {
			return statementEnd;
		}

		public StatementEndSearch perform() {
			try {
				int docLength = doc.getLength();
				for (statementEnd = offset; statementEnd < docLength; statementEnd++) {
					char currentChar = doc.getChar(statementEnd);
					if (currentChar == ';') break;
					endOfLine = endOfLine && Character.isWhitespace(currentChar);
				}
			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
			return this;
		}
	}

	private List<Position> getSkippedCalls(ASTNode rootNode, final ASTNode throwSite, int statementEnd) {
		//TODO: Hack / Quick and dirty
		if (throwSite.getNodeType() == ASTNode.TRY_STATEMENT) return Collections.emptyList();
		
		
		final int throwSiteEnd = throwSite.getStartPosition() + throwSite.getLength();
		final List<Position> skippedPositions = new ArrayList<>();

		try {
			System.out.println("Looking for node in: " + doc.get(throwSiteEnd, statementEnd - throwSiteEnd));
		} catch (BadLocationException e) {
		}

		NodeFinder nodeFinder = new NodeFinder(rootNode, throwSiteEnd, statementEnd - throwSiteEnd);
		ASTNode outerNode = nodeFinder.getCoveringNode();
		System.out.println("Found node: " + getNodeDebugString(outerNode));

		EditorUtility.acceptParents(throwSite.getParent(), new ASTStatementVisitor() {
			private ASTNode lastSeen = throwSite;

			@Override
			public boolean preVisit2(ASTNode node) {
				boolean visit = lastSeen != outerNode;
				lastSeen = node;
				if (!visit) System.out.println("Reached outer node, no more skipped calls");
				return visit;
			}

			@Override
			public boolean visit(ClassInstanceCreation node) {
				markSkippedCall(node, node.arguments());
				return true;
			}

			@Override
			public boolean visit(MethodInvocation node) {
				markSkippedCall(node, node.arguments());
				return true;
			}

			private void markSkippedCall(ASTNode node, List<ASTNode> args) {
				args.stream().findFirst().ifPresent(arg -> {
					try {
						int startPosition = node.getStartPosition();
						for (int i = arg.getStartPosition(); i > startPosition; i--) {
							if (doc.getChar(i) == '(') {
								if (i < throwSiteEnd) {
									int length = i + 1 - startPosition;
									System.out.println("Marking call as skipped: " + doc.get(startPosition, length));
									skippedPositions.add(new Position(startPosition, length));
								}
								break;
							}
						}
					} catch (BadLocationException e) {
						throw new RuntimeException(e);
					}
				});
			}
		});

		return skippedPositions;
	}

	private void createRedHighlight(List<Position> skippedPositions) {
		//Create the annotations
		skippedPositions.forEach(position ->
				highlights.put(new Annotation(MarkerDef.RED_HIGHLIGHT, false, ""), position));
	}


    private void createThrowSiteHighlight() {
        /* Draw the blue relevant catch highlight */
    	
    	ASTNode throwSite = exceptionInfo.getThrowSite();
		//TODO: Hack / Quick and dirty
		if (throwSite.getNodeType() == ASTNode.TRY_STATEMENT) return;
    	
        //Create the annotation
        Annotation blueHighlight = new Annotation(MarkerDef.THROWSITE_HIGHLIGHT, false, "");

        //Add the highlight annotation to the model and list
		Position throwPosition = new Position(throwSite.getStartPosition(),
                                              throwSite.getLength());
        highlights.put(blueHighlight, throwPosition);
    }

	/** Creates a blue highlight for relevant catch highlighting 
	 *  Adds the Annotation object to blueHighlights
	 * 
	 * @return The Annotation object associated with the blue highlight
	 */
	private void createBlueHighlights()
	{
		//Find the appropriate position
		exceptionInfo.getFlowSteps().stream()
				.map(ExceptionInfo.FlowStep::getHighlight)
				.filter(Objects::nonNull)
				.forEach(this::createBlueHighlight);
	}

	private void createBlueHighlight(Position linePos) {
    	/* Draw the blue relevant catch highlight */
		//Create the annotation
		Annotation blueHighlight = new Annotation(MarkerDef.BLUE_HIGHLIGHT, false, "");

		//Add the highlight annotation to the model and list
		highlights.put(blueHighlight, linePos);
	}

	void onClose()
	{
		removeHighlightListener(this);
		unfocus();
	}

	@Override
	public void dispose() {
		//Dispose of the colors
		darkBlue.dispose();
		red.dispose();
		super.dispose();
	}

	private class HighlightListener extends MouseTrackAdapter {
		public boolean entered = false;

		@Override
        public void mouseEnter(MouseEvent e) {
            if (!entered) {
                entered = true;
                focus();
                parentDelegate.onFocus(GhostCommentControl.this);
                getShell().pack(false);
            }
        }
	}

	public interface EditorDelegate {
		boolean allowAcceptComment();
		void acceptedComment(CommentInfo commentInfo);
		void jump(IRegion region);
		IEditorPart getEditor();
	}

	ITextViewer getTextViewer() {
		ITextOperationTarget target = editorDelegate.getEditor().getAdapter(ITextOperationTarget.class);
		return (ITextViewer) target;
	}

	public Collection<Position> getHightlightPositions() {
		return highlights.values();
	}
}
