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

package net.sf.mzmine.util;

import net.sf.mzmine.util.RUtilities.StreamGobbler;

import org.math.R.RserverConf;
import org.math.R.Rsession;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.ByteArrayOutputStream;


/**
 * @description TODO
 * @author Gauthier Boaglio
 * @date Nov 19, 2014
 */
public class RSessionWrapper {

	// Logger.
	private static final Logger LOG = Logger.getLogger(RSessionWrapper.class.getName());

	private static boolean DEBUG = false;

	// Rsession semaphore - all usage of R engine must be synchronized using this semaphore.
	public static final Object R_SESSION_SEMAPHORE = new Object();
	public static Rsession MASTER_SESSION = null;
	public static final ArrayList<RSessionWrapper> R_SESSIONS_REG = new ArrayList<RSessionWrapper>();
	public final static String R_HOME_KEY = "R_HOME";

	private final Object R_DUMMY_SEMAPHORE = new Object();

	private static final Level rsLogLvl = (DEBUG) ? Level.FINEST : Level.OFF;
	private static final Level logLvl = Level.FINEST;
	private static PrintStream logStream = new LoggerStream(LOG, rsLogLvl);

	//	// Enhanced remote security stuffs.
	//	private static final String RS_LOGIN = "MZmineUser";
	//	private static final String RS_DYN_PWD = String.valueOf(java.util.UUID.randomUUID());

	public enum RengineType {

		JRIengine("JRIengine - mono-instance engine"), 
		Rserve("Rserve - multi-instance of Rserve (fast)");

		private String type;

		RengineType(String type) {
			this.type = type;
		}

		public String toString() {
			return type;
		}

	}


	public static class NullPrintStream extends PrintStream {

		public NullPrintStream() {
			super(new NullByteArrayOutputStream());
		}

		private static class NullByteArrayOutputStream extends ByteArrayOutputStream {

			@Override
			public void write(int b) {
				// do nothing
			}

			@Override
			public void write(byte[] b, int off, int len) {
				// do nothing
			}

			@Override
			public void writeTo(OutputStream out) throws IOException {
				// do nothing
			}

		}
	}


	private RengineType rEngineType;
	private Object rEngine = null;
	private String[] reqPackages;

	private Rsession session;
	private int rServePid = -1;

	private boolean userCanceled = false;

	/**
	 * Constructor.
	 */
	public RSessionWrapper(RengineType type, String[] reqPackages) {
		this.rEngineType = type;
		this.reqPackages = reqPackages;
	}


