package net.praqma.clearcase.cleartool;


import java.io.File;

import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.praqma.util.execute.CmdResult;
import net.praqma.util.execute.Command;
import net.praqma.util.execute.CommandLineException;



/**
 * The Cleartool proxy class
 * All calls to cleartool, should be done through these static functions.
 * run( String )  : returns the return value as String.
 * run_a( String ): returns the return value as an array of Strings, separated by new lines. 
 * @author wolfgang
 *
 */
public abstract class Cleartool
{	
	public static CmdResult run( String cmd ) throws CommandLineException, AbnormalProcessTerminationException
	{
		return Command.run( "cleartool " + cmd, null, false );
	}
	
	public static CmdResult run( String cmd, File dir ) throws CommandLineException, AbnormalProcessTerminationException
	{
		return Command.run( "cleartool " + cmd, dir, false );
	}
	
	public static CmdResult run( String cmd, File dir, boolean merge ) throws CommandLineException, AbnormalProcessTerminationException
	{
		return Command.run( "cleartool " + cmd, dir, merge );
	}
}