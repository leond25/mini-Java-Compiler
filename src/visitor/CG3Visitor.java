package visitor;

import syntaxtree.*;
import errorMsg.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.List;

/* David Leon */

public class CG3Visitor extends Visitor
{
    // The purpose here is to generate assembly for each Node
    // in the AST.

    // IO stream to which we will emit code
    CodeStream code;

    // current stack height
    int stack;

    public CG3Visitor(ErrorMsg e, PrintStream out)
    {
        code = new CodeStream(out, e);
        code.setVisitor3(this);
        stack = 0;
    }

    @Override
    public Object visit(Program n)
    {
        code.emit(".text");
        code.emit(".globl main");
        code.emit("main:");
        code.emit("  jal vm_init");

        code.emit("li $s6, 1          # only data field is the VMT");
        code.emit("li $s7, 0          # no object fields");
        code.emit("jal newObject      # make new Main()");
        code.emit("la $t0, CLASS_Main # set the VMT");
        code.emit("sw $t0, -12($s7)");
        code.emit("lw $s2, ($sp)      # pop Main into s2 ");
        code.emit("addu $sp, $sp, 4     # for this pointer"); 

        code.emit("  jal fcn_main_Main");

        //exit the program
        code.emit("  li $v0, 10");
        code.emit("  syscall");


        // emit code for all the methods in all the class declarations
        n.classDecls.accept(this);

        // flush the output and return
        code.flush();
        return null;
    }



    @Override
    public Object visit(MethodDeclVoid n)
    {

        stack = 0;

        code.comment(n, "begin");
        
        methodHeader(n.name); //make method space more readable 

        code.emit(".globl fcn_" + n.name + "_" + n.classDecl.name);
        code.emit("fcn_" + n.name + "_" + n.classDecl.name + ":"); //method label

        code.emit("subu $sp, $sp, 4");
        code.emit("sw $ra, ($sp)"); //store return address 


        n.formals.accept(this); //accept formal parameters
        n.stmts.accept(this); //accepts statements in method 
 
        code.emit("addu $sp, $sp, " + stack); //pop off all local variables 
        code.emit("lw $ra, ($sp)");
        code.emit("addu $sp, $sp, 4");
        //maybe adjust here
    

        code.emit("jr $ra");

        
        code.comment(n, "end");
        
    

        return null;
    }



    @Override
    public Object visit(MethodDeclNonVoid n)
    {
        stack = 0;

        
        code.comment(n, "begin");

        methodHeader(n.name); //make method space more readable 

        code.emit(".globl fcn_" + n.name + "_" + n.classDecl.name);
        code.emit("fcn_" + n.name + "_" + n.classDecl.name + ":"); //method label

        code.emit("subu $sp, $sp, 4");
        code.emit("sw $ra, ($sp)");


        n.formals.accept(this); //accept formal parameters 
        n.stmts.accept(this); //accept statements 
        n.rtnType.accept(this);
        n.rtnExp.accept(this);

        pop(n.rtnExp.type, "$t0");

        code.emit("addu $sp, $sp, " + stack); //pop off all local variables 

        code.emit("lw $ra, ($sp)");
        code.emit("addu $sp, $sp, 4");


        code.emit("jr $ra");
       
        code.comment(n, "end");
        
        return null;
    }



    public Object visit(Call n)
    {
        n.obj.accept(this);
        n.parms.accept(this);

        code.emit("");
        code.comment(n, "begin");



        if (n.obj instanceof Super)
        {
            swap("$s2", n.methodLink.paramSize);
            code.emit("jal fcn_" + n.methodLink.name + "_" + n.methodLink.classDecl.name);
            code.emit("addu $sp, $sp, " + n.methodLink.paramSize);
            stack -= n.methodLink.paramSize;
            pop(n.obj.type, "$s2");
            push(n.type, "$t0");
        }
        else
        {

            swap("$s2", n.methodLink.paramSize);
            code.emit("beq $s2, $zero, nullPtrException");
            code.emit("lw $t0, -12($s2)");

            int methodOffset = n.methodLink.vtableOffset * 4;

            code.emit("lw $t0, " + methodOffset + "($t0)");
            code.emit("jalr $t0");

            code.emit("addu $sp, $sp, " + n.methodLink.paramSize);
            stack -= n.methodLink.paramSize;
            pop(n.obj.type, "$s2");
            push(n.type, "$t0");
        }

        code.comment(n, "end");
        code.emit("");

        return null;
    }


