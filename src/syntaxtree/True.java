package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * the expression, 'true'
 */
public class True extends Exp
{

    /**
     * constructor
     * @param pos file position
     */
    public True(int pos)
    {
        super(pos);
    }

    public String name() {return "True";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }
}
