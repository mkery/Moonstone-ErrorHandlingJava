package edu.cmu.moonstone.view;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class CustomView extends ViewPart {

	public static final String ID = "moonstone.CustomView";

	private StyledText viewedText = null;
	private StyleRange viewedStyle = new StyleRange();
	private List<StyledText> rows = new ArrayList<>();
	private Map<StyledText, Boolean> styles = new LinkedHashMap<>();
	ScrolledComposite sc;
	Composite par;
	private StyledText titleText;
	private List<CatchExample[]> examples;

	@Override
	public void createPartControl(Composite parent) {
		//Set the parent to our static par variable
		par = parent;
		
		//Setup parent composite layout, this is basically our entire view real estate
		GridLayout gridLayoutParent = new GridLayout();
		gridLayoutParent.numColumns = 1;
		par.setLayout(gridLayoutParent);

		//Grey "title" bar to show underneath the view tab
		GridData gridDataTitleComposite = new GridData(SWT.FILL, SWT.CENTER, true, false);
		Composite titleComposite = new Composite(parent, SWT.NO_FOCUS);
		titleComposite.setLayoutData(gridDataTitleComposite);
		titleComposite.setLayout(new RowLayout());
		titleText = new StyledText(titleComposite, SWT.NONE);
		titleText.setEnabled(false);
		titleText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		addDivider(par);
		
		setupSearchBar(par);
	    
		//The scrolled composite (sc) holds the row composite, allowing the user to vertically scroll it
		sc = new ScrolledComposite(par, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gridDataSC = new GridData();
		gridDataSC.horizontalAlignment = GridData.FILL;
		gridDataSC.verticalAlignment = GridData.FILL;
		gridDataSC.grabExcessHorizontalSpace = true;
		gridDataSC.grabExcessVerticalSpace = true;
		sc.setLayoutData(gridDataSC);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
	}
	
	//Grey "title" bar to show underneath the view tab
	public void setupTitleDescription(int exampleCount, String exceptionType, int classCount) {
		titleText.setText("Viewing " + exampleCount + " " + exceptionType + " from " + classCount + " different files!");
		titleText.requestLayout();
	}

	private void addDivider(Composite parent) {
		Composite testC = new Composite(parent, SWT.NONE);
		GridData testGD = new GridData(SWT.FILL, SWT.FILL, true, false);
		testGD.horizontalSpan = 3;
		testGD.heightHint = 3;
		testGD.minimumHeight = 3;
		testC.setBackground(new Color(null, 54, 54, 54));
		testC.setLayoutData(testGD);
	}
	
	private void setupSearchBar(Composite parent) {
		GridData gridDataSearchComposite = new GridData(SWT.FILL, SWT.CENTER, true, false);

		Composite searchComposite = new Composite(parent, SWT.NONE);
		searchComposite.setLayoutData(gridDataSearchComposite);
		searchComposite.setLayout(new RowLayout());
		
		final Text searchTextBox = new Text(searchComposite, SWT.SEARCH);
		if ((searchTextBox.getStyle() & SWT.ICON_CANCEL) == 0) {
			URL searchIconURL = CustomView.class.getResource("/icon/search.png");
			Image searchImage = (Image) ImageDescriptor.createFromURL(searchIconURL).createResource(parent.getDisplay());
			ToolBar toolBar = new ToolBar(searchComposite, SWT.FLAT);
			
			ToolItem searchItem = new ToolItem(toolBar, SWT.PUSH);
			searchImage.setBackground(new Color(null, 255, 255, 255));
			searchItem.setImage(searchImage);
			searchItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					search(searchTextBox, sc);
				}
			});
		}

		searchTextBox.setLayoutData(new RowData(300, 20));
		searchTextBox.setMessage("Enter search here");
		searchTextBox.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.keyCode != SWT.CR) {
					System.out.println("Typed something");
					removeStyles();
				}
		    }
		});
		searchTextBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (e.detail == SWT.CANCEL) {
					System.out.println("Search cancelled");
				} else {
					search(searchTextBox, sc);
				}
			}
		});
		
	    /*Combo sortSelectionCombo = new Combo(searchComposite, SWT.DROP_DOWN | SWT.BORDER); //SWT.READ_ONLY makes it so they can't type, but it greys it
	    sortSelectionCombo.add("Frequency");
	    sortSelectionCombo.add("Relevance");
	    sortSelectionCombo.select(0);
	    sortSelectionCombo.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent e) {
	    		String sortSelectionText = sortSelectionCombo.getText();
	            if (sortSelectionText.equals("Frequency")) {
	            	System.out.println("Frequency");
	            } else if (sortSelectionText.equals("Relevance")) {
	            	System.out.println("Relevance");
	            } else {
	            	//Well, this shouldn't happen technically
	            }
	          }
	        });*/
	}

	private List<int[]> selectionIndexes = new ArrayList<>();

	private void addRow(Composite parent, CatchExample[] examples) {
		String className = examples[0].getTypeName();
		String methodName = examples[0].getMethods().get(0);
		ASTNode code = examples[0].getCode().get(0);

		//Add a thin line between each row
		addDivider(parent);
	
		// The catch example on the left, showing the first one of the set, because
		// they're generalized to be the same. This is easy to change

		StyledText exampleText = new StyledText(parent, SWT.WRAP);
		exampleText.setEditable(false);
		exampleText.setText(className + "." + methodName);
		exampleText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		exampleText.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		
		Button copyButton = new Button(parent, SWT.PUSH);
		copyButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY));
		copyButton.setText("Copy to clipboard");
		copyButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

		// The TreeView with the class and accompanying methods on the right
		makeTree(parent, examples);

		try {
			CompilationUnit cu = (CompilationUnit) code.getRoot();
			ICompilationUnit icu = (ICompilationUnit) cu.getJavaElement();
			Document document = new Document(icu.getSource());

			SourceViewer viewer = createViewer(parent, document, code);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true, 2, 1));

			StyledText st = viewer.getTextWidget();
			st.setCaret(null); // hide caret
			rows.add(st);

			Block block = code.getNodeType() == ASTNode.CATCH_CLAUSE ? ((CatchClause) code).getBody() : (Block)code;
			List<Statement> statements = block.statements();
			int x, y;
			if (statements.size() > 0) {
				int offset = document.getLineOffset(document.getLineOfOffset(code.getStartPosition()));
				x = statements.get(0).getStartPosition() - offset;
				Statement lastStatement = statements.get(statements.size() - 1);
				y = lastStatement.getStartPosition() + lastStatement.getLength() - offset;
			} else {
				x = y = 0;
			}
			selectionIndexes.add(new int[]{ x, y });

			copyButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (y > 0) st.setSelection(x, y);
					else st.selectAll();
					st.copy();
					st.setSelection(x);
				}
			});

			st.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					focus(st, x, y);
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}

		//Set caret null this will hide caret if desired
	    //styledTextWidget.setCaret(null);
		    
		//Point parentCompositeSize = parent.getShell().getSize();
		//Point estimatedExampleSize = exampleText.computeSize(parentCompositeSize.x, SWT.DEFAULT);
	}

	private SourceViewer createViewer(Composite parent, Document document, ASTNode nodeToShow) throws JavaModelException {
		IPreferenceStore store = EditorsUI.getPreferenceStore();

		JavaTextTools javaTextTools = new JavaTextTools(store);
		IDocumentPartitioner partitioner = javaTextTools.createDocumentPartitioner();
		document.setDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING, partitioner);
		partitioner.connect(document);

		SourceViewer viewer = new SourceViewer(parent, null, null, false, SWT.READ_ONLY);
		viewer.configure(new JavaSourceViewerConfiguration(JavaUI.getColorManager(), store, null, IJavaPartitions.JAVA_PARTITIONING));
		viewer.setDocument(document, nodeToShow.getStartPosition(), nodeToShow.getLength());
        viewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
        return viewer;
	}
	
	private void makeTree(Composite parent, CatchExample[] examples) {
		Tree tree = new Tree(parent, SWT.V_SCROLL | SWT.NO_FOCUS);
		tree.setHeaderVisible(true);
		TreeColumn column = new TreeColumn(tree, SWT.NONE);

		int exampleCount = 0;
		for (CatchExample ce : examples) {
			TreeItem item = new TreeItem(tree, SWT.NONE);
			item.setText(ce.getTypeName());
			item.setData(ce);
			boolean expanded = exampleCount == 0;
			exampleCount += ce.getExampleCount();

			List<String> methods = ce.getMethods();
			List<ASTNode> code = ce.getCode();
			for (int i = 0; i < methods.size(); i++) {
				TreeItem subItem = new TreeItem(item, SWT.NONE);
				subItem.setText(methods.get(i));
				subItem.setData(code.get(i));
			}
			item.setExpanded(expanded);
		}

		column.setText(exampleCount + " " + examples[0].getExceptionType() + " instances match this");
		column.pack();

		GridData gridDataTree = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
		gridDataTree.verticalSpan = 2;
		tree.setLayoutData(gridDataTree);
		tree.pack();

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent ev) {
				TreeItem ti = tree.getItem(new Point(ev.x, ev.y));
				TreeItem parentItem = ti.getParentItem();
				if (parentItem == null) return;

				Object data = ti.getData();
				if (!(data instanceof ASTNode)) return;
				ASTNode node = (ASTNode) data;

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

				CompilationUnit cu = (CompilationUnit) node.getRoot();
				IFile file = (IFile) cu.getJavaElement().getResource();

				try {
                    ITextEditor editor = (ITextEditor) IDE.openEditor(page, file, true);
                    editor.selectAndReveal(node.getStartPosition(), node.getLength());
                } catch (PartInitException e) {
                    e.printStackTrace();
                }
			}
		});
		
		//Setup menu items for right clicking on the tree rows
		final Menu menu = new Menu(tree);		
	    tree.setMenu(menu);
	    menu.addMenuListener(new MenuAdapter()
	    {
	        public void menuShown(MenuEvent e)
	        {
	            MenuItem[] items = menu.getItems();
				for (MenuItem item : items) {
					item.dispose();
				}
	            MenuItem newItem = new MenuItem(menu, SWT.NONE);
	            newItem.setText("Menu for " + tree.getSelection()[0].getText());
	        }
	    });

	}
	
	//The styling for search is a little strange atm, I'll need to clean it up
	private void search(Text text, ScrolledComposite parent) {	
		String searchText = text.getText();
		
		if(viewedText != null) {
			viewedStyle.fontStyle = SWT.NORMAL;
			viewedText.setStyleRange(viewedStyle);
			viewedText = null;
		}

		if (searchText == null || searchText.isEmpty()) return;
		if (styles.isEmpty()) {
            for (StyledText exampleTextControl : rows) {
				int start = StringUtils.indexOfIgnoreCase(exampleTextControl.getText(), searchText);
                if (start >= 0) {
                    int len = searchText.length();
                    int end = start + len;

                    exampleTextControl.setSelection(start, end);
                    styles.put(exampleTextControl, false);
					viewedText = styles.keySet().stream().findFirst().get();
                }
            }
        } else {
            for (Map.Entry<StyledText, Boolean> entry : styles.entrySet()) {
                boolean visited = entry.getValue();
                if(!visited) {
                    StyledText controlToScrollTo = entry.getKey();
                    entry.setValue(true);
                    viewedText = controlToScrollTo;
                    break;
                }
            }

            if(!styles.values().contains(false)) {
                for (Map.Entry<StyledText, Boolean> entry : styles.entrySet()) {
                    entry.setValue(false);
                }
            }
        }

		if (viewedText != null) {
            parent.setOrigin(viewedText.getLocation());
            viewedStyle.start = StringUtils.indexOfIgnoreCase(viewedText.getText(), searchText);
            viewedStyle.length = searchText.length();
            viewedStyle.fontStyle = SWT.BOLD;
            viewedText.setStyleRange(viewedStyle);
        }
	}

	private void removeStyles() {
		if(viewedText != null) {
			viewedStyle.fontStyle = SWT.NORMAL;
			viewedText.setStyleRange(viewedStyle);
			viewedText = null;
		}
		
		for (int i = 0; i < styles.size(); i++) {
			StyledText catchBlock = (StyledText) styles.keySet().toArray()[i];
			catchBlock.setSelection(0, 0);
		}
		
		styles.clear();
	}


	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		// viewer.getControl().setFocus();
	}
	
	public void setup(List<CatchExample[]> examples) {		
		GridLayout gridLayoutRow = new GridLayout();
		gridLayoutRow.numColumns = 3;
		gridLayoutRow.makeColumnsEqualWidth = false;
		
		Composite rowComposite = new Composite(sc, SWT.NONE);
		rowComposite.setBackground(new Color(null, 255, 255, 255));
		rowComposite.setLayout(gridLayoutRow);
		
		//Call addRow for each catchExample, addRow adds a row consisting of the example (StlyedText) and class list (Tree)
		rows.clear();
		selectionIndexes.clear();
		this.examples = examples;
		for(CatchExample[] ce : examples) {
			addRow(rowComposite, ce);
		}

		Control oldContent = sc.getContent();
		if (oldContent != null) oldContent.dispose();
		
		sc.setContent(rowComposite);
		sc.setMinSize(rowComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public void focusLog() {
		for (int i = 0; i < examples.size(); i++) {
			if (examples.get(i)[0].isLog()) {
				focus(rows.get(i), selectionIndexes.get(i));
				break;
			}
		}
	}

	public void focusNonLog() {
		for (int i = 0; i < examples.size(); i++) {
			if (!examples.get(i)[0].isLog()) {
				focus(rows.get(i), selectionIndexes.get(i));
				break;
			}
		}
	}

	private void focus(StyledText st, int[] selectionIndexes) {
		int x = selectionIndexes[0], y = selectionIndexes[1];
		focus(st, x, y);
	}

	private void focus(StyledText st, int x, int y) {
		sc.showControl(st);
		st.forceFocus();
		st.setFocus();
		if (y > 0) st.setSelection(x, y);
		else st.selectAll();
	}
}
