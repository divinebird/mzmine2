/*
 * Copyright 2006-2014 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.Rserve.RConnection;

/**
 * Utilities for interfacing with R.
 * 
 * @author $Author$
 * @version $Revision$
 */
public class RUtilities {

	// Logger.
	private static final Logger LOG = Logger.getLogger(RUtilities.class
			.getName());

	//** JRI utilities

	/**
	 * R semaphore - all usage of R engine must be synchronized using this
	 * semaphore.
	 */
	public static final Object R_SEMAPHORE = new Object();

	// An R Engine singleton.
	private static Rengine rEngine = null;

	/**
	 * Utility class - no public access.
	 */
	private RUtilities() {
		// no public access.
	}

	/**
	 * Gets the R Engine.
	 * 
	 * @return the R Engine - creating it if necessary.
	 */
	public static Rengine getREngine() {

		synchronized (R_SEMAPHORE) {

			if (rEngine == null) {

				try {

					LOG.finest("Checking R Engine.");

					/*
					 * For some reason if we run Rengine.versionCheck() and R is
					 * not installed, it will crash the JVM. This was observed
					 * at least on Windows and Mac OS X. However, if we call
					 * System.loadLibrary("jri") before calling Rengine class,
					 * the crash is avoided and we can catch the
					 * UnsatisfiedLinkError properly.
					 */
					System.loadLibrary("jri");

					if (!Rengine.versionCheck()) {
						throw new IllegalStateException("JRI version mismatch");
					}

				} catch (UnsatisfiedLinkError error) {
					throw new IllegalStateException(
							"Could not start R. Please check if R is installed and path to the "
									+ "libraries is set properly in the startMZmine script.");
				}

				LOG.finest("Creating R Engine.");
				rEngine = new Rengine(new String[] { "--vanilla" }, false,
						new LoggerConsole());

				LOG.finest("Rengine created, waiting for R.");
				if (!rEngine.waitForR()) {
					throw new IllegalStateException("Could not start R");
				}

			}
			return rEngine;
		}
	}

	/**
	 * Logs all output.
	 */
	private static class LoggerConsole implements RMainLoopCallbacks {
		@Override
		public void rWriteConsole(final Rengine re, final String text,
				final int oType) {
			LOG.finest(text);
		}

		@Override
		public void rBusy(final Rengine re, final int which) {
			LOG.finest("rBusy(" + which + ')');
		}

		@Override
		public String rReadConsole(final Rengine re, final String prompt,
				final int addToHistory) {
			return null;
		}

		@Override
		public void rShowMessage(final Rengine re, final String message) {
			LOG.finest("rShowMessage \"" + message + '\"');
		}

		@Override
		public String rChooseFile(final Rengine re, final int newFile) {
			return null;
		}

		@Override
		public void rFlushConsole(final Rengine re) {
		}

		@Override
		public void rLoadHistory(final Rengine re, final String filename) {
		}

		@Override
		public void rSaveHistory(final Rengine re, final String filename) {
		}
	}


	//    //** JRI + RCaller utilities
	//	static public void checkPackage(String packageName) throws IllegalStateException {
	//
	//		String loadCode = "library(" + packageName + ", logical.return = TRUE)";
	//		String errorMsg = "The \"" + packageName + "\" R package couldn't be loaded - is it installed in R?";
	//
	//		RengineType rEngineType = (rEngine instanceof Rengine) ? RengineType.JRIengine : RengineType.RCaller;
	//		
	//		if (rEngineType == RengineType.JRIengine) {
	//			synchronized (RUtilities.R_SEMAPHORE) {
	//				Rengine rEngine = RUtilities.getREngine();
	//				if (((Rengine) rEngine).eval(loadCode).asBool().isFALSE()) {
	//					throw new IllegalStateException(errorMsg);
	//				}
	//			}
	//		} else {
	//			// New instance
	//			RCaller rcaller = new RCaller();
	//			RCode rcallerCode = new RCode();
	//			
	//			String logicalRet = "pkgOK";
	//			rcallerCode.addRCode(logicalRet + " <- " + loadCode);
	//			rcaller.runAndReturnResultOnline(logicalRet);
	//			if (rcaller.getParser().getAsLogicalArray(logicalRet)[0]) {
	//				throw new IllegalStateException(errorMsg);
	//			}
	//			// Turn instance off
	//			rcaller.stopStreamConsumers();
	//			rcaller.StopRCallerOnline();
	//		}		
	//	}
	//
	//    //** RCaller utilities
	//	//...


