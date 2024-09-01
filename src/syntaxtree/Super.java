package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * the expression 'super'
 */
public class Super extends Exp
{

    /**
     * constructor
     * @param pos file position
     */
    public Super(int pos)
    {
        super(pos);
    }

    public String name() {return "Super";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

}
