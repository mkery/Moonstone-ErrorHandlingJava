package recommend.experimentOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.*;

public class StatementWalker 
{
	private static final boolean debug = false;
	
	public static List<String> extractStatements(Block body)
	{
		List<String> statementList = new ArrayList<>();

		body.accept(new ASTVisitor() {
			boolean counted = false; //<- debug, just to warn us what we're missing
			Stack<String> workingName = new Stack<>();
			
			@Override
			public void preVisit(ASTNode node) {
				counted = false;
			}

			@Override
			public void postVisit(ASTNode node) {

				if( !(node instanceof Block))//!counted &&
				{
					if(debug) System.out.println("[recWalker] UNKNOWN of '" + node.toString()+" "+ASTNode.nodeClassForType(node.getNodeType()));
				}
			}
			
			//--------------------
			/*can stand alone as a statement
			 * but can be in something like a while statement
			 */
			public boolean visit(Assignment node)
			{
				workingName.push("#a");
				return true;
			}
			
			/*
			 * Expect 2 things on the stack: right hand side of '=' and left hand side of '='
			 */
			public void endVisit(Assignment node)
			{
				counted = false;
				
				String rightHand = workingName.pop(); //remove right side from stack
				
				String leftHand = workingName.pop(); //remove left side
				if(node.getLeftHandSide() instanceof Name)
					leftHand = node.getLeftHandSide().toString();
				
				
				workingName.pop(); //get rid of #a placeholder
				String statement = leftHand + " = " + rightHand;
				statementList.add(statement.toString());
				if(debug) System.out.println("[recWalker] NEW statement complete: "+statement);

				/*
				 * If the stack is not empty, ie, this assign is within something, add
				 * the completed version back to the stack.
				 */
				if(!workingName.isEmpty())
				{
					workingName.push(statement);
				}
			}
			
			//--------------------
		
			public boolean visit(NullLiteral node)
			{
				counted = true;
				workingName.push("null");
				return false;//do not continue
			}
			
			//--------------------
			//can stand alone as a statement
			public boolean visit(IfStatement node)
			{
				//workingName.push(new StringBuilder());
				//workingName.peek().append("if ( %s )");
				//TODO
				return false;
			}
			
			//--------------------
			//can ONLY stand alone as a statement
			public boolean visit(ReturnStatement node) {
				counted = true;
				workingName.push("#ret");
				return true;
			}
			
			public void endVisit(ReturnStatement node){
				String statement;
				
				// if just 'return;'
				if(node.getExpression() == null)
				{
					statement = "return ";
				}
				else //otherwise, we expect a return value on the stack
					statement = "return " + workingName.pop();
				
				workingName.pop(); //get rid of #ret placeholder
				statementList.add(statement);
				if(debug) System.out.println("[recWalker] NEW statement complete: "+statement);
			}

			//--------------------
			//can ONLY stand alone as a statement?
			public boolean visit(ThrowStatement node) {
				counted = true;
				workingName.push("#throw");
				return true; //will go further down the tree, filling out the remainder of the throw statement
			}
			
			/*
			 * Expecting the expression thrown to be on the stack
			 */
			public void endVisit(ThrowStatement node){
				String statement = "throw " + workingName.pop();
				workingName.pop(); //get rid of #throw
				statementList.add(statement);
				if(debug) System.out.println("[recWalker] NEW statement complete: "+statement);
			}
			//--------------------
			//can ONLY stand alone as a statement
			public boolean visit(TryStatement node)
			{
				counted = true;	
				workingName.push("#try");
				return true;
			}
			
			/*
			 * Expecting 0-N things on the stack: a finally block, catch blocks, and lastly a try body
			 */
			public void endVisit(TryStatement node)
			{
				counted = true;
				
				String fina = "";
				if(node.getFinally() != null)
					fina = String.format("finally\n %s \n #endfinally", workingName.pop());
				
				
				StringBuilder cat = new StringBuilder();
				for(int i=0; i<node.catchClauses().size(); i++)
					cat.append(workingName.pop()+"\n");
				
				String tr = String.format("try\n %s \n #endtry\n", workingName.pop());	
			
				String tcf = tr+cat+fina;
				statementList.add(tcf);
				if(debug) System.out.println("[recWalker] NEW statement complete: "+tcf);
				workingName.pop(); //get rid of #try placeholder
			}
			
			//--------------------
			
			public boolean visit(CatchClause node)
			{
				workingName.push("#catch");
				return true;
			}
			
			/*
			 * Expecting 2 things on the stack: the exception type being caught, the catch body
			 */
			public void endVisit(CatchClause node)
			{
				counted = true;
				
				String catchBody = "";
				if(!node.getBody().statements().isEmpty())
					catchBody = "{\n"+workingName.pop()+"\n}";
				
				String catchType = workingName.pop();
				String statement = "catch( "+catchType+" )\n"+catchBody;
				workingName.pop(); //get rid of #catch placeholder
				/*
				 * Now, since catch is absolutely a dependent thing, add the completed catch back 
				 * to the stack
				 */
				workingName.push(statement);
			}
			
			//--------------------
			
			public boolean visit(ClassInstanceCreation node)
			{
				counted = true;
				workingName.push("#class");
				return true;
			}
			
			/*
			 * Expecting stack with class name, arguments
			 */
			public void endVisit(ClassInstanceCreation node){
				
				String arguments = "";
				List<Expression> args = node.arguments();
				for(int i=0; i< args.size(); i++)
				{
					if(i+1 < args.size())
						arguments += workingName.pop() + ", ";
					else
						arguments += workingName.pop();
				}
				
				String newClass = "new " + workingName.pop() + "("+arguments+")";
				workingName.pop(); //get rid of #class placeholder
				
				//only add if its on it's own, because that seems like an unusual case
				if(workingName.isEmpty())
				{
					statementList.add(newClass);
					if(debug) System.out.println("[recWalker] NEW statement complete: "+newClass);
				}
				else
					workingName.push(newClass);
			}
			
			//--------------------
			
			//eg. e.printStackTrace()
			public boolean visit(MethodInvocation node) {
				counted = true;
				workingName.push("#meth");
				return true;
			}
			
			public void endVisit(MethodInvocation node){
				/*System.out.println("ending on "+node);
				System.out.println("the stack looks like "+workingName.toString());
				System.out.println("the first stack element is "+workingName.peek());
				System.exit(0);*/
				
				//first, args will be on the stack
				String arguments = "";
				List<Expression> args = node.arguments();
				for(int i=0; i< args.size(); i++)
				{
					if(i+1 < args.size())
						arguments += workingName.pop() + ", ";
					else
						arguments += workingName.pop();
				}
				
				//next, the method name will be on the stack
				workingName.pop();//not interested in return type of method, which will be recorded here
				String method = node.getName().getFullyQualifiedName(); //you don't want the TYPE (which is what SimpleNAme will return)
				
				//finally, an object for this method may be on the stack
				String caller = "";
				if(node.getExpression() != null)
					caller = workingName.pop()+".";
				
				String statement = caller + method +"(" + arguments + ")";
				statementList.add(statement);
				if(debug) System.out.println("[recWalker] NEW statement complete: "+statement);
				
				workingName.pop(); //get rid of #meth placeholder
				//if we are nested in something else, add completed back to the stack
				if(!workingName.isEmpty())
					workingName.push(statement);
			}
			//--------------------
			
			public boolean visit(InfixExpression node) 
			{
				counted = true;
				workingName.push("#infix");
				return true;
			}
			
			/*
			 * Expect 2 things on the stack, a right side expression, a left side expression
			 */
			public void endVisit(InfixExpression node)
			{
				String right = workingName.pop();
				String left = workingName.pop();
				String statement = left + " "+node.getOperator().toString()+" "+right;
				
				List<Expression> others = node.extendedOperands();
				for(Expression ex : others)
				{
					statement += " "+node.getOperator()+" "+workingName.pop();
				}
				workingName.pop();//get rid of #infix place holder
				
				if(!workingName.isEmpty())
					workingName.push(statement);
				else
				{
					statementList.add(statement);
					if(debug) System.out.println("[recWalker] NEW statement complete: "+statement);
				}
			}
			
			//--------------------
			
			public boolean visit(SimpleName node) {
				counted = true;

				if(node.resolveTypeBinding() != null)
					workingName.push(node.resolveTypeBinding().getQualifiedName()+" ");
				else
					workingName.push(node.toString()+" ");
				return false;
			}
			
			//--------------------
			
			public boolean visit(NumberLiteral node){
				counted = true;
				workingName.push("NumberLiteral");
				return false;
			}
			
			//--------------------
			
			public boolean visit(BooleanLiteral node){
				counted = true;
				workingName.push("bool");
				return false;
			}
			
			//--------------------
			
			public boolean visit(StringLiteral node){
				counted = true;
				workingName.push("String");
				return false;
			}
			
			//--------------------
			
			//can stand alone
			public boolean visit(VariableDeclarationStatement node) {
				counted = true;
				
				String statement = node.getType().toString();
				ITypeBinding typeBinding = node.getType().resolveBinding();
				if(typeBinding != null)
					statement = typeBinding.getQualifiedName();
				
				List<VariableDeclarationFragment> frags = node.fragments();
				for(VariableDeclarationFragment f : frags)
				{
					statement += " "+f.getName();
				}
				
				//since this could be independent, just add it to the statement list
				statementList.add(statement);
				
				if(!workingName.isEmpty())
					workingName.push(statement);
				
				return false; // do not continue 
			}
			
			//--------------------

			public boolean visit(FieldDeclaration node) {						
				counted = true;
				
				String statement = node.getType().toString();
				ITypeBinding typeBinding = node.getType().resolveBinding();
				if(typeBinding != null)
					statement = typeBinding.getQualifiedName();
				
				workingName.push(statement);
				return false; // do not continue 
			}
			
			//--------------------
			
			public boolean visit(SingleVariableDeclaration node){
				counted = true;
				
				String statement = node.getType().toString();
				ITypeBinding typeBinding = node.getType().resolveBinding();
				if(typeBinding != null)
					statement = typeBinding.getQualifiedName();

				statement += " "+node.getName();
				//since this could be independent, just add it to the statement list
				statementList.add(statement);
				
				if(!workingName.isEmpty())
					workingName.push(statement);
				
				return false; // do not continue 
			}
			
			//--------------------
			
			public boolean visit(ArrayAccess node){
				workingName.push("#array");
				return true;
			}
			
			@Override
			public void endVisit(ArrayAccess node){
				String access = workingName.pop();
				String arr = node.getArray().toString();
				workingName.pop(); //get rid off array type
				workingName.pop(); //get rid of #array placeholder
				String statement = arr+"["+access+"]";
				
				if(!workingName.isEmpty())
					workingName.push(statement);
			}

		});

		return statementList;
	}

}