	//** RCaller utilities
	/** helper class that consumes output of a process. In addition, it filter output of the REG command on Windows to look for InstallPath registry entry which specifies the location of R. */
	static class StreamHog extends Thread {
		InputStream is;
		boolean capture;
		String installPath;
		StreamHog(InputStream is, boolean capture) {
			this.is = is;
			this.capture = capture;
			start();
		}
		public String getInstallPath() {
			return installPath;
		}
		public void run()
		{
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line = null;
				while ( (line = br.readLine()) != null) {
					if (capture) { // we are supposed to capture the output from REG command
						int i = line.indexOf("InstallPath");
						if (i >= 0) {
							String s = line.substring(i + 11).trim();
							int j = s.indexOf("REG_SZ");
							if (j >= 0)
								s = s.substring(j + 6).trim();
							installPath = s;
							LOG.log(Level.INFO, "R InstallPath = "+s);
						}
					} else 
						LOG.log(Level.INFO, "Rserve>" + line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String getRexecutablePath() {

		String osname = System.getProperty("os.name");
		if (osname != null && osname.length() >= 7 && osname.substring(0,7).equals("Windows")) {
			LOG.log(Level.INFO, "Windows: query registry to find where R is installed ...");
			String installPath = null;
			try {
				Process rp = Runtime.getRuntime().exec("reg query HKLM\\Software\\R-core\\R");
				StreamHog regHog = new StreamHog(rp.getInputStream(), true);
				rp.waitFor();
				regHog.join();
				installPath = regHog.getInstallPath();
			} catch (Exception rge) { 
				LOG.log(Level.SEVERE, "ERROR: unable to run REG to find the location of R: "+rge);
				return null;
			}
			if (installPath == null) {
				LOG.log(Level.SEVERE, "ERROR: canot find path to R. Make sure reg is available and R was installed with registry settings.");
				return null;
			}
			return installPath + "\\bin\\R.exe";
		}

		File f = new File("/Library/Frameworks/R.framework/Resources/bin/R");
		if (f.exists()) return f.getPath();
		f = new File("/usr/local/lib/R/bin/R");
		if (f.exists()) return f.getPath();
		f = new File("/usr/lib/R/bin/R");
		if (f.exists()) return f.getPath();
		f = new File("/sw/bin/R");
		if (f.exists()) return f.getPath();
		f = new File("/usr/common/bin/R");
		if (f.exists()) return f.getPath();
		f = new File("/opt/bin/R");
		if (f.exists()) return f.getPath();


		return null;

	}

	
	//** Rserve utilities
	/**
	 * rosuda
	 */
	/** simple utility that starts Rserve locally if it's not running already - see mainly <code>checkLocalRserve</code> method. It spits out quite some debugging outout of the console, so feel free to modify it for your application if desired.<p>
	 <i>Important:</i> All applications should shutdown every Rserve that they started! Never leave Rserve running if you started it after your application quits since it may pose a security risk. Inform the user if you started an Rserve instance.
	 */
	/** shortcut to <code>launchRserve(cmd, "--no-save --slave", "--no-save --slave", false)</code> */
	public static boolean launchRserve(String cmd) { return launchRserve(cmd, "--no-save --slave --vanilla", "--no-save --slave --vanilla --RS-conf Rserve.conf --RS-enable-control --RS-pidfile /home/golgauth/Rserve_pid.txt", false); }

	/** attempt to start Rserve. Note: parameters are <b>not</b> quoted, so avoid using any quotes in arguments
		 @param cmd command necessary to start R
		 @param rargs arguments are are to be passed to R
		 @param rsrvargs arguments to be passed to Rserve
		 @return <code>true</code> if Rserve is running or was successfully started, <code>false</code> otherwise.
	 */
	public static boolean launchRserve(String cmd, String rargs, String rsrvargs, boolean debug) {
		try {
			Process p;
			boolean isWindows = false;
			String osname = System.getProperty("os.name");
			if (osname != null && osname.length() >= 7 && osname.substring(0,7).equals("Windows")) {
				isWindows = true; /* Windows startup */
				p = Runtime.getRuntime().exec("\""+cmd+"\" -e \"library(Rserve);Rserve("+(debug?"TRUE":"FALSE")+",args='"+rsrvargs+"')\" "+rargs);
			} else /* unix startup */
				p = Runtime.getRuntime().exec(new String[] {
						"/bin/sh", "-c",
						"echo 'library(Rserve);Rserve("+(debug?"TRUE":"FALSE")+",args=\""+rsrvargs+"\")'|"+cmd+" "+rargs
				});
			LOG.log(Level.INFO, "waiting for Rserve to start ... ("+p+")");
			// we need to fetch the output - some platforms will die if you don't ...
			StreamHog errorHog = new StreamHog(p.getErrorStream(), false);
			StreamHog outputHog = new StreamHog(p.getInputStream(), false);
			if (!isWindows) /* on Windows the process will never return, so we cannot wait */
				p.waitFor();
//			LOG.log(Level.INFO, "call terminated, let us try to connect ...");
//			processesList.add(p);
		} catch (Exception x) {
			LOG.log(Level.SEVERE, "failed to start Rserve process with "+x.getMessage());
			return false;
		}
		int attempts = 5; /* try up to 5 times before giving up. We can be conservative here, because at this point the process execution itself was successful and the start up is usually asynchronous */
		while (attempts > 0) {
			try {
				RConnection c = new RConnection();
				LOG.log(Level.INFO, "Rserve is running.");
				c.close();
				return true;
			} catch (Exception e2) {
				LOG.log(Level.SEVERE, "Try failed with: "+e2.getMessage());
			}
			/* a safety sleep just in case the start up is delayed or asynchronous */
			try { Thread.sleep(500); } catch (InterruptedException ix) { };
			attempts--;
		}
		return false;
	}

	/** checks whether Rserve is running and if that's not the case it attempts to start it using the defaults for the platform where it is run on. This method is meant to be set-and-forget and cover most default setups. For special setups you may get more control over R with <<code>launchRserve</code> instead. */
	public static boolean checkLocalRserve() {
		if (isRserveRunning()) return true;
		String osname = System.getProperty("os.name");
		if (osname != null && osname.length() >= 7 && osname.substring(0,7).equals("Windows")) {
			LOG.log(Level.INFO, "Windows: query registry to find where R is installed ...");
			String installPath = null;
			try {
				Process rp = Runtime.getRuntime().exec("reg query HKLM\\Software\\R-core\\R");
				StreamHog regHog = new StreamHog(rp.getInputStream(), true);
				rp.waitFor();
				regHog.join();
				installPath = regHog.getInstallPath();
			} catch (Exception rge) { 
				LOG.log(Level.SEVERE, "ERROR: unable to run REG to find the location of R: "+rge);
				return false;
			}
			if (installPath == null) {
				LOG.log(Level.SEVERE, "ERROR: canot find path to R. Make sure reg is available and R was installed with registry settings.");
				return false;
			}
			return launchRserve(installPath+"\\bin\\R.exe");
		}
		return (launchRserve("R") || /* try some common unix locations of R */
				((new File("/Library/Frameworks/R.framework/Resources/bin/R")).exists() && launchRserve("/Library/Frameworks/R.framework/Resources/bin/R")) ||
				((new File("/usr/local/lib/R/bin/R")).exists() && launchRserve("/usr/local/lib/R/bin/R")) ||
				((new File("/usr/lib/R/bin/R")).exists() && launchRserve("/usr/lib/R/bin/R")) ||
				((new File("/usr/local/bin/R")).exists() && launchRserve("/usr/local/bin/R")) ||
				((new File("/sw/bin/R")).exists() && launchRserve("/sw/bin/R")) ||
				((new File("/usr/common/bin/R")).exists() && launchRserve("/usr/common/bin/R")) ||
				((new File("/opt/bin/R")).exists() && launchRserve("/opt/bin/R"))
				);
	}

	/** check whether Rserve is currently running (on local machine and default port).
		 @return <code>true</code> if local Rserve instance is running, <code>false</code> otherwise
	 */
	public static boolean isRserveRunning() {
		try {
			RConnection c = new RConnection();
			LOG.log(Level.INFO, "Rserve is running.");
			c.close();
			return true;
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "First connect try failed with: " + e.getMessage());
		}
		return false;
	}

}