	private void getRengineInstance() throws RSessionWrapperException {

		LOG.log(logLvl, "'getRengineInstance()' of type: '" + this.rEngineType.toString() + "'.");

		try {
			if (this.rEngineType == RengineType.JRIengine) {
				// Get JRI engine unique instance.
				this.rEngine = RUtilities.getREngine();
				// Quick test
				LOG.log(logLvl, ((Rengine)this.rEngine).eval("R.version.string").asString());
			} else {

				String globalFailureMsg = 
						"Could not start Rserve ( R> install.packages(c('Rserve')) ). "
								+ "Please check if R and Rserve are installed and, in "
								+ "case the path to the R installation directory could not be "
								+ "detected automatically, if the '" + R_HOME_KEY + "' environment variable is "
								+ "correctly set in the startMZmine script.";

				if (this.rEngine == null) {

					boolean isWindows = RUtilities.isWindows();
					String rHome = System.getenv(R_HOME_KEY);

					try {

						// If retrieving 'R_HOME' from environment failed, try to find out automatically.
						// (Since 'Rsession.newInstanceTry()', checks the environment first).
						// @See RUtilities.getRhomePath().
						if (rHome == null || !(new File(rHome).exists())) {
							// Set "R_HOME" system property.
							rHome = RUtilities.getRhomePath();
							if (rHome != null) {
								System.setProperty(R_HOME_KEY, rHome);
								LOG.log(logLvl, "'" + R_HOME_KEY + "' set to '" + System.getProperty(R_HOME_KEY) + "'");
							}
						}
						if (rHome == null)
							throw new IllegalArgumentException(
									"Correct path to the R installation directory could not be obtained "
											+ "neither automatically, nor via the '" + R_HOME_KEY + "' environment variable. "
											+ "Please try to set it manually in the startMZmine script.");


						// Under *NUX, create the very first Rserve instance, designed only to spawn other 
						// (computing) instances (must be released when leaving the app.).
						synchronized (RSessionWrapper.R_SESSION_SEMAPHORE) {
							if (!isWindows && RSessionWrapper.MASTER_SESSION == null) {
								RSessionWrapper.MASTER_SESSION = Rsession.newInstanceTry(logStream, null);
								LOG.log(logLvl, ">> MASTER Rserve instance created (pid: '" + 
										RSessionWrapper.MASTER_SESSION.eval("Sys.getpid()").asInteger() + "').");
							}
						}

						// Need a new session to be completely instantiated before asking for another one
						// otherwise, under Windows, the "multi-instance emulation" system will try several
						// session startup on same port (aka: each new session port has to be in use/unavailable
						// before trying to get another one).					
						// Win: Synch with any previous session, if applicable. 
						// *NUX: Synch with nothing that matters.
						Object rSemaphore = (isWindows) ? RSessionWrapper.R_SESSION_SEMAPHORE : this.R_DUMMY_SEMAPHORE;
						synchronized (rSemaphore) { //RUtilities.R_SEMAPHORE) {

							//RserverConf conf = new RserverConf(null, -1, RSessionWrapper.RS_LOGIN, RSessionWrapper.RS_DYN_PWD, null);
							RserverConf conf = null; 

							// Then, spawn a new computing instance.
							if (isWindows /*|| RSessionWrapper.R_SESSIONS_REG.size() == 0*/) {
								this.session = Rsession.newInstanceTry(logStream, conf);
							} else {
								this.session = Rsession.newLocalInstance(logStream, null);
							}
							if (this.session == null)
								throw new IllegalArgumentException(globalFailureMsg);

							this.register();

						}

					} catch (IllegalArgumentException e) {
						// Redirect undeclared exceptions thrown by "Rsession" library to regular one.
						//**throw new RSessionWrapperException(e.getMessage());
						throw new RSessionWrapperException(globalFailureMsg);
					}

					// As "Rsession.newInstanceTry()" runs an Rdaemon Thread. It is scheduled already,
					// meaning the session will be opened even for "WAITING" tasks, in any case, and even
					// if it's been meanwhile canceled.
					// Consequently, we need to kill it after the instance has been created, since trying to abort
					// the instance (close the session) before it exists would result in no termination at all.
					if (this.session != null && this.userCanceled) {
						this.close(true);
						return;
					} else {

						// Keep an opened instance and store the related PID.
						this.rServePid = session.connection.eval("Sys.getpid()").asInteger();
						this.rEngine = session.connection;
						LOG.log(logLvl, "Rserve: started instance with pid '" + this.rServePid + "'.");

						// Quick test
						LOG.log(logLvl, ((RConnection) this.rEngine).eval("R.version.string").asString());		
					}
				}
			}
		}
		catch (Throwable t) {
			//t.printStackTrace();
			throw new RSessionWrapperException(
					"This feature requires R but it couldn't be loaded: \n" + TextUtils.wrapText(t.getMessage(), 80));
		}
	}

	public RengineType getRengineType() {
		return this.rEngineType;
	}


	public void loadPackage(String packageName) throws RSessionWrapperException {

		String loadCode = "library(" + packageName + ", logical.return = TRUE)";
		String errorMsg = "The \"" + packageName + "\" R package couldn't be loaded - is it installed in R?";

		if (this.rEngineType == RengineType.JRIengine) {
			synchronized (RSessionWrapper.R_SESSION_SEMAPHORE) {
				if (((Rengine) this.rEngine).eval(loadCode).asBool().isFALSE()) {
					throw new RSessionWrapperException(errorMsg);
				}
				LOG.log(logLvl, "Loaded package: '" + packageName + "'.");
			}
		} else {
			if (this.session != null && !this.userCanceled) {
				int loaded = 0;
				try {
					loaded = ((RConnection) this.rEngine).eval(loadCode).asInteger();
				} catch (RserveException | REXPMismatchException e) {
					// Remain silent if eval KO ("server down").
					loaded = Integer.MIN_VALUE;
				}
				// Throw loading failure only if eval OK (package not found).
				// ("server down" case will be handled soon enough).
				if (loaded == 0)
					if (!this.userCanceled) throw new RSessionWrapperException(errorMsg);

				LOG.log(logLvl, "Loaded package: '" + packageName + "'.");
			}
		}
	}

