package syntaxtree;
import java.io.PrintStream;

import visitor.Visitor;

/**
 * the type of an error (like looking up a method without it existing.
 */
public class ErrorType extends Type
{

    /**
     * constructor
     * @param pos file position
     */
    public ErrorType(int pos)
    {
        super(pos);
    }

    public String name() {return "ErrorType";}

    public String vtableName() { return "ERROR"; }
    public String typeName()   { return "X"; }

    /**
     * type equality
     * @param the object tested for being equal to me
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof NullType;
    }

    public boolean isError() {return true;}
    /**
     * hash code
     * @return the object's hash code
     */
    @Override
    public int hashCode()
    {
        return 7326835;
    }

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

    public String toString2()
    {
        return "(ERROR type)";
    }

}
