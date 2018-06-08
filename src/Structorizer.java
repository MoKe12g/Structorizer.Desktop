/*
    Structorizer
    A little tool which you can use to create Nassi-Schneiderman Diagrams (NSD)

    Copyright (C) 2009  Bob Fisch

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any
    later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/******************************************************************************************************
 *
 *      Author:         Bob Fisch
 *
 *      Description:    Structorizer class (main entry point for interactive and batch mode)
 *
 ******************************************************************************************************
 *
 *      Revision List
 *
 *      Author          Date			Description
 *      ------          ----			-----------
 *      Bob Fisch       2007.12.27		First Issue
 *      Kay Gürtzig     2015.12.16      Bugfix #63 - no open attempt without need
 *      Kay Gürtzig     2016.04.28      First draft for enh. #179 - batch generator mode (KGU#187)
 *      Kay Gürtzig     2016.05.03      Prototype for enh. #179 - incl. batch parser and help (KGU#187)
 *      Kay Gürtzig     2016.05.08      Issue #185: Capability of multi-routine import per file (KGU#194)
 *      Kay Gürtzig     2016.12.02      Enh. #300: Information about updates on start in interactive mode
 *                                      Modification in command-line concatenation
 *      Kay Gürtzig     2016.12.12      Issue #306: multiple arguments in simple command line are now
 *                                      interpreted as several files to be opened in series.
 *      Kay Gürtzig     2017.01.27      Issue #306 + #290: Support for Arranger files in command line
 *      Kay Gürtzig     2017.03.04      Enh. #354: Configurable set of import parsers supported now
 *      Kay Gürtzig     2017.04.27      Enh. #354: Verbose option (-v with log directory) for batch import
 *      Kay Gürtzig     2017.07.02      Enh. #354: Parser-specific options retrieved from Ini, parser cloned.
 *      Kay Gürtzig     2017.11.06      Issue #455: Loading of argument files put in a sequential thread to overcome races
 *      Kay Gürtzig     2018.03.21      Issue #463: Logging configuration via file logging.properties
 *      Kay Gürtzig     2018.06.07      Issue #463: Logging configuration mechanism revised (to support WebStart)
 *      Kay Gürtzig     2018.06.08      Issue #536: Precaution against command line argument trouble
 *
 ******************************************************************************************************
 *
 *      Comment:		
 *      
 ******************************************************************************************************///


import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import lu.fisch.structorizer.arranger.Arranger;
import lu.fisch.structorizer.elements.Root;
import lu.fisch.structorizer.generators.Generator;
import lu.fisch.structorizer.generators.XmlGenerator;
import lu.fisch.structorizer.gui.Mainform;
import lu.fisch.structorizer.helpers.GENPlugin;
import lu.fisch.structorizer.io.Ini;
import lu.fisch.structorizer.parsers.CodeParser;
import lu.fisch.structorizer.parsers.GENParser;
import lu.fisch.structorizer.parsers.NSDParser;
import lu.fisch.utils.StringList;

public class Structorizer
{

