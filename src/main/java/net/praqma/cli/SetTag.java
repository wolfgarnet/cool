package net.praqma.cli;

import java.io.File;
import java.io.IOException;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.UCMException.UCMType;
import net.praqma.clearcase.ucm.entities.Tag;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.util.BuildNumberStamper;
import net.praqma.util.option.Option;
import net.praqma.util.option.Options;

public class SetTag
{
	public static void main( String[] args ) throws UCMException
	{
		Options o = new Options();
		
		Option oentity  = new Option( "entity", "e", true, 1, "The UCM entity" );
		Option otag     = new Option( "tag", "t", true, 1, "The tag. Given as: \"key1=val1&key2=val2\"" );
		Option otagtype = new Option( "tagtype", "y", true, 1, "The tag type" );
		Option otagid   = new Option( "tagid", "i", true, 1, "The tag id" );
		Option ohelp    = new Option( "help", "h", false, 0, "The help" );
		
		o.setOption( oentity );
		o.setOption( otag );
		o.setOption( otagtype );
		o.setOption( otagid );
		o.setOption( ohelp );
		
		o.parse( args );
		
		if( ohelp.used )
		{
			o.display();
			System.exit( 0 );
		}
		
		try
		{
			o.checkOptions();
		}
		catch ( Exception e )
		{
			System.err.println( "Incorrect option: " + e.getMessage() );
			o.display();
			System.exit( 1 );
		}
		
		/* Do the ClearCase thing... */
		UCM.SetContext( UCM.ContextType.CLEARTOOL );
		
		UCMEntity e = UCMEntity.GetEntity( oentity.getString() );
		
		Tag tag = e.GetTag( otagtype.getString(), otagid.getString() );
		
		/* Split key value structure */
		String[] tags = otag.getString().split( "&" );

		System.out.println( "TAGS IN USE: " + otag.getString() );
		
		for( String t : tags )
		{
			System.out.println( "TAG: " + t );
			
			String[] entry = t.split( "=" );
			
			System.out.println( entry[0] + ", " + entry[1] );
			
			tag.SetEntry( entry[0].trim(), entry[1].trim() );
		}
		
		try
		{
			tag.Persist();
		}
		catch( UCMException ex )
		{
			if( ex.type == UCMType.TAG_CREATION_FAILED )
			{
				System.err.println( "Could not persist the tag." );
				System.exit( 1 );
			}
		}
		
		if( tag.IsCreated() )
		{
			System.out.println( "Tag created" );
		}
		else
		{
			System.out.println( "Tag updated" );
		}
	}
		
}
