package syntaxtree;
import java.io.PrintStream;

import treedisplay.TreeDisplayable;
import treedisplay.TreeDrawException;
import visitor.Visitor;

/**
 * a method declaration (abstract)
 */
public abstract class MethodDecl extends Decl
{
    // instance variables filled in by constructor
    public VarDeclList formals; // the method's formal parameters
    public StatementList stmts; // the method's body

    // instance variables filled in during later phases
    public MethodDecl superMethod; // the method that this method is overriding, if any
    public int paramSize; // the total size of the parameters for the method.
    public int vtableOffset; // this method's position in the v-table
    public ClassDecl classDecl; // the class in which the method is declared

    /**
     * constructor
     * @param pos file position
     * @param aname the name being declared
     * @param aformals the list of formal parameter declarations
     * @param astmts the statements that are the method's body
     */
    public MethodDecl(int pos, String aname, VarDeclList aformals,
                      StatementList astmts)
    {
        super(pos,aname);
        formals=aformals;
        stmts=astmts;
        superMethod = null;
        vtableOffset = Integer.MIN_VALUE;
    }

    public String name() {return "MethodDecl";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

    public TreeDisplayable getDrawTreeSubobj(int n) throws TreeDrawException
    {
        switch (n)
        {
        case 0:
            return formals;
        case 1:
            return stmts;
        }
        throw new TreeDrawException();
    }

    public AstNode[] links()
    {
        return new AstNode[] {superMethod};
    }

    protected String[]stringsInDescr()
    {
        if (vtableOffset == Integer.MIN_VALUE)
        {
            return super.stringsInDescr();
        }
        else
        {
            return strArrayPlus1(super.stringsInDescr(),
                                 "vtableOffset="+vtableOffset);
        }
    }
}