	// entry point
	public static void main(String args[])
	{
		// START KGU#484 2018-03-21: Issue #463 Configurability of the logging system ensured
		// The logging configuration (for java.util.logging) is expected next to the jar file
		// (or in the project directory while debugged from the IDE).
		// FIXME: Check a proper configuration scenario for Java WebStart!
		File iniDir = Ini.getIniDirectory();
		File configFile = new File(iniDir.getAbsolutePath() + System.getProperty("file.separator") + "logging.properties");
		// If the file doesn't exist then we'll copy it from the resource
		if (!configFile.exists()) {
			InputStream configStr = Structorizer.class.getResourceAsStream("/lu/fisch/structorizer/logging.properties");
			if (configStr != null) {
				copyStream(configStr, configFile);
			}
		}
		if (configFile.exists()) {
			System.setProperty("java.util.logging.config.file", configFile.getAbsolutePath());
			try {
				LogManager.getLogManager().readConfiguration();
			} catch (SecurityException | IOException e) {
				// Just write the trace to System.err
				e.printStackTrace();
			}
		}
		// END KGU#484 2018-03-21
		// START KGU#484 2018-04-05: Issue #463 - Find out where WebStart assumes the properties file
		else {
			File logLogFile = new File(iniDir.getAbsolutePath(), "Structorizer.log");
			try {
				OutputStreamWriter logLog =	new OutputStreamWriter(new FileOutputStream(logLogFile), "UTF-8");
				logLog.write("No logging config file in dir " + iniDir + " - using Java logging standard.");
				logLog.close();
			} catch (IOException e) {
				// Just write the trace to System.err
				e.printStackTrace();
			}
		}
		// END KGU#484 2018-04-05
		// START KGU#187 2016-04-28: Enh. #179
		Vector<String> fileNames = new Vector<String>();
		String generator = null;
		String parser = null;
		String options = "";
		String outFileName = null;
		String charSet = "UTF-8";
		// START KGU#354 2017-04-27:Enh. #354
		String logDir = null;
		// END KGU#354 2017-04-27
		//System.out.println("arg 0: " + args[0]);
		if (args.length == 1 && args[0].equals("-h"))
		{
			printHelp();
			return;
		}
		for (int i = 0; i < args.length; i++)
		{
			//System.out.println("arg " + i + ": " + args[i]);
			if (i == 0 && args[i].equals("-x") && args.length > 1)
			{
				generator = args[++i];
			}
			else if (i == 0 && args[i].equals("-p") && args.length > 1)
			{
				parser = "*";
			}
			// Legacy support - parsers will now be derived from the file extensions 
			else if (i > 0 && (parser != null) && (args[i].equalsIgnoreCase("pas") || args[i].equalsIgnoreCase("pascal"))
					&& !parser.endsWith("pas")) {
				parser += "pas";
			}
			else if (args[i].equals("-o") && i+1 < args.length)
			{
				outFileName = args[++i];
			}
			else if (args[i].equals("-e") && i+1 < args.length)
			{
				charSet = args[++i];
			}
			// START KGU#354 2017-04-27: Enh. #354 verbose mode?
			else if (args[i].equals("-v") && i+1 < args.length)
			{
				logDir = args[++i];
			}
			// END KGU#354 2017-04-27
			// Target standard output?
			else if (args[i].equals("-"))
			{
				options += "-";
			}
			// Other options
			else if (args[i].startsWith("-"))
			{
				options += args[i].substring(1);
			}
			else
			{
				fileNames.add(args[i]);
			}
		}
		if (generator != null)
		{
			Structorizer.export(generator, fileNames, outFileName, options, charSet);
			return;
		}
		else if (parser != null)
		{
			// START KGU#354 2017-04-27: Enh. #354 verbose mode
			//Structorizer.parse(parser, fileNames, outFileName, options, charSet);
			Structorizer.parse(parser, fileNames, outFileName, options, charSet, logDir);
			// END KGU#354 2017-04-27
			return;
		}
		// END KGU#187 2016-04-28
		
		// try to load the system Look & Feel
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e)
		{
			//System.out.println("Error setting native LAF: " + e);
		}

		// load the mainform
		final Mainform mainform = new Mainform();

		// START KGU#440 2017-11-06: Issue #455 Decisive measure against races on loading an drawing
        try {
        	EventQueue.invokeAndWait(new Runnable() {
        		@Override
        		public void run() {
        // END KGU#440 2017-11-06
        			//String s = new String();
        			int start = 0;
        			if (args.length > 0 && args[0].equals("-open")) {
        				start = 1;
        			}
        			// FIXME (KGU 2016-12-12): This concatenation still doesn't make sense...
        			// (If the file name contained blanks then the OS should have quoted it,
        			// if the command line contained several file names, on the other hand, then
        			// they would have to be loaded separately - this could be done by moving
        			// the previously loaded one to the Arranger on each consecutive load.)
        			for (int i=start; i<args.length; i++)
        			{
        				// START KGU#306 2016-12-12/2017-01-27: This seemed to address file names with blanks...
        				//s += args[i];
        				String s = args[i].trim();
        				String lastExt = "";	// Last file extension
        				if (!s.isEmpty())
        				{
        					if (lastExt.equals("nsd") && !mainform.diagram.getRoot().isEmpty()) {
        						// Push the previously loaded diagram to Arranger
        						mainform.diagram.arrangeNSD();
        					}
        					lastExt = mainform.diagram.openNsdOrArr(s);
        					// START KGU#521 2018-06-08: Bugfix #536 (try)
        					if (lastExt == "") {
        						String msg = "Unsuited or misplaced command line argument \"" + s + "\" ignored.";
        						Logger.getLogger(Structorizer.class.getName()).log(Level.WARNING, msg);
        						JOptionPane.showMessageDialog(mainform, msg,
        								"Command line", JOptionPane.WARNING_MESSAGE);
        					}
        					// END KGU#521 2018-06-08
        				}
        				// END KGU#306 2016-12-12/2017-01-27
        			}
       	// START KGU#440 2017-11-06: Issue #455 Decisive measure against races on loading an drawing
        		}
        		// START KGU#306 2016-12-12: Enh. #306 - Replaced with the stuff in the loop above
//			s = s.trim();
//			// START KGU#111 2015-12-16: Bugfix #63 - no open attempt without need
//			//mainform.diagram.openNSD(s);
//			if (!s.isEmpty())
//			{
//				mainform.diagram.openNSD(s);
//			}
//			// END KGU#111 2015-12-16
        	// END KGU#306 2016-12-12
        	});
        } catch (InvocationTargetException e1) {
        	e1.printStackTrace();
        } catch (InterruptedException e1) {
        	e1.printStackTrace();
        }
        // END KGU#440 2017-11-06
        mainform.diagram.redraw();