    public void swap(String reg, int paramSize)
    {
        code.emit("lw $t0, " + paramSize + "($sp)");
        code.emit("sw " + reg + ", " + paramSize + "($sp)");
        code.emit("move " + reg + ", $t0");
    }
    


    @Override
    public Object visit(NewObject n)
    {

        code.emit("");
        code.comment(n, "begin");


        n.objType.accept(this);

        int data = n.objType.link.numDataInstVars + 1;

        code.emit("li $s6, " + data); //get the number of data values + 1 in class
        code.emit("li $s7, " + n.objType.link.numObjInstVars); //get the number of objects in class
        code.emit("jal newObject");
        code.emit("la $t0, CLASS_" + n.objType.link.name); //get the class label
        code.emit("sw $t0, -12($s7)"); //put class label address in memory 


     
        code.comment(n, "end");
        code.emit("");

        stack += 4; //since newObject gets pushed on the stack 

        return null;
    }

    public Object visit(CallStatement n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.callExp.accept(this);

        pop(n.callExp.type, "$t0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(If n)
    {

        code.emit("");
        code.comment(n, "begin");

        n.exp.accept(this);
        pop(n.exp.type, "$t0");
        code.emit("beq $t0, $0, if_else_" + n.uniqueId);
        n.trueStmt.accept(this);
        code.emit("j if_done_" + n.uniqueId);
        code.emit("if_else_" + n.uniqueId + ":");
        n.falseStmt.accept(this);
        code.emit("if_done_" + n.uniqueId +":");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(While n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.stackHeight = stack;

        code.emit("while_cond_" + n.uniqueId + ":");
        n.exp.accept(this);
        pop(n.exp.type, "$t0");
        code.emit("beq $t0, $0 break_target_" + n.uniqueId);
        n.body.accept(this);
        code.emit("j while_cond_" + n.uniqueId);
        code.emit("break_target_" + n.uniqueId + ":");

        code.comment(n, "end");
        code.emit("");


        return null;
    }

    
    public Object visit(Break n)
    {

        code.emit("");
        code.comment(n, "begin");

        int amountToPop = stack - n.breakLink.stackHeight;
        code.emit("addu $sp, $sp, " + amountToPop);
        code.emit("j break_target_" + n.breakLink.uniqueId);
        

        code.comment(n, "end");
        code.emit("");

        return null;
    }


    public Object visit(Switch n)
    {

        code.emit("");
        code.comment(n, "begin");

        n.exp.accept(this);
        pop(n.exp.type, "$t0");

        for (int i = 0; i < n.stmts.size(); i++)
        {
            if (n.stmts.get(i) instanceof Case)
            {
                Case someCase = (Case) n.stmts.get(i);
        
                IntegerLiteral intLit = (IntegerLiteral) someCase.exp;

                code.emit("li $t1, " + intLit.val);
                code.emit("beq $t0, $t1, case_label_" + someCase.uniqueId);

            }
            else if (n.stmts.get(i) instanceof Default)
            {
                Default someDefault = (Default) n.stmts.get(i);

                code.emit("j case_label_" + someDefault.uniqueId);

            }
        }

        n.stmts.accept(this);

        code.emit("break_target_" + n.uniqueId + ":");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(Case n)
    {
       // n.exp.accept(this);

       n.enclosingSwitch.stackHeight = stack;

       code.emit("");
       code.comment(n, "begin");

       code.emit("case_label_" + n.uniqueId + ":");

       code.comment(n, "end");
       code.emit("");

        return null;
    }

    public Object visit(Default n)
    {

        n.enclosingSwitch.stackHeight = stack;

        code.emit("");
        code.comment(n, "begin");

        code.emit("case_label_" + n.uniqueId + ":");

        code.comment(n, "end");
        code.emit("");

        return null;
    }
 
     
    public Object visit(Block n)
    {
        code.emit("");
        code.comment(n, "begin");
        int amountToPop = 0;

        n.stmts.accept(this);

        for (int i = 0; i < n.stmts.size(); i++)
        {
            if (n.stmts.get(i) instanceof LocalDeclStatement)
            {
                LocalDeclStatement var = (LocalDeclStatement) n.stmts.get(i);
              //  System.out.println(var.localVarDecl.initExp.type);

                if (var.localVarDecl.initExp.type.isInt())
                {
                    amountToPop += 8;
                }
                else
                {
                    amountToPop += 4;
                }
            }
        }

    
        code.emit("addu $sp, $sp, " + amountToPop);
        stack -= amountToPop;

        code.comment(n, "end");
        code.emit("");


        return null;
    }
     
   
    public Object visit(IntegerLiteral n)
    {
        code.emit("");
        code.comment(n, "begin");
     

        code.emit("li $t0, " + n.val);
        push(n.type, "$t0");

     
        code.comment(n, "end");
        code.emit("");
        

        return null;
    }

    public Object visit(True n)
    {
        code.emit("");
        code.comment(n, "begin");

        code.emit("li $t0, 1");
        push(n.type, "$t0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(False n)
    {
        code.emit("");
        code.comment(n, "begin");

        push(n.type, "$0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }
    
    
    public Object visit(StringLiteral n)
    {
        
        code.emit("");
        code.comment(n, "begin");
        

        code.emit("la $t0, strLit_" + n.uniqueCgRep.uniqueId + " # get the string (its address)"); //get the label 
        push(n.type, "$t0"); //push the label's address onto the stack 

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(LocalVarDecl n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.initExp.accept(this);
        code.emit("lw $0, ($sp) #**\"" + n.name + "\""); //give local varDecl a name
        n.offset = stack;

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(Null n)
    {
        code.emit("");
        code.comment(n, "begin");

        push(n.type, "$0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(This n)
    {
        code.emit("");
        code.comment(n, "begin");

        push(n.type, "$s2");

        code.comment(n, "end");
        code.emit("");


        return null;
    }

    public Object visit(Super n)
    {
        code.emit("");
        code.comment(n, "begin");

        push(n.type, "$s2");

        code.comment(n, "end");
        code.emit("");


        return null;
    }

    public Object visit(InstVarAccess n)
    {
        code.emit("");
        code.comment(n, "begin");

       // System.out.println("in instance var access");

        n.exp.accept(this);
        
        if(n.exp instanceof IdentifierExp)
        {
           // System.out.println(n.type);
            pop(n.exp.type, "$t0");
            npe("$t0");
            code.emit("lw $t0, " + n.varDec.offset + "($t0)");
         //   push(n.exp.type, "$t0");
            push(n.type, "$t0");
            

        }
        else
        {
            code.emit("lw $t0, " + n.varDec.offset + "($s2)");
            push(n.type, "$t0");
        }

        code.comment(n, "end");
        code.emit("");



        return null;
    }


    public Object visit(Cast n)
    {

        code.emit("");
        code.comment(n, "begin");

      //  n.castType.accept(this);
        n.exp.accept(this);

        code.emit("la $t0, CLASS_" + n.castType);
        code.emit("la $t1, END_CLASS_" + n.castType);
        code.emit("jal checkCast");


        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(InstanceOf n)
    {
        code.emit("");
        code.comment(n, "begin");

        // n.checkType.accept(this);
        n.exp.accept(this);

        code.emit("la $t0, CLASS_" + n.checkType);
        code.emit("la $t1, END_CLASS_" + n.checkType);
        code.emit("jal instanceOf");


        code.comment(n, "end");
        code.emit("");
        return null;
    }

    public Object visit(Assign n)
    {
      //  n.lhs.accept(this);
      //  n.rhs.accept(this);

      
        code.emit("");
        code.comment(n, "begin");

        
        
        if (n.lhs instanceof InstVarAccess)
        {
            InstVarAccess inst = (InstVarAccess) n.lhs;

            if (inst.exp instanceof IdentifierExp)
            {
                inst.exp.accept(this);
             //   System.out.println(inst.exp);
                n.rhs.accept(this);
             //   System.out.println(n.rhs);
                pop(n.rhs.type, "$t0"); //just in case this was pop(n.lhs.type, "$t0")
                pop(inst.exp.type, "$t1"); //just in case this was pop(n.lhs.type, "$t1");
              //  pop(n.lhs.type, "$t0");
               // pop(n.rhs.type, "$t1");
                code.emit("beq $t1, $zero, nullPtrException");
                code.emit("sw $t0, " + inst.varDec.offset + "($t1)");
            }
            else if (inst.exp instanceof This)
            {
                n.rhs.accept(this);
                pop(n.rhs.type, "$t0");
                code.emit("sw $t0, " + inst.varDec.offset + "($s2)");
            }
        }
        else if (n.lhs instanceof IdentifierExp)
        {

            IdentifierExp exp = (IdentifierExp) n.lhs;
            
            if (exp.link instanceof InstVarDecl) //this is the case where this is not explictly seen in the code 
            {
                n.rhs.accept(this);
                pop(n.rhs.type, "$t0");
                code.emit("sw $t0, " + exp.link.offset + "($s2)");
            }
            else
            {
                //check here 
                n.rhs.accept(this);
                pop(n.rhs.type, "$t0");

                int localOffset = stack - exp.link.offset;

                code.emit("sw $t0, " + localOffset + "($sp)");
            }

        }
        else if (n.lhs instanceof ArrayLookup)
        {
            ArrayLookup arr = (ArrayLookup) n.lhs;

            arr.arrExp.accept(this);
            arr.idxExp.accept(this);
            n.rhs.accept(this);

            pop(n.rhs.type, "$t0");
            pop(arr.idxExp.type, "$t1");
            pop(arr.arrExp.type, "$t2");
            oob("$t2", "$t1");
            arrLoad("$t1", "$t2", "$t1");
            code.emit("sw $t0, ($t1)");

        }


        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public void arrLoad(String r0, String r1, String r2)
    {
        code.emit("sll " + r0 + ", " + r2 + ", 2");
        code.emit("addu " + r0 + ", " + r0 + ", " + r1);

    }

    public void oob(String r1, String r2)
    {
        npe(r1);
        code.emit("lw $t3, -4(" + r1 + ")");
        code.emit("bgeu " + r2 +", $t3, arrayIndexOutOfBounds");

    }

    public void npe(String r1)
    {
        code.emit("beq " + r1 + ", $0, nullPtrException");
    }

    public Object visit(NewArray n)
    {
       // n.objType.accept(this);

        code.emit("");
        code.comment(n, "begin");

        n.sizeExp.accept(this);

        code.emit("li $s6, 1");
        pop(n.sizeExp.type, "$s7");
        code.emit("jal newObject");

        if (n.objType.isInt())
        {
            code.emit("la $t0, CLASS_ARRAY_INT"); 
        }
        else if(n.objType.isBoolean())
        {
            code.emit("la $t0, CLASS_ARRAY_BOOLEAN"); 
        }
        else
        {
            code.emit("la $t0, CLASS_ARRAY_" + n.objType);
        }

        code.emit("sw $t0, -12($s7)");

        code.comment(n, "end");
        code.emit("");

        stack += 4; //since newObject gets pushed on the stack


        return null;
    }

    public Object visit(ArrayLookup n)
    {
     
        code.emit("");
        code.comment(n, "begin");

        n.idxExp.accept(this);
        n.arrExp.accept(this);
        pop(n.arrExp.type, "$t0");
        pop(n.idxExp.type, "$t1");
        oob("$t0", "$t1");
        arrLoad("$t1","$t0", "$t1");
        code.emit("lw $t0, ($t1)");
      //  push(n.arrExp.type, "$t0");
        push(n.type, "$t0");

        code.comment(n, "end");
        code.emit("");




        return null;
    }



    public Object visit(Not n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.exp.accept(this);

        code.emit("lw $t0, ($sp)");
        code.emit("xor $t0, $t0, 1");
        code.emit("sw $t0, ($sp)");

        code.comment(n, "end");
        code.emit("");
        
        return null;
    }

    public Object visit(ArrayLength n)
    {

        code.emit("");
        code.comment(n, "begin");

        n.exp.accept(this);

        pop(n.exp.type, "$t0");
        npe("$t0");
        code.emit("lw $t0, -4($t0)");
      //  push(n.exp.type, "$t0");
        push(n.type, "$t0");
        
        code.comment(n, "end");
        code.emit("");
        
        return null;
    }

   



    public Object visit(IdentifierExp n)
    {

        code.emit("");
        code.comment(n, "begin");

        if (n.link instanceof LocalVarDecl)
        {
          
            int offset = stack - n.link.offset;
            code.emit("lw $t0, " + offset + "($sp)");
            push(n.type, "$t0");
        }
        else if (n.link instanceof InstVarDecl)
        {
            code.emit("lw $t0, " + n.link.offset + "($s2)"); //changed to s2 it was sp
            push(n.type, "$t0");
        }
        else if (n.link instanceof FormalDecl)
        {

            int offset = stack + n.link.offset;
          //  System.out.println("i am: " + n.link.name + " and my offset is: " + n.link.offset);
            code.emit("lw $t0, " + offset + "($sp)");
            push(n.type, "$t0");
        }

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(Plus n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);
        n.right.accept(this);

        pop(n.left.type, "$t2");
        pop(n.right.type, "$t1");

        code.emit("addu $t0, $t1, $t2");

        push(n.type, "$t0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(Minus n)
    {

        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);
        n.right.accept(this);

        pop(n.left.type, "$t2");
        pop(n.right.type, "$t1");

        code.emit("subu $t0, $t1, $t2");

        push(n.type, "$t0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(Times n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);
        n.right.accept(this);

        pop(n.left.type, "$t2");
        pop(n.right.type, "$t1");

        code.emit("mul $t0, $t1, $t2");

        push(n.type, "$t0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(Equals n)
    {

        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);
        n.right.accept(this);

        pop(n.left.type, "$t2");
        pop(n.right.type, "$t1");

        code.emit("seq $t0, $t1, $t2");

        push(n.type, "$t0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(LessThan n)
    {

        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);
        n.right.accept(this);

        pop(n.left.type, "$t2");
        pop(n.right.type, "$t1");

        code.emit("slt $t0, $t1, $t2");

        push(n.type, "$t0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(GreaterThan n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);
        n.right.accept(this);

        pop(n.left.type, "$t2");
        pop(n.right.type, "$t1");

        code.emit("sgt $t0, $t1, $t2");

        push(n.type, "$t0");

        code.comment(n, "end");
        code.emit("");

        return null;
    }

    public Object visit(Divide n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);
        n.right.accept(this);

        code.emit("jal divide");

        stack -= 8;

        code.comment(n, "end");
        code.emit("");


        return null;
    }


    public Object visit(Remainder n)
    {
        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);
        n.right.accept(this);

        code.emit("jal remainder");

        stack -= 8;

        code.comment(n, "end");
        code.emit("");


        return null;
    }

    public Object visit(And n)
    {

        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);

        code.emit("lw $t0, ($sp)");
        code.emit("beq $t0, $0, skip_" + n.uniqueId);
        pop(n.type, "$t0");

        n.right.accept(this);

        code.emit("skip_" + n.uniqueId + ":");

        code.comment(n, "end");
        code.emit("");

        return null;

    }

    public Object visit(Or n)
    {

        code.emit("");
        code.comment(n, "begin");

        n.left.accept(this);

        code.emit("lw $t0, ($sp)");
        code.emit("bne $t0, $0, skip_" + n.uniqueId);
        pop(n.type, "$t0");

        n.right.accept(this);

        code.emit("skip_" + n.uniqueId + ":");

        code.comment(n, "end");
        code.emit("");

        return null;

    }

    

    /* this method pushes things on to the stack */
    public void push(Type t, String reg)
    {
    if (t.isInt())
    {

        
        code.emit("# begin PUSH INT");

        /////////////////////////////////
        
        code.emit("subu $sp , $sp , 8");
        code.emit("sw $s5 , 4($sp)");
        code.emit("sw " + reg + ", ($sp)"); 
        stack += 8;

        /////////////////////////////////

        code.emit("# end PUSH INT");
       
    }
    else
    {

        code.emit("# begin PUSH");

        /////////////////////////////////

        code.emit("subu $sp , $sp , 4");
        code.emit("sw " + reg + ", ($sp)");
        stack += 4;

        /////////////////////////////////

        code.emit("# end PUSH");

        
    }
    }

    /* this method pops integers off the stack  */
    public void pop(Type t, String reg)
    {

        if (t.isInt())
        {
       
        code.emit("# begin POP INT");

        //////////////////////////////////

        code.emit("lw " + reg + ", ($sp)");
        code.emit("addu $sp, $sp, 8");
        stack -= 8;

        /////////////////////////////////

        code.emit("# end POP INT");
        
        }
        else
        {
            
            code.emit("# begin POP INT");
 
         //////////////////////////////////
 
         code.emit("lw " + reg + ", ($sp)");
         code.emit("addu $sp, $sp, 4");
         stack -= 4;
 
         /////////////////////////////////
 
            code.emit("# end POP INT");
            

        }


    }

        /*
     * this method provides space 
     * 
     */
    public void space()
    {
        code.emit("");
        code.emit("");
    }


     /*
     * this method creates a header for a method
     * for better readability
     * 
     */
    public void methodHeader(String methName)
    {

        code.emit("");
        code.emit("################################");
        code.emit("#        " + methName + "       ");
        code.emit("################################");
        code.emit("");


    }
}