/*
    This file is part of Structorizer.

    Structorizer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Structorizer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

 ***********************************************************************

    BASH Source Code Generator

    Copyright (C) 2008 Markus Grundner

    This file has been released under the terms of the GNU Lesser General
    Public License as published by the Free Software Foundation.

    http://www.gnu.org/licenses/lgpl.html

 */

package lu.fisch.structorizer.generators;

/******************************************************************************************************
 *
 *      Author:         Markus Grundner
 *
 *      Description:    BASH Source Code Generator
 *
 ******************************************************************************************************
 *
 *      Revision List
 *
 *      Author					Date			Description
 *      ------					----			-----------
 *      Markus Grundner         2008.06.01		First Issue based on KSHGenerator from Jan Peter Kippel
 *      Bob Fisch               2011.11.07      Fixed an issue while doing replacements
 *      Kay Gürtzig             2014.11.16      Bugfixes in operator conversion and enhancements (see comments)
 *      Kay Gürtzig             2015.10.18      Indentation logic and comment insertion revised
 *                                              generateCode(For, String) and generateCde(Root, String) changed 
 *
 ******************************************************************************************************
 *
 *      Comment:		LGPL license (http://www.gnu.org/licenses/lgpl.html).
 *      
 *      2015.10.18 - Bugfixes
 *      - Conversion of functions improved by producing headers that are conformous with BASH syntax
 *      - Conversion of For loops slightly improved (not robust, may still fail with complex expressions as loop parameters
 *      
 *      2014.11.16 - Bugfixes / Enhancement
 *      - conversion of Pascal-like logical operators "and", "or", and "not" supported 
 *      - conversion of comparison and operators accomplished
 *      - comment export introduced 
 *
 ******************************************************************************************************///


import lu.fisch.structorizer.elements.Alternative;
import lu.fisch.structorizer.elements.Call;
import lu.fisch.structorizer.elements.Case;
import lu.fisch.structorizer.elements.Element;
import lu.fisch.structorizer.elements.For;
import lu.fisch.structorizer.elements.Forever;
import lu.fisch.structorizer.elements.Instruction;
import lu.fisch.structorizer.elements.Jump;
import lu.fisch.structorizer.elements.Repeat;
import lu.fisch.structorizer.elements.Root;
import lu.fisch.structorizer.elements.Subqueue;
import lu.fisch.structorizer.elements.While;
import lu.fisch.structorizer.parsers.D7Parser;
import lu.fisch.utils.BString;
import lu.fisch.utils.StringList;


public class BASHGenerator extends Generator {
	

	/************ Fields ***********************/
	@Override
	protected String getDialogTitle()
	{
		return "Export BASH Code ...";
	}
	
	@Override
	protected String getFileDescription()
	{
		return "BASH Source Code";
	}
	
	@Override
	protected String getIndent()
	{
		return " ";
	}
	
	@Override
	protected String[] getFileExtensions()
	{
		String[] exts = {"sh"};
		return exts;
	}
	
    // START KGU 2015-10-18: New pseudo field
    @Override
    protected String commentSymbolLeft()
    {
    	return "#";
    }
    // END KGU 2015-10-18

	/************ Code Generation **************/
	