		if(System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
		{

			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("apple.awt.graphics.UseQuartz", "true");

			com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();

			try
			{
				application.setEnabledPreferencesMenu(true);
				application.addApplicationListener(new com.apple.eawt.ApplicationAdapter() {
					public void handleAbout(com.apple.eawt.ApplicationEvent e) {
						mainform.diagram.aboutNSD();
						e.setHandled(true);
					}
					public void handleOpenApplication(com.apple.eawt.ApplicationEvent e) {
					}
					public void handleOpenFile(com.apple.eawt.ApplicationEvent e) {
						if(e.getFilename()!=null)
						{
							mainform.diagram.openNSD(e.getFilename());
						}
					}
					public void handlePreferences(com.apple.eawt.ApplicationEvent e) {
						mainform.diagram.preferencesNSD();
					}
					public void handlePrintFile(com.apple.eawt.ApplicationEvent e) {
						mainform.diagram.printNSD();
					}
					public void handleQuit(com.apple.eawt.ApplicationEvent e) {
						mainform.saveToINI();
						mainform.dispose();
					}
				});
			}
			catch (Exception e)
			{
			}
		}/**/

		// Without this, the toolbar had often wrong status when started from a diagram 
		mainform.doButtons();
		// START KGU#300 2016-12-02
		mainform.popupWelcomePane();
		// END KGU#300 2016-12-02
	}
	
	// START KGU#187 2016-05-02: Enh. #179
	private static String[] synopsis = {
		"Structorizer [NSDFILE]",
		"Structorizer -x GENERATOR [-b] [-c] [-f] [-l] [-t] [-e CHARSET] [-] [-o OUTFILE] NSDFILE...",
		"Structorizer -p [pas|pascal] [-f] [-v LOGPATH] [-e CHARSET] [-o OUTFILE] SOURCEFILE...",
		"Structorizer -h"
	};
	// END KGU#187 2016-05-02
	