	public String loadRequiredPackages() {

		String reqPackage = null;
		try {
			for (int i=0; i < this.reqPackages.length; ++i) {
				reqPackage = this.reqPackages[i];
				this.loadPackage(this.reqPackages[i]);
			}
			return null;
		} catch (Exception e) {
			LOG.severe("Failed loading package: '" + reqPackage + "'.");
			return reqPackage;
		}			
	}


	public static class InputREXPFactory {

		public static <T> REXP getREXP(T object) {

			REXP x = null;

			//			// First check if we have primitive (single or array) or Object
			//			boolean isPrimitiveOrWrapped = ClassUtils.isPrimitiveOrWrapper(object.getClass());

			if (object instanceof Integer) {
				x = new REXPInteger((Integer)object);
			} 
			else if (object instanceof int[]) {
				x = new REXPInteger((int[])object);
			} 
			else if (object instanceof Double) {
				x = new REXPDouble((Double)object);
			}
			else if (object instanceof double[]) {
				x = new REXPDouble((double[])object);
			} 
			else if (object instanceof String) {
				x = new REXPString((String)object);
			}

			return x;
		}
	}
	public static class OutputObjectFactory {

		public static <T> Object getObject(REXP rexp) throws REXPMismatchException {

			Object o = null;

			if (rexp instanceof REXPInteger) {
				int[] obj = rexp.asIntegers();
				if (obj == null) return null;

				if (obj.length == 0) o = null;
				else if (obj.length == 1) o = obj[0];
				else o = obj;
			} 
			else if (rexp instanceof REXPDouble) {
				double[] obj = rexp.asDoubles();
				if (obj == null) return null;

				if (obj.length == 0) o = null;
				else if (obj.length == 1) o = obj[0];
				else o = obj;
			} 
			else if (rexp instanceof REXPString) {
				o = rexp.asString();
			}

			return o;
		}
	}

	// TODO: Templatize: assignDoubleArray<T>(String objName, T obj)
	public void assignDoubleArray(String objName, double[] dArray) throws RSessionWrapperException {

		if (this.rEngineType == RengineType.JRIengine) {
			synchronized (RSessionWrapper.R_SESSION_SEMAPHORE) {
				((Rengine) this.rEngine).assign(objName, dArray);
			}
		} else {
			if (this.session != null && !this.userCanceled) {
				String msg = "Rserve error: couldn't assign R object '" + objName + "' (instance '" + this.getPID() + "').";
				try {
					((RConnection) this.rEngine).assign(objName, dArray);
				} 
				catch (REngineException e) {
					throw new RSessionWrapperException(msg);
				} catch (Exception e) {
					throw new RSessionWrapperException(e.getMessage());
				}
			}
		}
	}
	public <T> void assign(String objName, T object) throws RSessionWrapperException {

		if (this.session != null && !this.userCanceled) {
			String msg = "Rserve error: couldn't assign R object '" + objName + "' (instance '" + this.getPID() + "').";
			try {
				((RConnection) this.rEngine).assign(objName, InputREXPFactory.getREXP(object));
			} 
			catch (REngineException e) {
				throw new RSessionWrapperException(msg);
			} catch (Exception e) {
				throw new RSessionWrapperException(e.getMessage());
			}
		}
	}

	public void eval(String rCode) throws RSessionWrapperException {

		if (this.rEngineType == RengineType.JRIengine) {
			synchronized (RSessionWrapper.R_SESSION_SEMAPHORE) {
				((Rengine) this.rEngine).eval(rCode);
			}
		} else {
			if (this.session != null && !this.userCanceled) {
				String msg = "Rserve error: couldn't eval R code '" + rCode + "' (instance '" + this.getPID() + "').";
				try {
					((RConnection) this.rEngine).eval(rCode);
				}
				catch (RserveException e) {
					throw new RSessionWrapperException(msg);
				} catch (Exception e) {
					throw new RSessionWrapperException(e.getMessage());
				}
			}
		}
	}

