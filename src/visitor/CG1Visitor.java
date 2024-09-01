package visitor;

import syntaxtree.*;

import java.util.*;

import errorMsg.*;
import java.io.*;
import java.awt.Point;

/* David Leon */

//the purpose here is to annotate things with their offsets:
// - formal parameters, with respect to the (callee) frame
// - instance variables, with respect to their slot in the object
// - methods, with respect to their slot in the v-table
// - (Optionally) generate all v-tables.
public class CG1Visitor extends Visitor
{

    // IO stream to emit code.
    CodeStream code;


    //code.emit()
    //used to track the object class, since that's
    //the root of the inheritance tree.
    private ClassDecl object;

    //current class we're visiting.
    private ClassDecl currentClass;

    ////////////////////////////////////////////////////////////
    // This is used for doing your own VMT generation.
    // Otherwise you don't need it.
    ////////////////////////////////////////////////////////////
    // to collect the array types that are referenced in the code
    private HashSet<ArrayType> arrayTypes;

    public CG1Visitor(ErrorMsg e, PrintStream out, ClassDecl Object)
    {
        code = new CodeStream(out, e);
        object = Object;
        arrayTypes = new HashSet<ArrayType>();
    }

    public Object visit(Program p)
    {

        // comment the following line out if 
        // you are doing your own vtable generation:
        VtableGenerator.generate(p, code);
        p.classDecls.accept(this);
        p.dummyNodes.accept(this);
        return null;
    }


    /* calculate each offset for an object  
     * offset values are subject to change 
     * 
    */
    public Object visit(ClassDecl n)
    {
        n.decls.accept(this);

        int dataOffSet = -12;
        int objOffSet = 0;
        int numData = 0;
        int numObj = 0;


        ClassDecl parent = n.superLink;
        
        while (parent != null)
        {
            for (String key : parent.instVarTable.keySet())
            {
                if (parent.instVarTable.get(key).type.isBoolean() || parent.instVarTable.get(key).type.isInt()) //if we see an int or a bool
                { 
                    numData++;
                    dataOffSet += -4; 
                }
                else
                { 
                    numObj++;
                    objOffSet += 4; 
                }
            }
            
            parent = parent.superLink;
        }

      

        
        for (String key : n.instVarTable.keySet()) //iterate through all instance varaibles 
        {
          //  System.out.println("current key in class: " + n.instVarTable.get(key));
            if (n.instVarTable.get(key).type.isBoolean() || n.instVarTable.get(key).type.isInt()) //if we see an int or a bool
            {
                dataOffSet += -4;
                n.instVarTable.get(key).offset = dataOffSet; //add offset 
              //  System.out.println(n.instVarTable.get(key).name + " offSet: " + n.instVarTable.get(key).offset);
                numData++; //increment number of data seen
                
            }
            else
            {
                n.instVarTable.get(key).offset = objOffSet; //add offset 
               // System.out.println(n.instVarTable.get(key).name + " offSet: " + n.instVarTable.get(key).offset);

                objOffSet += 4;
                numObj++; //increment number of objects seen
            }
        }

        n.numObjInstVars = numObj; //class number of objects 
        n.numDataInstVars = numData; //class number of data fields 

       // System.out.println(n.name + " num obj: " + n.numObjInstVars + " num data: " + n.numDataInstVars);
        

        return null;
    }
  


    /* we must go through all the formal params and calculate offsets */ 
    public Object visit(MethodDeclVoid n)
    {
        n.formals.accept(this);
        n.stmts.accept(this);

       // System.out.println(n.name + " offset: " + n.vtableOffset);


       int parameterSize = 0;


        int paramOffset = 4;

     //   for (int i = n.formals.size() - 1; i >= 0; i--) //iterate through all the formal decls
        for (int i = 0; i < n.formals.size(); i++)
        {
           
            //System.out.println("param: " + n.formals.get(i).name);

            if (n.formals.get(i).type.isInt()) //if the var decl is an int 
            {
                n.formals.get(i).offset = paramOffset;
                paramOffset += 8; //we must account for garbage collector tag 
                parameterSize += 8;

              //  System.out.println(n.formals.get(i).name + " offset: " + n.formals.get(i).offset);
            }
            else //object or boolean 
            {
                n.formals.get(i).offset = paramOffset;
                paramOffset += 4;
                parameterSize += 4;

            //    System.out.println(n.formals.get(i).name + " offset: " + n.formals.get(i).offset);
            }


        }

        n.paramSize = parameterSize;

        return null;
    }