	// START KGU#187 2016-04-28: Enh. #179
	/*****************************************
	 * batch code export methods
	 *****************************************/
	public static void export(String _generatorName, Vector<String> _nsdFileNames, String _codeFileName, String _options, String _charSet)
	{
		Vector<Root> roots = new Vector<Root>();
		for (String fName : _nsdFileNames)
		{
			Root root = null;
			try
			{
				// Test the existence of the current NSD file
				File f = new File(fName);
				if (f.exists())
				{
					// open an existing file
					NSDParser parser = new NSDParser();
					// START KGU#363 2017-05-21: Issue #372 API change
					//root = parser.parse(f.toURI().toString());
					root = parser.parse(f);
					// END KGU#363 2017-05-21
					root.filename = fName;
					roots.add(root);
					// If no output file name is given then derive one from the first NSD file
					if (_codeFileName == null && _options.indexOf('-') < 0)
					{
						_codeFileName = f.getCanonicalPath();
					}
				}
				else
				{
					System.err.println("*** File " + fName + " not found. Skipped.");
				}
			}
			catch (Exception e)
			{
				System.err.println("*** Error while trying to load " + fName + ": " + e.getMessage());
			}
		}
		
		String genClassName = null;
		if (!roots.isEmpty())
		{
			String usage = "Usage: " + synopsis[1] + "\nwith GENERATOR =";
			// We just (ab)use some class residing in package gui to fetch the plugin configuration 
			BufferedInputStream buff = new BufferedInputStream(lu.fisch.structorizer.gui.EditData.class.getResourceAsStream("generators.xml"));
			GENParser genp = new GENParser();
			Vector<GENPlugin> plugins = genp.parse(buff);
			try { buff.close();	} catch (IOException e) {}
			for (int i=0; genClassName == null && i < plugins.size(); i++)
			{
				GENPlugin plugin = (GENPlugin) plugins.get(i);
				StringList names = StringList.explode(plugin.title, "/");
				String className = plugin.getKey();
				usage += (i>0 ? " |" : "") + "\n\t" + className;
				if (className.equalsIgnoreCase(_generatorName))
				{
					genClassName = plugin.className;
				}
				else
				{
					for (int j = 0; j < names.count(); j++)
					{
						if (names.get(j).trim().equalsIgnoreCase(_generatorName))
							genClassName = plugin.className;
						usage += " | " + names.get(j).trim();
					}
				}
			}

			if (genClassName == null)
			{
				System.err.println("*** Unknown code generator \"" + _generatorName + "\"");
				System.err.println(usage);
				System.exit(1);
			}
			
			try
			{
				Class<?> genClass = Class.forName(genClassName);
				Generator gen = (Generator) genClass.newInstance();
				gen.exportCode(roots, _codeFileName, _options, _charSet);
			}
			catch(java.lang.ClassNotFoundException ex)
			{
				System.err.println("*** Generator class " + ex.getMessage() + " not found!");
				System.exit(3);
			}
			catch(Exception e)
			{
				System.err.println("*** Error while using " + _generatorName + "\n" + e.getMessage());
				e.printStackTrace();
				System.exit(4);
			}
		}
		else
		{
			System.err.println("*** No NSD files for code generation.");
			System.exit(2);
		}
	}
	// END KGU#187 2016-04-28
	
