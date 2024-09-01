package syntaxtree;
import treedisplay.TreeDisplayable;
import treedisplay.TreeDrawException;
import visitor.Visitor;

import java.io.PrintStream;
import java.util.*;

/**
 * a class declaration
 */
public class ClassDecl extends Decl
{
    // instance variables filled in by constructor
    public String superName; // superclass name
    public DeclList decls; // list of declarations

    // instance variables filled in during later phases
    public ClassDecl superLink; // link to superclass declaration
    public ClassDeclList subclasses; // list of subclasses
    // symbol table for instance variables
    public Hashtable<String,InstVarDecl> instVarTable;
    // symbol table for methods
    public Hashtable<String,MethodDecl> methodTable;
    public int numDataInstVars; // number of non-object instance variables
    public int numObjInstVars; // number of object instance variables

    /**
     * constructor
     * @param pos file position
     * @param aname the name being declared
     * @param asuperName the name of the superclass
     * @param aDeclList the list of declarations inside the class
     */
    public ClassDecl(int pos, String aname, String asuperName,
                     DeclList aDeclList)
    {
        super(pos, aname);
        superName=asuperName;
        decls = aDeclList;
        superLink = null;
        subclasses = new ClassDeclList();
        instVarTable = new Hashtable<String,InstVarDecl>();
        methodTable = new Hashtable<String,MethodDecl>();
    }

    public String name() {return "ClassDecl";}

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
            return decls;
        }
        throw new TreeDrawException();
    }

    protected String[]stringsInDescr()
    {
        return strArrayPlus1(super.stringsInDescr(), superName);
    }

    public AstNode[] links()
    {
        return new AstNode[] {superLink};
    }
}
