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

import org.math.R.Rsession;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import rcaller.Globals;
import rcaller.RCaller;
import rcaller.RCode;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @description TODO
 * @author Gauthier Boaglio
 * @date Nov 19, 2014
 */
public class RSessionWrapper {

	// Logger.
	private static final Logger LOG = Logger.getLogger(RSessionWrapper.class.getName());

	public enum RengineType {

		JRIengine("JRIengine - mono-instance engine"), 
		RCaller("RCaller - multi-instance of R (slow)"),
		Rserve("Rserve - multi-instance of Rserve (fast)");

		private String type;

		RengineType(String type) {
			this.type = type;
		}

		public String toString() {
			return type;
		}

	}

	private RengineType rEngineType;
	private Object rEngine = null;
	private String[] reqPackages;

	private RCode rcallerCode;
	
	private Rsession session;
	private int rServePid = -1;

	public RSessionWrapper(RengineType type, String[] reqPackages) {
		this.rEngineType = type;
		this.reqPackages = reqPackages;
	}


	// Since the batch launcher already added the correct paths
	private void setExecutablePaths() {
		Globals.R_Windows = RUtilities.getRexecutablePath(); //"R.exe";
		Globals.R_Linux = RUtilities.getRexecutablePath();//"/usr/bin/R";		
		Globals.RScript_Windows = Globals.R_Windows.substring(0, Globals.R_Windows.length()-5) + "Rscript.exe";//"Rscript.exe";
		Globals.RScript_Linux = Globals.R_Linux.substring(0, Globals.R_Linux.length()-1) + "Rscript";//"/usr/bin/Rscript";
		Globals.detect_current_rscript();					
	}

	
	