	// TODO: Templatize: T collectDoubleArray(String objName)
	public double[] collectDoubleArray(String objName) throws RSessionWrapperException {
		if (this.rEngineType == RengineType.JRIengine) {
			synchronized (RSessionWrapper.R_SESSION_SEMAPHORE) {
				return ((Rengine) this.rEngine).eval(objName).asDoubleArray();
			}
		} else {
			if (this.session != null && !this.userCanceled) {
				String msg = "Rserve error: couldn't collect R object '" + objName + "' (instance '" + this.getPID() + "').";
				try {
					return ((RConnection) this.rEngine).eval(objName).asDoubles();
				} 
				catch (RserveException | REXPMismatchException e) {
					throw new RSessionWrapperException(msg);
				} catch (Exception e) {
					throw new RSessionWrapperException(e.getMessage());
				}
			}
		}
		return null;
	}
	/**
	 * Casting the result to the correct type is left to the user.
	 * @param objName
	 * @return
	 * @throws RSessionWrapperException
	 */
	public Object collect(String objName) throws RSessionWrapperException {

		Object object = null;

		if (this.session != null && !this.userCanceled) {
			String msg = "Rserve error: couldn't collect R object '" + objName + "' (instance '" + this.getPID() + "').";
			try {
				object = OutputObjectFactory.getObject(((RConnection) this.rEngine).eval(objName));
			} 
			catch (RserveException | REXPMismatchException e) {
				throw new RSessionWrapperException(msg);
			} catch (Exception e) {
				throw new RSessionWrapperException(e.getMessage());
			}
		}
		return object;
	}



	public void open() throws RSessionWrapperException {
		// Redirect 'Rsession' gossiping on standard outputs to logger.
		System.setOut(logStream);
		System.setErr(logStream);
		// Load R engine (Do nothing if session was canceled).
		if (!this.userCanceled) { getRengineInstance(); }
	}

	/**
	 * This can be necessary to call 'close()' from a different thread
	 * than the one which called 'open()', sometimes, with Rserve (if
	 * the related instance is busy).
	 * @param userCanceled Tell the application the closure came from a user 
	 * 			action rather than from an unknown source error. 
	 * @throws RSessionWrapperException 
	 */
	public void close(boolean userCanceled) throws RSessionWrapperException {

		this.userCanceled = userCanceled;

		if (this.session != null && this.rEngineType == RengineType.Rserve) {

			try {

				LOG.log(logLvl, "Rserve: try terminate " + ((this.rServePid == -1) ? "pending" : "") + " session" + 
						((this.rServePid == -1) ? "..." : " with pid '" + this.rServePid + "'..."));

				// Avoid 'Rsession' to 'printStackTrace' while catching 'SocketException'
				// (since we are about to brute force kill the Rserve instance, such that
				// the session won't end properly).
				RSessionWrapper.muteStdOutErr();
				{
					RSessionWrapper.killRserveInstance(this);			
					this.session.end();
				}
				RSessionWrapper.unMuteStdOutErr();

				LOG.log(logLvl, "Rserve: terminated " + ((this.rServePid == -1) ? "pending" : "") + " session" + 
						((this.rServePid == -1) ? "." : " with pid '" + this.rServePid + "'."));

				// Release session (prevents from calling close again on a closed instance).
				this.session = null;

			} catch (Throwable t) {
				// Adapt/refactor message accordingly to the way the termination was provoked:
				// User requested or unexpectedly...
				String msg;
				if (userCanceled) {
					msg = "Rserve error: couldn't terminate instance with pid '" + this.rServePid + "'. Details:\n";
				} else {
					msg = "Rserve error: something when wrong with instance of pid '" + this.rServePid + "'. Details:\n";
				}
				throw new RSessionWrapperException(msg + TextUtils.wrapText(t.getMessage(), 80));

			} finally {				
				// Make sure to restore standard outputs.
				System.setOut(System.out);
				System.setErr(System.err);
			}
		}

		this.unRegister();
	}