	// START KGU#187 2016-04-29: Enh. #179 - for symmetry reasons also allow a parsing in batch mode
	/*****************************************
	 * batch code import methods
	 * @param _logDir - Path of the target folder for the parser log
	 *****************************************/
	private static void parse(String _parserName, Vector<String> _filenames, String _outFile, String _options, String _charSet, String _logDir)
	{
		
		String usage = "Usage: " + synopsis[2] + "\nAccepted file extensions:";

		String fileExt = null;
		// START KGU#354 2017-03-10: Enh. #354 configurable parser plugins
		// Initialize the mapping file extensions -> CodeParser
		// We just (ab)use some class residing in package gui to fetch the plugin configuration 
		BufferedInputStream buff = new BufferedInputStream(lu.fisch.structorizer.gui.EditData.class.getResourceAsStream("parsers.xml"));
		GENParser genp = new GENParser();
		Vector<GENPlugin> plugins = genp.parse(buff);
		try { buff.close();	} catch (IOException e) {}
		HashMap<CodeParser, GENPlugin> parsers = new HashMap<CodeParser, GENPlugin>();
		//String parsClassName = null;
		CodeParser parser = null;
		for (int i=0; i < plugins.size(); i++)
		{
			GENPlugin plugin = (GENPlugin) plugins.get(i);
			String className = plugin.className;
			try
			{
				Class<?> parsClass = Class.forName(className);
				parser = (CodeParser) parsClass.newInstance();
				parsers.put(parser, plugin);
				usage += "\n\t";
				for (String ext: parser.getFileExtensions()) {
					usage += ext + ", ";
				}
				// Get rid of last ", " 
				if (usage.endsWith(", ")) {
					usage = usage.substring(0, usage.length()-2) + " for " + parser.getDialogTitle();
				}
			}
			catch(java.lang.ClassNotFoundException ex)
			{
				System.err.println("*** Parser class " + ex.getMessage() + " not found!");
			}
			catch(Exception e)
			{
				System.err.println("*** Error on creating " + _parserName + "\n" + e.getMessage());
				e.printStackTrace();
			}
		}

		// END KGU#354 2017-03-10
		
		// START KGU#193 2016-05-09: Output file name specification was ignored, option f had to be tuned.
		boolean overwrite = _options.indexOf("f") >= 0 && 
				!(_outFile != null && !_outFile.isEmpty() && _filenames.size() > 1);
		// END KGU#193 2016-05-09
		
		// START KGU#354 2017-03-03: Enh. #354 - support several parser plugins
		// While there was only one input language candidate, a single Parser instance had been enough
		//D7Parser d7 = new D7Parser("D7Grammar.cgt");
		// END KGU#354 2017-03-04
		
		for (String filename : _filenames)
		{
			// START KGU#194 2016-05-08: Bugfix #185 - face more contained roots
			//Root rootNew = null;
			List<Root> newRoots = new LinkedList<Root>();
			// END KGU#194 2016-05-08
			// START KGU#354 2017-03-04: Enh. #354
			//if (fileExt.equals("pas"))
			File importFile = new File(filename);
			parser = null;
			// START KGU#416 2017-07-02: Enh. #354, #409 Parser retrieval combined with option retrieval
			for (Entry<CodeParser, GENPlugin> entry: parsers.entrySet()) {
				if (entry.getKey().accept(importFile)) {
					parser = cloneWithPluginOptions(entry.getValue());
					break;
				}
			}
			// END KGU#416 2017-07-02
			if (parser != null)
			// END KGU#354 2017-03-09
			{
				//D7Parser d7 = new D7Parser("D7Grammar.cgt");
				// START KGU#194 2016-05-04: Bugfix for 3.24-11 - encoding wasn't passed
				//rootNew = d7.parse(filename);
				// START KGU#354 2017-04-27: Enh. #354 pass in the log directory path
				//newRoots = parser.parse(filename, _charSet);
				newRoots = parser.parse(filename, _charSet, _logDir);
				// END KGU#354 2017-04-27
				// END KGU#194 2016-05-04
				if (!parser.error.isEmpty())
				{
					System.err.println("Parser error in file " + filename + "\n" + parser.error);
					continue;
				}
			}
			else {
				System.err.println("File " + filename + " not accpeted by any parser!");
			}
		
			// Now save the roots as NSD files. Derive the target file names from the source file name
			// if _outFile isn't given.
			// START KGU#193 2016-05-09: Output file name specification was ignred, optio f had to be tuned.
			if (_outFile != null && !_outFile.isEmpty())
			{
				filename = _outFile;
			}
			// END KGU#193 2016-05-09
			// START KGU#194 2016-05-08: Bugfix #185 - face more contained roots
			//if (rootNew != null)
			boolean multipleRoots = newRoots.size() > 1;
			for (Root rootNew : newRoots)
			// END KGU#194 2016-05-08
			{
				StringList nameParts = StringList.explode(filename, "[.]");
				String ext = nameParts.get(nameParts.count()-1).toLowerCase();
				if (ext.equals(fileExt))
				{
					nameParts.set(nameParts.count()-1, "nsd");
				}
				else if (!ext.equals("nsd"))
				{
					nameParts.add("nsd");
				}
				// In case of multiple roots (subroutines) insert the routine's proposed file name
				if (multipleRoots && !rootNew.isProgram())
				{
					nameParts.insert(rootNew.proposeFileName(), nameParts.count()-1);
				}
				//System.out.println("File name raw: " + nameParts);
				if (!overwrite)
				{
					int count = 0;
					do {
						File file = new File(nameParts.concatenate("."));
						if (file.exists())
						{
							if (count == 0) {
								nameParts.insert(Integer.toString(count), nameParts.count()-1);
							}
							else {
								nameParts.set(nameParts.count()-2, Integer.toString(count));
							}
							count++;
						}
						else
						{
							overwrite = true;
						}
					} while (!overwrite);
				}
				String filenameToUse = nameParts.concatenate(".");
				//System.out.println("Writing to " + filename);
				try {
					FileOutputStream fos = new FileOutputStream(filenameToUse);
					Writer out = null;
					out = new OutputStreamWriter(fos, "UTF8");
					XmlGenerator xmlgen = new XmlGenerator();
					out.write(xmlgen.generateCode(rootNew,"\t"));
					out.close();
				}
				catch (UnsupportedEncodingException e) {
					System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
				}
				catch (IOException e) {
					System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			}
		}
	}
	// END KGU#187 2016-04-29
	
	// START KGU#416 2017-07-03: Enh. #354, #409
	private static CodeParser cloneWithPluginOptions(GENPlugin plugin) {
		CodeParser parser;
		try {
			parser = (CodeParser)Class.forName(plugin.getKey()).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
			System.err.println("Structorizer.CloneWithPluginSpecificOptions("
					+ plugin.getKey()
					+ "): " + ex.getMessage() + " on creating \"" + plugin.getKey()
					+ "\"");
			return null;
		}
		Ini ini = Ini.getInstance();
		if (!plugin.options.isEmpty()) {
			try {
				ini.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (HashMap<String, String> optionSpec: plugin.options) {
			String optionKey = optionSpec.get("name");
			String valueStr = ini.getProperty(plugin.getKey() + "." + optionKey, "");
			Object value = null;
			String type = optionSpec.get("type");
			String items = optionSpec.get("items");
			// Now convert the option into the specified type
			if (!valueStr.isEmpty() && type != null || items != null) {
				// Better we fail with just a single option than with the entire method
				try {
					if (items != null) {
						value = valueStr;
					}
					else if (type.equalsIgnoreCase("character")) {
						value = valueStr.charAt(0);
					}
					else if (type.equalsIgnoreCase("boolean")) {
						value = Boolean.parseBoolean(valueStr);
					}
					else if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("integer")) {
						value = Integer.parseInt(valueStr);
					}
					else if (type.equalsIgnoreCase("unsiged")) {
						value = Integer.parseUnsignedInt(valueStr);
					}
					else if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float")) {
						value = Double.parseDouble(valueStr);
					}
					else if (type.equalsIgnoreCase("string")) {
						value = valueStr;
					}
				}
				catch (NumberFormatException ex) {
					System.err.println("Structorizer.CloneWithPluginSpecificOptions("
							+ plugin.getKey()
							+ "): " + ex.getMessage() + " on converting \""
							+ valueStr + "\" to " + type + " for " + optionKey);
				}
			}
			if (value != null) {
				parser.setPluginOption(optionKey, value);
			}
		}
		return parser;
	}
	// END KGU#416 2017-07-02

	// START KGU#187 2016-05-02: Enh. #179 - help might be sensible
	private static void printHelp()
	{
		System.out.print("Usage:\n");
		for (int i = 0; i < synopsis.length; i++)
		{
			System.out.println(synopsis[i]);
		}
		System.out.println("with");
		System.out.print("\tGENERATOR = ");
		// We just (ab)use some class residing in package gui to fetch the plugin configuration 
		BufferedInputStream buff = new BufferedInputStream(lu.fisch.structorizer.gui.EditData.class.getResourceAsStream("generators.xml"));
		GENParser genp = new GENParser();
		Vector<GENPlugin> plugins = genp.parse(buff);
		try { buff.close();	} catch (IOException e) {}
		for (int i=0; i < plugins.size(); i++)
		{
			GENPlugin plugin = (GENPlugin) plugins.get(i);
			StringList names = StringList.explode(plugin.title, "/");
			String className = plugin.getKey();
			System.out.print( (i>0 ? " |" : "") + "\n\t\t" + className );
			for (int j = 0; j < names.count(); j++)
			{
				System.out.print(" | " + names.get(j).trim());
			}
		}
		System.out.println("\n\tPARSER = ");
		String[] names = {"pas", "pascal"};
		for (int j = 0; j < names.length; j++)
		{
			System.out.print((j > 0 ? " | " : "\t\t") + names[j]);
		}
		System.out.println("");
	}
	// END KGU#187 2016-05-02
	
	/** @return the installation path of Structorizer */
    public static String getApplicationPath()
    {
        CodeSource codeSource = Structorizer.class.getProtectionDomain().getCodeSource();
        File rootPath = null;
        try {
            rootPath = new File(codeSource.getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }           
        return rootPath.getParentFile().getPath();
    }
		
	/**
	 * Performs a bytewise copy of {@code sourceFile} to {@code targetFile} as workaround
	 * for Linux where {@link File#renameTo(File)} may fail among file systems. If the
	 * target file exists after the copy the source file will be removed
	 * @param sourceFile
	 * @param targetFile
	 * @param removeSource - whether the {@code sourceFile} is to be removed after a successful
	 * copy
	 * @return in case of errors, a string describing them.
	 */
	private static String copyStream(InputStream sourceStrm, File targetFile) {
		String problems = "";
		final int BLOCKSIZE = 512;
		byte[] buffer = new byte[BLOCKSIZE];
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetFile.getAbsolutePath());
			int readBytes = 0;
			do {
				readBytes = sourceStrm.read(buffer);
				if (readBytes > 0) {
					fos.write(buffer, 0, readBytes);
				}
			} while (readBytes > 0);
		} catch (FileNotFoundException e) {
			problems += e + "\n";
		} catch (IOException e) {
			problems += e + "\n";
		}
		finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {}
			}
			try {
				sourceStrm.close();
			} catch (IOException e) {}
		}
		return problems;
	}
}
