package visitor;

import syntaxtree.*;

import java.util.*;

import errorMsg.*;
import java.io.*;
import java.awt.Point;

//the purpose here is to emit mips code to represent string literals.
public class CG2Visitor extends Visitor
{
    // IO stream to which we will emit code
    CodeStream code;

    // Environment for string to the first node that we found with that string.
    HashMap<String,StringLiteral> stringEnv;

    public CG2Visitor(ErrorMsg e, PrintStream out)
    {
        stringEnv = new HashMap<String,StringLiteral>();
        code = new CodeStream(out, e);
    }

    public Object visit(Program p)
    {
        // CS 358 STUDENTS:
        // This generates MIPS for string literals,
        // however it does not account for duplicate strings.
        // In order to get == to work correctly,
        // you must create your own version which does account for duplicate
        // strings.
        StrLitSimpleGenerator.generate(p,  code);

        return null;
    }

}
