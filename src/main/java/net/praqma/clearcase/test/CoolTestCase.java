package net.praqma.clearcase.test;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.Policy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.PVob;
import net.praqma.clearcase.annotations.TestConfiguration;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.UCMEntityNotFoundException;
import net.praqma.clearcase.exceptions.UnableToCreateEntityException;
import net.praqma.clearcase.exceptions.UnableToGetEntityException;
import net.praqma.clearcase.exceptions.UnableToListProjectsException;
import net.praqma.clearcase.exceptions.UnableToLoadEntityException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.view.DynamicView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.util.debug.appenders.Appender;
import net.praqma.util.debug.appenders.ConsoleAppender;

import junit.framework.TestCase;

public abstract class CoolTestCase extends TestCase {

	protected static Logger logger = Logger.getLogger();
	protected static ConsoleAppender appender = new ConsoleAppender();

	protected static boolean rolling = true;
	protected static boolean tearDownAsMuchAsPossible = true;

	static {
		System.out.println( "STATIC" );
		appender.setTemplate( "[%level]%space %message%newline" );
		appender.setMinimumLevel( LogLevel.DEBUG );
		Logger.addAppender( appender );
	}

	protected PVob pvob;
	private String pvobStr;
	protected boolean removePvob = false;
	protected boolean fail = false;

	protected File prefix;

	protected String dynamicViewTag = "TestDynamicView";
	protected String bootstrapViewTag = "TestBootstrapView";
	
	protected DynamicView baseView;
	protected DynamicView bootstrapView;
	private String basepathStr;
	protected File basepath;
	protected File bootstrappath;
	
	protected Project project;
	protected Stream integrationStream;
	
	protected Component systemComponent;
	protected Component modelComponent;
	protected Component clientComponent;
	
	protected Baseline structure;
	
	public CoolTestCase() {
		logger.verbose( "Constructor" );
		
		/* Check options */
		pvobStr = System.getProperty( "pvob", "TESTING_PVOB" );
		basepathStr = System.getProperty( "path", "" );
		prefix = new File( System.getProperty( "path", "m:/" ) );
	}

	public DynamicView getBaseView() {
		return baseView;
	}
	
	public boolean bootStrap( String projectName, String integrationName ) {
		try {
			/* Unrooted component */
			systemComponent = Component.create( "_System", pvob, null, "Unrooted system component", basepath );
			
			/* Rooted components */
			modelComponent = Component.create( "Model", pvob, "Model", "Model component", basepath );
			clientComponent = Component.create( "Client", pvob, "Client", "Client component", basepath );
			
			project = Project.create( projectName, null, pvob, Project.POLICY_INTERPROJECT_DELIVER, "Test", modelComponent, clientComponent );
			integrationStream = Stream.createIntegration( integrationName, project, structure );
			
			/**/
			bootstrapView = DynamicView.create( null, bootstrapViewTag, integrationStream );
			bootstrappath = new File( prefix, bootstrapViewTag + "/" + this.pvob.getName() );
			
			structure = Baseline.create( "Structure", systemComponent, bootstrappath, LabelBehaviour.DEFAULT, false, null, new Component[] { modelComponent, clientComponent } );
			
			return true;
		} catch( ClearCaseException e ) {
			e.print( appender.getOut() );
			return false;
		}
	}

	@Override
	protected void setUp() {
		logger.debug( "Setup ClearCase" );

		String pvob = Cool.filesep + pvobStr;

		removePvob = false;
		PVob pv = PVob.get( pvob );
		if( pv == null ) {
			logger.info( "Creating " + pvob );
			try {
				logger.verbose( "Creating pvob " + pvob );
				this.pvob = PVob.create( pvob, null, "testing" );
				this.pvob.mount();
				logger.verbose( "Creating dynamic view" );
				baseView = DynamicView.create( null, dynamicViewTag, null );
				logger.verbose( "Starting view" );
				new DynamicView( null, dynamicViewTag ).startView();
				removePvob = true;
			} catch( ClearCaseException e ) {
				e.print( System.err );
				fail = true;
			}
		} else {
			logger.fatal( "The PVob " + pvob + " already exists" );
			fail = true;
		}
		
		/* Base path */
		basepath = new File( prefix, dynamicViewTag + "/" + this.pvob.getName() );
		
		/* Prepare */
		try {
			FileUtils.deleteDirectory( basepath );
		} catch( IOException e ) {
			logger.error( "Unable to delete " + basepath.getAbsolutePath() );
		}
		
		basepath.mkdirs();
		
		logger.verbose( "Base path is " + basepath.getAbsolutePath() );
	}

	@Override
	protected void runTest() throws Throwable {
		logger.info( "RUN TEST" );
		if( !fail ) {
			super.runTest();
		} else {
			logger.fatal( "ClearCase not set up, unable to run test" );
			throw new Exception( "ClearCase not set up, unable to run test" );
		}
	}

	@Override
	public void runBare() throws Throwable {
		logger.info( "BEFORE BARE" );
		Thread t = Thread.currentThread();
		String o = getClass().getName() + '.' + t.getName();
		t.setName( "Executing " + getName() );
		try {
			super.runBare();
		} finally {
			t.setName( o );
		}
		logger.info( "AFTER BARE" );
	}

	@Override
	protected void tearDown() {
		logger.info( "Tear down ClearCase" );
		boolean tearDownSuccess = true;

		if( removePvob ) {
			try {
				/* Removing views */
				Set<String> viewTags = UCMView.getViews().keySet();
				for( String viewTag : viewTags ) {
					try {
						UCMView.getViews().get( viewTag ).remove();
					} catch( ClearCaseException e ) {
						tearDownSuccess = false;
						e.print( appender.getOut() );
						if( !tearDownAsMuchAsPossible ) {
							throw e;
						}
					}
				}
				
				/* Removing baseview */
				/*
				logger.verbose( "Removing base view" );
				try {
					baseView.remove();
				} catch( ClearCaseException e ) {
					e.print( appender.getOut() );
					if( !tearDownAsMuchAsPossible ) {
						throw e;
					}
				}
				*/
				
				try {
					logger.info( "Removing PVob " + pvob );
					pvob.remove();
				} catch( ClearCaseException e ) {
					tearDownSuccess = false;
					e.print( appender.getOut() );
					if( !tearDownAsMuchAsPossible ) {
						throw e;
					}
				}
			} catch( ClearCaseException e ) {
				tearDownSuccess = false;
				logger.fatal( "Unable to tear down ClearCase" );
				e.print( System.err );
			}
		}
		
		if( tearDownSuccess ) {
			logger.info( "Tear down is successful" );
		} else {
			logger.fatal( "Tear down failed" );
		}
	}
}