	private void register() {
		RSessionWrapper.R_SESSIONS_REG.add(this);
	}
	private void unRegister() {
		RSessionWrapper.R_SESSIONS_REG.remove(this);
	}

	/**
	 * Keep logging clean.
	 * Turn off standard outputs, since 'Rsession' library is way far too talkative
	 * on them.
	 */
	private static void muteStdOutErr() {
		System.setOut(new NullPrintStream());
		System.setErr(new NullPrintStream());		
		logStream = new NullPrintStream();
	}
	/**
	 * Restore standard outputs.
	 */
	private static void unMuteStdOutErr() {
		System.setOut(System.out);
		System.setErr(System.err);		
		logStream = new LoggerStream(LOG, rsLogLvl);
	}

	public static void killRserveInstance(RSessionWrapper rSession) throws RSessionWrapperException {

		if (rSession != null && rSession.getPID() != -1)
		{
			// Win: faster to brute force kill the process (avoids "Rsession.newInstanceTry()"
			// to attempt to recover the connection).
			if (RUtilities.isWindows()) {
				try {
					// Working but damn slow.
					//		// BEGIN OK
					//		Rsession s = Rsession.newInstanceTry(logStream, null);
					//		s.eval("tools::pskill("+ rSession.getPID() + ")");
					//		LOG.info("Eval: " + "tools::pskill("+ rSession.getPID() + ")");
					//		s.end();
					//		// END OK

					// ... Using 'TASKKILL' instead ...
					//FileOutputStream fos_out = new FileOutputStream("output.txt");
					//FileOutputStream fos_err = new FileOutputStream("error.txt");
					OutputStream os_err = new OutputStream() {
						private StringBuilder string = new StringBuilder();

						@Override
						public void write(int b) throws IOException {
							this.string.append((char) b );
						}

						@Override
						public String toString(){
							return this.string.toString();
						}
					};

					Process proc = new ProcessBuilder("TASKKILL", "/PID", "" + rSession.getPID(), "/F").start();
					StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "Error", os_err); //, fos_err);            
					StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "Output", System.out); //, fos_out);

					// Consume outputs.
					errorGobbler.start();
					outputGobbler.start();

					// Any error while processing 'TASKKILL'? (expected '0').
					int exitVal = proc.waitFor();
					//System.out.println("ExitValue: " + exitVal);
					if (exitVal != 0)
						throw new RSessionWrapperException("Killing Rserve instance of PID '" + rSession.getPID() + "'" + 
								" failed. \n" + os_err.toString());
					//fos_out.flush(); fos_out.close();        
					//fos_err.flush(); fos_err.close();   
				} catch (Exception e) {			// IOException | InterruptedException
					// Silent.
				}
			} 
			// *NUX: For portability reasons, we prefer asking R to terminate the targeted instance
			// rather than using a call such as 'kill -9 pid' (even if this would work in most cases).
			else {
				try {
					final RConnection c2 = new RConnection(); //session.connection;
					// SIGTERM might not be understood everywhere: so using explicitly SIGKILL signal, as well.
					if (c2 != null && c2.isConnected()) {
						c2.eval("tools::pskill("+ rSession.getPID() + ")"); 				// win
						c2.eval("tools::pskill("+ rSession.getPID() + ", tools::SIGKILL)");	// *nux
						c2.close();
					}
				} catch (RserveException e) {
					throw new RSessionWrapperException(e.getMessage());
				}
			}
		}
	}


	public int getPID() {
		return this.rServePid;
	}
	public Rsession getSession() {
		return this.session;
	}
	public boolean isSessionRunning() {
		return (this.session != null && !this.userCanceled);
	}

	
	public static void CleanAll() {
		// Cleanup Rserve instances.
		if (RUtilities.isWindows()) {	// Should die with the app. anyway.
			for (int i=0; i < RSessionWrapper.R_SESSIONS_REG.size(); ++i) {
				try {
					if (RSessionWrapper.R_SESSIONS_REG.get(i) != null)
						RSessionWrapper.R_SESSIONS_REG.get(i).close(true);
				} catch (RSessionWrapperException e) {
					// Silent.
				}
			}
		} else {
			if (RSessionWrapper.MASTER_SESSION != null)
				RSessionWrapper.MASTER_SESSION.end();
		}
	}
	
}