	private String transform(String _input)
	{
		// START KGU 2014-11-16: comparison and assignment have to be handled more carefully
        String s = _input;
        // variable assignment
        s=s.replace(":=", "<-");
        // testing
        s=s.replace("==", "=");
		s=s.replace("!=", "<>");
        s=s.replace("=", "==");
        s=s.replace("<==", "<=");
        s=s.replace(">==", ">=");
        s=s.replace("<>", "!=");
        _input=s;
        // END KGU 2014-11-16
	
		_input=BString.replace(_input, " <- ", "=");
		_input=BString.replace(_input, "<- ", "=");
		_input=BString.replace(_input, " <-", "=");
		_input=BString.replace(_input, "<-", "=");
		
        // START KGU 2014-11-16 Support for Pascal-style operators		
        // convert Pascal operators
        _input=BString.replace(_input," mod "," % ");
        _input=BString.replace(_input," div "," / ");
        _input=BString.replace(_input," and "," && ");
        _input=BString.replace(_input," or "," || ");
        _input=BString.replace(_input," not "," !");
        _input=BString.replace(_input,"(not ", "(!");
        _input=BString.replace(_input," not(", " !(");
        _input=BString.replace(_input,"(not(", "(!(");
       	if (_input.startsWith("not ") || _input.startsWith("not(")) {
       		_input = "!" + _input.substring(3);
       	}
        _input=BString.replace(_input," xor "," ^ ");	// Might cause some operator preference trouble
        // END KGU 2014-11-06

        StringList empty = new StringList();
            empty.addByLength(D7Parser.preAlt);
            empty.addByLength(D7Parser.postAlt);
            empty.addByLength(D7Parser.preCase);
            empty.addByLength(D7Parser.postCase);
            empty.addByLength(D7Parser.preFor);
            empty.addByLength(D7Parser.postFor);
            empty.addByLength(D7Parser.preWhile);
            empty.addByLength(D7Parser.postWhile);
            empty.addByLength(D7Parser.postRepeat);
            empty.addByLength(D7Parser.preRepeat);
            //System.out.println(empty);
            for(int i=0;i<empty.count();i++)
            {
                _input=BString.replace(_input,empty.get(i),"");
                //System.out.println(i);
            }
            if(!D7Parser.postFor.equals("")){_input=BString.replace(_input,D7Parser.postFor,"to");}

            
/*

                if(!D7Parser.preAlt.equals("")){_input=BString.replace(_input,D7Parser.preAlt,"");}
		if(!D7Parser.postAlt.equals("")){_input=BString.replace(_input,D7Parser.postAlt,"");}
		if(!D7Parser.preCase.equals("")){_input=BString.replace(_input,D7Parser.preCase,"");}
		if(!D7Parser.postCase.equals("")){_input=BString.replace(_input,D7Parser.postCase,"");}
		if(!D7Parser.preFor.equals("")){_input=BString.replace(_input,D7Parser.preFor,"");}
		if(!D7Parser.postFor.equals("")){_input=BString.replace(_input,D7Parser.postFor,"");}
		if(!D7Parser.preWhile.equals("")){_input=BString.replace(_input,D7Parser.preWhile,"");}
		if(!D7Parser.postWhile.equals("")){_input=BString.replace(_input,D7Parser.postWhile,"");}
		if(!D7Parser.preRepeat.equals("")){_input=BString.replace(_input,D7Parser.preRepeat,"");}
		if(!D7Parser.postRepeat.equals("")){_input=BString.replace(_input,D7Parser.postRepeat,"");}
*/
            
		if(!D7Parser.input.equals("")&&_input.indexOf(D7Parser.input+" ")>=0){_input=BString.replace(_input,D7Parser.input+" ","read ");}
		if(!D7Parser.output.equals("")&&_input.indexOf(D7Parser.output+" ")>=0){_input=BString.replace(_input,D7Parser.output+" ","echo ");}
		if(!D7Parser.input.equals("")&&_input.indexOf(D7Parser.input)>=0){_input=BString.replace(_input,D7Parser.input,"read ");}
		if(!D7Parser.output.equals("")&&_input.indexOf(D7Parser.output)>=0){_input=BString.replace(_input,D7Parser.output,"echo ");}
		return _input.trim();
	}
	
	protected void generateCode(Instruction _inst, String _indent) {
		
		if(!insertAsComment(_inst, _indent)) {
			// START KGU 2014-11-16
			insertComment(_inst, _indent);
			// END KGU 2014-11-16
			for(int i=0;i<_inst.getText().count();i++)
			{
				code.add(_indent+transform(_inst.getText().get(i)));
			}
		}

	}

	protected void generateCode(Alternative _alt, String _indent) {
		
		code.add("");
		// START KGU 2014-11-16
		insertComment(_alt, _indent);
		// END KGU 2014-11-16
		code.add(_indent+"if "+BString.replace(transform(_alt.getText().getText()),"\n","").trim());
		code.add(_indent+"then");
		generateCode(_alt.qTrue,_indent+this.getIndent());
		
		if(_alt.qFalse.getSize()!=0) {
			
			code.add(_indent+"");
			code.add(_indent+"else");			
			generateCode(_alt.qFalse,_indent+this.getIndent());
			
		}
		
		code.add(_indent+"fi");
		code.add("");
		
	}
	
