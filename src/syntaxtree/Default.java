package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * a default-label within a switch statement
 */
public class Default extends Label
{

    /**
     * constructor
     * @param pos file position
     */
    public Default(int pos)
    {
        super(pos);
    }

    public String name() {return "Default";}

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

}

