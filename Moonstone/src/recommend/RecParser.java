package recommend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.ui.SharedASTProvider;

public class RecParser 
{
	public static final boolean debug = true;
	
	
	public static void extractEH(RecommendationRepository recommendationRepository, String fileName, ICompilationUnit unit)
	{
		try
		{
			if(debug) System.out.printf("[RecASTWalker] Beginning eh crawl on %s%n", fileName);
			CompilationUnit cu = SharedASTProvider.getAST(unit, SharedASTProvider.WAIT_NO, null);
			if (cu == null) 
				cu = parse(unit);
			crawlEH(recommendationRepository, cu, fileName);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static CompilationUnit parse(ICompilationUnit unit) throws IOException {
		if (!unit.exists()) return null;

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);

		return (CompilationUnit) parser.createAST(null);
	}
	
	private static void crawlEH(RecommendationRepository recommendationRepository, CompilationUnit cu, String fileName)
	{
		cu.accept(new ASTVisitor(){
			/*
			 * Okay, we reach a try block
			 */
			public boolean visit(TryStatement tryStatement) {
				//if(debug) System.out.println("[recommend] Found a try block! Beginning recording it.");

				List<Feature> tryFeatures = new ArrayList<>();
				addTryResourceTypes(tryStatement, tryFeatures);
				
				/*
				 * For each catch clause
				 */
				List<CatchClause> catchClauses = tryStatement.catchClauses();
				for (CatchClause c : catchClauses)
				{
					CatchHandler catchHandler = new CatchHandler(c);
					if (!c.getBody().statements().isEmpty())
						RecDatabase.addHandlerToDB(recommendationRepository, fileName, catchHandler);
				}
				
				/*
				 * For a finally block, if present
				 */
				Block finallyBlock = tryStatement.getFinally();
				if (finallyBlock != null)
				{
					FinallyHandler finallyHandler = new FinallyHandler(tryStatement);
					if (!finallyBlock.statements().isEmpty())
						RecDatabase.addHandlerToDB(recommendationRepository, fileName, finallyHandler);
				}
				
				return false;
			}
			

		});
	}
	
	private static void addTryResourceTypes(TryStatement ts, List<Feature> tryFeatures)
	{
		ts.accept(new ASTVisitor(){
			public boolean visit(SimpleName node) {
				ITypeBinding tb = node.resolveTypeBinding();
				if(tb != null)//unfortunately, cannot resolve all types statically?
				{
					ITypeBinding[] interfaces = tb.getInterfaces();
					for(ITypeBinding inter: interfaces)
					{
						if(inter.getName().equals("Closeable"))
						{
							//System.out.println("&&&&&&&&&found a resource! "+tb.getQualifiedName());
							tryFeatures.add(new Feature(tb.getQualifiedName(), Feature.RESOURCE));
						}
					}
				}
				return false;
			}
			
			public boolean visit(CatchClause c)
			{
				return false;
			}
			
		});
	}
}