	protected void generateCode(Case _case, String _indent) {
		
		code.add("");
		// START KGU 2014-11-16
		insertComment(_case, _indent);
		// END KGU 2014-11-16
		code.add(_indent+"case "+transform(_case.getText().get(0))+" in");
		
		for(int i=0;i<_case.qs.size()-1;i++)
		{
			code.add("");
			code.add(_indent+this.getIndent()+_case.getText().get(i+1).trim()+")");
			generateCode((Subqueue) _case.qs.get(i),_indent+this.getIndent()+this.getIndent()+this.getIndent());
			code.add(_indent+this.getIndent()+";;");
		}
		
		if(!_case.getText().get(_case.qs.size()).trim().equals("%"))
		{
			code.add("");
			code.add(_indent+this.getIndent()+"*)");
			generateCode((Subqueue) _case.qs.get(_case.qs.size()-1),_indent+this.getIndent()+this.getIndent());
			code.add(_indent+this.getIndent()+";;");
		}
		code.add(_indent+"esac");
		code.add("");
		
	}
	
	
	protected void generateCode(For _for, String _indent) {
		
		code.add("");
		// START KGU 2014-11-16
		insertComment(_for, _indent);
		// END KGU#53 2014-11-16
		// START KGU 2015-10-18: This resulted in nonsense if the algorithm was a real counting loop
		// We now use form for ((var = sval; var < eval; var=var+incr)) like in C...
		// But of course is the rather blind splitting of the for text hazardous!  
		//code.add(_indent+"for "+BString.replace(BString.replace(transform(_for.getText().getText()),"=", " in "),"\n","").trim());
        String startValueStr="";
        String endValueStr="";
        String stepValueStr="";
        String editStr = BString.replace(transform(_for.getText().getText()),"\n","").trim();
        String[] words = editStr.split("[ =]");
        int nbrWords = words.length;
        String counterStr = words[0];	// FIXME This works only for just some typical examples 
        if (nbrWords > 1) startValueStr = words[1];
        if (nbrWords > 3) endValueStr = words[3];
        if (nbrWords > 5) {
                stepValueStr = words[5];
        }
        else {
                stepValueStr = "1";
        }
        String incrStr = counterStr + "++";
        if (!stepValueStr.equals("1")) incrStr = "(( "+counterStr+"="+counterStr+"+("+stepValueStr+") ))";
        code.add(_indent+"for (("+
                        counterStr+"="+startValueStr+"; "+counterStr+"<="+endValueStr+"; " + incrStr + " ))");
		// END KGU#53 2015-10-18
		code.add(_indent+"do");
		generateCode(_for.q,_indent+this.getIndent());
		code.add(_indent+"done");	
		code.add("");
		
	}
	protected void generateCode(While _while, String _indent) {
		
		code.add("");
		// START KGU 2014-11-16
		insertComment(_while, _indent);
		// END KGU 2014-11-16
		code.add(_indent+"while "+BString.replace(transform(_while.getText().getText()),"\n","").trim());
		code.add(_indent+"do");
		generateCode(_while.q,_indent+this.getIndent());
		code.add(_indent+"done");
		code.add("");
		
	}
	
	protected void generateCode(Repeat _repeat, String _indent) {
		
		code.add("");
		// START KGU 2014-11-16
		insertComment(_repeat, _indent);
		// END KGU 2014-11-16
		code.add(_indent+"until "+BString.replace(transform(_repeat.getText().getText()),"\n","").trim());
		code.add(_indent+"do");
		generateCode(_repeat.q,_indent+this.getIndent());
		code.add(_indent+"done");
		code.add("");
		
	}
	protected void generateCode(Forever _forever, String _indent) {
		
		code.add("");
		// START KGU 2014-11-16
		insertComment(_forever, _indent);
		// END KGU 2014-11-16
		code.add(_indent+"while [1]");
		code.add(_indent+"do");
		generateCode(_forever.q,_indent+this.getIndent());
		code.add(_indent+"done");
		code.add("");
		
	}
	
	protected void generateCode(Call _call, String _indent) {
		if(!insertAsComment(_call, _indent)) {
			// START KGU 2014-11-16
			insertComment(_call, _indent);
			// END KGU 2014-11-16
			for(int i=0;i<_call.getText().count();i++)
			{
				code.add(_indent+transform(_call.getText().get(i))+";");
			}
		}
	}
	
	protected void generateCode(Jump _jump, String _indent) {
		if(!insertAsComment(_jump, _indent)) {
			// START KGU 2014-11-16
			insertComment(_jump, _indent);
			// END KGU 2014-11-16
			for(int i=0;i<_jump.getText().count();i++)
			{
				code.add(_indent+transform(_jump.getText().get(i))+";");
			}
		}
	}
	
	protected void generateCode(Subqueue _subqueue, String _indent) {
		
		for(int i=0;i<_subqueue.children.size();i++)
		{
			generateCode((Element) _subqueue.children.get(i),_indent);
		}
		
	}
	
	public String generateCode(Root _root, String _indent) {
		
		code.add("#!/bin/bash");
		code.add("");

		// START KGU 2014-11-16
		insertComment(_root, _indent);
		// END KGU 2014-11-16
		String indent = _indent;
		insertComment("(generated by structorizer)", indent);
		
		if( ! _root.isProgram ) {
			// START KGU#53 2015-10-18: Shell functions get their arguments via $1, $2 etc.
			//code.add(_root.getText().get(0)+" () {");
			String header = _root.getMethodName() + "()";
			code.add(header + " {");
			indent = indent + this.getIndent();
			StringList paraNames = _root.getParameterNames();
			for (int i = 0; i < paraNames.count(); i++)
			{
				code.add(indent + paraNames.get(i) + "=$" + (i+1));
			}
			// END KGU#53 2015-10-18
		} else {				
			code.add("");
		}
		
		code.add("");
		generateCode(_root.children, indent);
		
		if( ! _root.isProgram ) {
			code.add("}");
		}
		
		return code.getText();
		
	}
	
}


