package syntaxtree;
import java.io.PrintStream;

import treedisplay.TreeDisplayable;
import treedisplay.TreeDrawException;
import visitor.Visitor;

/**
 * a method-call expression
 */
public class Call extends Exp
{

    // instance variables filled in by constructor
    public Exp obj; // the object on which the method is being called
    public String methName; // the name of the method being called
    public ExpList parms; // the list of actual parameters in the call

    // instance variables filled in during later phases
    public MethodDecl methodLink; // declaration of the method being called

    /**
     * constructor
     * @param pos file position
     * @param aobj the object on which the method is being called
     * @param amethName the name of the method
     * @param aparms the actual parameter list
     */
    public Call(int pos, Exp aobj, String amethName, ExpList aparms)
    {
        super(pos);
        obj=aobj;
        methName=amethName;
        parms=aparms;
        methodLink = null;
    }

    public String name() {return "Call";}

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
            return obj;
        case 1:
            return parms;
        }
        throw new TreeDrawException();
    }
    protected String[]stringsInDescr()
    {
        return strArrayPlus1(methName,super.stringsInDescr());
    }

    public AstNode[] links()
    {
        return new AstNode[] {methodLink};
    }
}
