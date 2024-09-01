package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * the expression, 'this'
 */
public class This extends Exp
{

    /**
     * constructor
     * @param pos file position
     */
    public This(int pos)
    {
        super(pos);
    }

    public String name() {return "This";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

}