    /* we must go through all the formal params and calculate offsets */ 
    public Object visit(MethodDeclNonVoid n)
    {
        n.formals.accept(this);
        n.stmts.accept(this);
        n.rtnType.accept(this);
        n.rtnExp.accept(this);


       // code.emit("mipsssss coooooooodeeeeeeee");

      //  System.out.println(n.name + " offset: " + n.vtableOffset);

        int parameterSize = 0;
        int paramOffset = +4;

 
       // for (int i = n.formals.size() - 1; i >= 0; i--) //iterate through all the formal decls
        for (int i = 0; i < n.formals.size(); i++)
        {
        
    //        System.out.println("param: " + n.formals.get(i).name);

            if (n.formals.get(i).type.isInt()) //if the var decl is an int 
            {
                n.formals.get(i).offset = paramOffset;
                paramOffset += 8; //we must account for garbage collector tag 
                parameterSize += 8;

      //         System.out.println(n.formals.get(i).name + " offset: " + n.formals.get(i).offset);
            }
            else //object or boolean 
            {
                n.formals.get(i).offset = paramOffset;
                paramOffset += 4;
                parameterSize += 4;

      //          System.out.println(n.formals.get(i).name + " offset: " + n.formals.get(i).offset);
            }

        }

        n.paramSize = parameterSize;

        return null;
    }



    /////////////////////////////////////////////////////////////
    //
    // helper methods for generating VMTs
    //
    /////////////////////////////////////////////////////////////

    /**
     * emits the name of the class as a sequence of bytes.
     * This is used by the default implementation of toString(),
     * So, we need it as part of the VMT.
     */
    public void emitPrintName(AstNode n, String name)
    {
        // emit padding bytes for string
        for(int i = name.length()%4; 0 < i && i < 4; i++)
        {
            code.emit(n, "  .byte 0");
        }

        //print out the first character with the first bit set to 1
        //This allows the toString method to know that
        //we've reached the first character of the string.
        code.emit(n, "  .byte "+ ((int)name.charAt(0) | 0x80) +
                     " # '"+name.charAt(0)+"' with high bit set");
        for(char c : name.substring(1).toCharArray())
        {
            code.emit(n, "  .byte "+(int)c+ " # '"+c+"'");
        }
    }

    /**
     * Emit VMT for arrays.
     * Since arrays can't override methods, 
     * they have the same VMT as Object.
     */
    public void emitArrayTypeVtables()
    {
        // emit object arrays before int and bool arrays (if they exists)
        // because the garbage collector
        // needs to know if it's a data array.
        ArrayType iarr = null;
        ArrayType barr = null;
        for(ArrayType at : arrayTypes)
        {
            if(at.baseType.isInt())
            {
                iarr = at;
            }
            else if(at.baseType.isBoolean())
            {
                barr = at;
            }
            else
            {
                emitArray(at);
            }
        }
        code.emit(new IntegerType(-1), "dataArrayVTableStart:");
        if(iarr != null)
        {
            emitArray(iarr);
        }
        if(barr != null)
        {
            emitArray(barr);
        }
    }

    public void emitArray(ArrayType at)
    {
        emitPrintName(at, at.typeName());
        code.emit(at, "CLASS_"+at.vtableName()+":");
        code.emit(at, "  .word fcn_hashCode_Object");
        code.emit(at, "  .word fcn_toString_Object");
        code.emit(at, "  .word fcn_equals_Object");
        code.emit(at, "END_CLASS_"+at.vtableName()+":");
    }

}