	private void getRengineInstance() {
		
		LOG.info("getRengineInstance() for: " + this.rEngineType.toString());
		
		try {
			if (this.rEngineType == RengineType.JRIengine) {
				// Get JRI engine unique instance.
				this.rEngine = RUtilities.getREngine();
				// Quick test
				LOG.info(((Rengine)this.rEngine).eval("R.version.string").asString());
			} else if (this.rEngineType == RengineType.RCaller) {
				if (this.rEngine == null) {
					// Create RCaller new instance for this task.
					
					LOG.info("PATHS: R=" + Globals.R_Linux + "  |  " + Globals.RScript_Linux);
					LOG.info("PATHS: R=" + Globals.R_current + "  |  " + Globals.Rscript_current);
					
					RCaller rcaller = new RCaller();
					this.setExecutablePaths();			
					rcaller.setRExecutable(Globals.R_current);
					rcaller.setRscriptExecutable(Globals.Rscript_current);
					this.rcallerCode = new RCode();
					rcaller.setRCode(this.rcallerCode);
					this.rEngine = rcaller;
					
					LOG.info("RCaller: started instance from paths <R:" + 
								Globals.R_current + " | Rscipt:" + Globals.Rscript_current + ">.");
					// Quick test
					this.rcallerCode.addRCode("r_version <- R.version.string");
					((RCaller)this.rEngine).runAndReturnResultOnline("s");
					LOG.info(((RCaller) this.rEngine).getParser().getAsStringArray("r_version")[0]);
					this.rcallerCode.clearOnline();			
				}
			} else {
				
				if (this.rEngine == null) {
					
					// Need a new session to be completely instantiated before asking for another one
					// otherwise, under Windows, the "multi-instance emulation" system will try several
					// session startup on same port (aka: each new session port has to be in unavailable
					// before trying to get another one).
					synchronized (RUtilities.R_SEMAPHORE) {
						//*if (!RUtilities.checkLocalRserve())
						if ((session = Rsession.newInstanceTry(new LoggerStream(LOG, Level.INFO), null)) == null)
							throw new IllegalStateException(
									"Could not start Rserve. Please check if R and Rserve are installed and path to the "
											+ "libraries is set properly in the startMZmine script.");
					}
//			        org.math.R.Logger l = new org.math.R.Logger() {
//
//			            public void println(String string, Level level) {
//			                LOG.log(level, string);
//			            }
//
//			            public void close() {
//			            }
//
//						@Override
//						public void println(String string) {
//							// TODO Auto-generated method stub
//							println(string, Level.INFO);
//						}
//			        };
					
					// Keep an opened instance and store the related pid
					//*RConnection rconn = new RConnection();
//					session = Rsession.newLocalInstance(new LoggerStream(LOG, Level.INFO), null);
//					session = Rsession.newInstanceTry(new LoggerStream(LOG, Level.INFO), null);
					//*RConnection rconn = session.connection;
					this.rServePid = session.connection.eval("Sys.getpid()").asInteger();
					this.rEngine = session.connection;
					LOG.info("Rserve: started instance with pid '" + this.rServePid + "'.");
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw new IllegalStateException(
					"This feature requires R but it couldn't be loaded (" + t.getMessage() + ')');
		}
	}

	public RengineType getRengineType() {
		return this.rEngineType;
	}

	// TODO: fix testing packages with RCaller
	public void loadPackage(String packageName) {

		String loadCode = "library(" + packageName + ", logical.return = TRUE)";
		String errorMsg = "The \"" + packageName + "\" R package couldn't be loaded - is it installed in R?";

		if (this.rEngineType == RengineType.JRIengine) {
			synchronized (RUtilities.R_SEMAPHORE) {
				if (((Rengine) this.rEngine).eval(loadCode).asBool().isFALSE()) {
					throw new IllegalStateException(errorMsg);
				}
			}
		} else if (this.rEngineType == RengineType.RCaller) {
//			RCaller rcaller = ((RCaller) this.rEngine);
//			String logicalRet = "pkgOK";
//			this.rcallerCode.addRCode(logicalRet + " <- " + loadCode);
//			rcaller.runAndReturnResultOnline(logicalRet);
//			//this.rcallerCode.clear();
//			if (rcaller.getParser().getAsLogicalArray(logicalRet)[0]) {
//				throw new IllegalStateException(errorMsg);
//			}
			this.rcallerCode.R_require(packageName);
		} else {
			int loaded = 0;
			try {
				loaded = ((RConnection)this.rEngine).eval(loadCode).asInteger(); //("library(" + packageName + ")");
			} catch (RserveException | REXPMismatchException e) {
				// Remain silent if eval KO ("server down").
				loaded = Integer.MIN_VALUE;
			}
			// Throw loading failure only if eval OK ("server down" case will be handled soon enough).
			if (loaded == 0)
				throw new IllegalStateException(errorMsg);
		}
	}
	
	public String loadRequiredPackages() {
		
		String reqPackage = null;
		try {
			for (int i=0; i < this.reqPackages.length; ++i) {
				reqPackage = this.reqPackages[i];
				this.loadPackage(this.reqPackages[i]);
				LOG.info("Loaded package: " + reqPackage + "'.");
			}
			return null;
		} catch (Exception e) {
			LOG.info("Failed loading package: '" + reqPackage + "'.");
			return reqPackage;
		}			
	}

	
	// TODO: templatize: assignDoubleArray<T>(String objName, T obj)
	public void assignDoubleArray(String objName, double[] dArray) {
		//LOG.info("Assign '" + dArray + "' array to object '" + objName + "'.");
		if (this.rEngineType == RengineType.JRIengine) {
			//synchronized (RUtilities.R_SEMAPHORE) {
				((Rengine) this.rEngine).assign(objName, dArray);
			//}
		} else if (this.rEngineType == RengineType.RCaller) {
			//synchronized (RUtilities.R_SEMAPHORE) {
				this.rcallerCode.addDoubleArray(objName, dArray);
			//}
		} else {
			try {
				((RConnection)this.rEngine).assign(objName, dArray);
			} catch (REngineException e) {
				//e.printStackTrace();
				throw new IllegalStateException("Rserve error: couldn't assign R object '" + objName + "'.");
			}
		}
		//LOG.info("Assigned '" + dArray + "' array to object '" + objName + "'.");
	}

	public void eval(String rCode) {
		//LOG.info("Eval: " + rCode);
		if (this.rEngineType == RengineType.JRIengine) {
			//synchronized (RUtilities.R_SEMAPHORE) {
				((Rengine) this.rEngine).eval(rCode);
			//}
		} else if (this.rEngineType == RengineType.RCaller) {
			this.rcallerCode.addRCode(rCode);
		} else {
			try {
				((RConnection)this.rEngine).eval(rCode);
			} catch (RserveException e) {
				//e.printStackTrace();
				throw new IllegalStateException("Rserve error: couldn't eval R code '" + rCode + "'.");
			}
		}
	}

	// TODO: templatize: T collectDoubleArray(String objName)
	public double[] collectDoubleArray(String objName) {
		if (this.rEngineType == RengineType.JRIengine) {
			//synchronized (RUtilities.R_SEMAPHORE) {
				return ((Rengine) this.rEngine).eval(objName).asDoubleArray();
			//}
		} else if (this.rEngineType == RengineType.RCaller) {
			RCaller rcaller = ((RCaller) this.rEngine);
			rcaller.runAndReturnResultOnline(objName);
			double[] dArray =  rcaller.getParser().getAsDoubleArray(objName);			
			this.rcallerCode.clearOnline();
			return dArray;
		} else {
			try {
				return ((RConnection)this.rEngine).eval(objName).asDoubles();
			} catch (RserveException | REXPMismatchException e) {
				//e.printStackTrace();
				throw new IllegalStateException("Rserve error: couldn't collect R object '" + objName + "'.");
			}
		}
	}
	


	public void open() {
		// Load engine
		getRengineInstance();
//		// Check & load packages
//		this.loadRequiredPackages();
	}
	
	/**
	 * This can be necessary to call 'close()' from a different thread
	 * than the one which called 'open()', sometimes, with Rserve.
	 * @param userCanceled
	 */
	public void close(boolean userCanceled) {

		if (this.rEngineType == RengineType.RCaller) {
			RCaller rcaller = ((RCaller) this.rEngine);
			rcaller.stopStreamConsumers();
			rcaller.StopRCallerOnline();
		} else if (this.rEngineType == RengineType.Rserve) {
			try {
//				// Try terminate gently.
//				if (((REngine) this.rEngine).close()) {
//					LOG.info("Rserve: closed (gently) instance with pid '" + this.rServePid + "'.");
//				}
//				// Otherwise: brute force kill...
//				else {
//					RConnection c2 = new RConnection();
//					c2.eval("tools::pskill("+ this.rServePid +")");
//					c2.close();
//					LOG.info("Rserve: killed (brute force) instance with pid '" + this.rServePid + "'.");
//				}
				
				
				
//				//**((REngine) this.rEngine).close();
//				// Terminate.
//				final RConnection c2 = new RConnection();
//				// SIGTERM might not be understood everywhere: so using SIGKILL signal, as well.
//				if (c2.isConnected() && this.rServePid != -1) {
//					c2.eval("tools::pskill("+ this.rServePid + ")"); //win
//					//c2.eval("tools::pskill("+ this.rServePid + ", tools::SIGKILL)"); //nix
//				}
////				// Need to wait for the process to be dead for real.
////				class R implements Runnable {
////					
////					private int pid;
////					
////					public R(int pid) {
////						super();
////						this.pid = pid;
////					}
////
////					/* (non-Javadoc)
////					 * @see java.lang.Runnable#run()
////					 */
////					@Override
////					public void run() {
////						int dead = 0;
////						int timeout = 0;
////						while (dead == 0 && timeout < 5) {
////							try {
////								dead = c2.eval("tools::pskill("+ this.pid + ")").asInteger();
////								LOG.info("Rserve: kill status = '" + dead + "'.");
////								if (dead == 0) {
////									try {
////										Thread.sleep(1000);
////										++timeout;
////									} catch (InterruptedException e) {
////										// TODO Auto-generated catch block
////										e.printStackTrace();
////										timeout = 5;
////									}
////								}
////							} catch (RserveException | REXPMismatchException e) {
////								// TODO Auto-generated catch block
////								e.printStackTrace();
////							}
////						}
////					}
////				}
////				Thread T = new Thread( new R(this.rServePid) );
////				T.start();
////				try {
////					T.join();
////				} catch (InterruptedException e) {
////					// TODO Auto-generated catch block
////					e.printStackTrace();
////				}
////				// Final closure.
//				c2.close();
				
				RUtilities.killRserveInstance(this);
				//this.session.end();
				
				LOG.info("Rserve: terminated instance with pid '" + this.rServePid + "'.");
			} catch (RserveException e) {
				// Adapt message accordingly to if the termination was provoked by user or
				// unexpected...
				if (userCanceled) {
					//e.printStackTrace();
					throw new IllegalStateException("Rserve error: couldn't terminate instance with pid '" + this.rServePid + "'.");
				} else {
					throw new IllegalStateException("Rserve error: something when wrong with instance of pid '" + this.rServePid + "'.");
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

}
