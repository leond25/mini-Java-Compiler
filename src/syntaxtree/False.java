package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * a expression 'false'
 */
public class False extends Exp
{

    /**
     * constructor
     * @param pos file position
     */
    public False(int pos)
    {
        super(pos);
    }

    public String name() {return "False";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

}
