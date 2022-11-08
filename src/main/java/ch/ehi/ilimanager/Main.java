package ch.ehi.ilimanager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.settings.Settings;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.gui.UserSettings;
import ch.interlis.ili2c.metamodel.TransferDescription;

/** Main program and commandline interface of ilimanager.
 */
public class Main {
	
	/** name of application as shown to user.
	 */
	public static final String APP_NAME="ilimanager";
	/** name of jar file.
	 */
	public static final String APP_JAR="ilimanager.jar";
	/** version of application.
	 */
	private static String version=null;
    private static int FC_NOOP=0;
    private static int FC_MODEL_REPOS_CLONE=1;
    private static int FC_CREATE_ILIDATA_XML=21;
    private static int FC_UPDATE_ILIDATA_XML=22;
    private static int FC_ILIMODELS_XML_CREATE=11;
    private static int FC_ILIMODELS_XML_UPDATE=12;
	
	/** main program entry.
	 * @param args command line arguments.
	 */
	static public void main(String args[]){
		Settings settings=new Settings();
		settings.setValue(Main.SETTING_ILIDIRS, Main.SETTING_DEFAULT_ILIDIRS);
		String appHome=getAppHome();
		if(appHome!=null){
		    settings.setValue(Main.SETTING_PLUGINFOLDER, new java.io.File(appHome,"plugins").getAbsolutePath());
		    settings.setValue(Main.SETTING_APPHOME, appHome);
		}else{
		    settings.setValue(Main.SETTING_PLUGINFOLDER, new java.io.File("plugins").getAbsolutePath());
		}
		Class mainFrame=null;
		try {
            //mainFrame=Class.forName("org.interlis2.validator.gui.MainFrame");
            mainFrame=Class.forName(preventOptimziation("ch.ehi.ilimanager.gui.Main")); // avoid, that graalvm native-image detects a reference to MainFrame
		}catch(ClassNotFoundException ex){
		    // ignore; report later
		}
		Method mainFrameMain=null;
		if(mainFrame!=null) {
		    try {
	            mainFrameMain = mainFrame.getMethod ("showDialog");
		    }catch(NoSuchMethodException ex) {
	            // ignore; report later
		    }
		}
		// arguments on export
		String xtfFile=null;
		String outFile=null;
        String repos=null;
		if(args.length==0){
			readSettings(settings);
            // MainFrame.main(xtfFile,settings);
			runGui(mainFrameMain, settings);     
			return;
		}
		int argi=0;
		int function=FC_NOOP;
		boolean doGui=false;
		for(;argi<args.length;argi++){
			String arg=args[argi];
			if(arg.equals("--trace")){
				EhiLogger.getInstance().setTraceFilter(false);
			}else if(arg.equals("--gui")){
				readSettings(settings);
				doGui=true;
			}else if(arg.equals("--modeldir")){
				argi++;
				settings.setValue(Main.SETTING_ILIDIRS, args[argi]);
            }else if (arg.equals("--cloneIliRepos")){
                function=FC_MODEL_REPOS_CLONE;
			}else if (arg.equals("--createIliData")){
			    function=FC_CREATE_ILIDATA_XML;
            }else if (arg.equals("--updateIliData")) {
                function=FC_UPDATE_ILIDATA_XML;
            }else if (arg.equals("--createIliModels")){
                function=FC_ILIMODELS_XML_CREATE;
            }else if (arg.equals("--updateIliModels")) {
                function=FC_ILIMODELS_XML_UPDATE;
            }else if (arg.equals("--out")) {
                argi++;
                outFile=args[argi];
            }else if (arg.equals("--data")) {
                argi++;
                xtfFile=args[argi];
			}else if (arg.equals("--srcfiles")) {
			    argi++;
			    settings.setValue(Main.SETTING_REMOTEFILE_LIST, args[argi]);
			}else if (arg.equals("--datasetId")) {
			    argi++;
			    settings.setValue(Main.SETTING_DATASETID_TO_UPDATE, args[argi]);
            }else if (arg.equals("--repos")) {
                argi++;
                repos=args[argi];
			}else if(arg.equals("--log")) {
			    argi++;
			    settings.setValue(Main.SETTING_LOGFILE, args[argi]);
			}else if(arg.equals("--plugins")) {
			    argi++;
			    settings.setValue(Main.SETTING_PLUGINFOLDER, args[argi]);
			}else if(arg.equals("--proxy")) {
				    argi++;
				    settings.setValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_HOST, args[argi]);
			}else if(arg.equals("--proxyPort")) {
				    argi++;
				    settings.setValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_PORT, args[argi]);
			}else if(arg.equals("--version")){
				printVersion();
				return;
			}else if(arg.equals("--help")){
					printVersion ();
					System.err.println();
					printDescription ();
					System.err.println();
					printUsage ();
					System.err.println();
					System.err.println("OPTIONS");
					System.err.println();
					System.err.println("--gui                 start GUI.");
					System.err.println("--cloneIliRepos       clone an existing repository (only models)");
					System.err.println("--createIliModels     create a new ilimodels.xml");
					System.err.println("--updateIliModels     update an existing ilimodels.xml");
					System.err.println("--createIliData       create a new ilidata.xml");
                    System.err.println("--updateIliData       update an existing ilidata.xml");
					System.err.println("--srcfiles file       file with list of relative file names");
                    System.err.println("--data file           data file");
					System.err.println("--datasetId ID        the ID of the dataset to be updated");
                    System.err.println("--repos URL           source repository or folder");
                    System.err.println("--out file            output file or folder");
				    System.err.println("--log file            text file, that receives validation results.");
					System.err.println("--modeldir "+SETTING_DEFAULT_ILIDIRS+" list of directories/repositories");
				    System.err.println("--plugins folder      directory with jar files that contain user defined functions.");
				    System.err.println("--proxy host          proxy server to access model repositories.");
				    System.err.println("--proxyPort port      proxy port to access model repositories.");
					System.err.println("--trace               enable trace messages.");
					System.err.println("--help                Display this help text.");
					System.err.println("--version             Display the version of "+APP_NAME+".");
					System.err.println();
					return;
				
			}else if(arg.startsWith("-")){
				EhiLogger.logAdaption(arg+": unknown option; ignored");
			}else{
				break;
			}
		}
		if(doGui){
			//MainFrame.main(xtfFile,settings);
            runGui(mainFrameMain, settings);                     
            return;
		}else{
            boolean ok=false;
            if(function==FC_NOOP) {
                ok=true;
            }else if(function==FC_MODEL_REPOS_CLONE) {
                CloneRepos cloner=new CloneRepos();
                ok=cloner.cloneRepos(new File(outFile),new String[] {repos}, settings);
            }else if(function==FC_ILIMODELS_XML_CREATE) {
                MakeIliModelsXml2 makeIliModelsXml=new MakeIliModelsXml2();
                ok=makeIliModelsXml.mymain(false,new File(outFile),repos,settings);
            }else if(function==FC_ILIMODELS_XML_UPDATE) {
                MakeIliModelsXml2 makeIliModelsXml=new MakeIliModelsXml2();
                ok=makeIliModelsXml.mymain(true,new File(outFile),repos,settings);
            }else if(function==FC_CREATE_ILIDATA_XML) {
                ok = CreateIliDataTool.start(new File(outFile),repos,settings);
            }else if (function==FC_UPDATE_ILIDATA_XML) {
                ok = UpdateIliDataTool.update(new File(outFile),repos,new File(xtfFile),settings);
            }else {
                throw new IllegalStateException("function=="+function);
            }
            System.exit(ok ? 0 : 1);
		}
		
	}
    private static String preventOptimziation(String val) {
        StringBuffer buf=new StringBuffer(val.length());
        buf.append(val);
        return buf.toString();
    }
    private static void runGui(Method mainFrameMain, Settings settings) {
        if(mainFrameMain!=null) {
            try {
                mainFrameMain.invoke(null);
                return;                 
            } catch (IllegalArgumentException ex) {
                EhiLogger.logError("failed to open GUI",ex);
            } catch (IllegalAccessException ex) {
                EhiLogger.logError("failed to open GUI",ex);
            } catch (InvocationTargetException ex) {
                EhiLogger.logError("failed to open GUI",ex);
            }
        }else {
            EhiLogger.logError(APP_NAME+": no GUI available");
        }
        System.exit(2);
    }
	/** Name of file with program settings. Only used by GUI, not used by commandline version.
	 */
	private final static String SETTINGS_FILE = System.getProperty("user.home") + "/.ilimanager";
	/** Reads program settings.
	 * @param settings Program configuration as read from file.
	 */
	public static void readSettings(Settings settings)
	{
		java.io.File file=new java.io.File(SETTINGS_FILE);
		try{
			if(file.exists()){
				settings.load(file);
			}
		}catch(java.io.IOException ex){
			EhiLogger.logError("failed to load settings from file "+SETTINGS_FILE,ex);
		}
	}
	/** Writes program settings.
	 * @param settings Program configuration to write.
	 */
	public static void writeSettings(Settings settings)
	{
		java.io.File file=new java.io.File(SETTINGS_FILE);
		try{
			settings.store(file,APP_NAME+" settings");
		}catch(java.io.IOException ex){
			EhiLogger.logError("failed to settings settings to file "+SETTINGS_FILE,ex);
		}
	}
	
	/** Prints program version.
	 */
	protected static void printVersion ()
	{
	  System.err.println(APP_NAME+", Version "+getVersion());
	  System.err.println("  Developed by Eisenhut Informatik AG, CH-3400 Burgdorf");
	}

	/** Prints program description.
	 */
	protected static void printDescription ()
	{
	  System.err.println("DESCRIPTION");
	  System.err.println("  creates/updates INTERLIS repository index files.");
	}

	/** Prints program usage.
	 */
	protected static void printUsage()
	{
	  System.err.println ("USAGE");
	  System.err.println("  java -jar "+APP_JAR+" [Options]");
	}
	/** Gets version of program.
	 * @return version e.g. "1.0.0"
	 */
	public static String getVersion() {
		  if(version==null){
		java.util.ResourceBundle resVersion = java.util.ResourceBundle.getBundle(ch.ehi.basics.i18n.ResourceBundle.class2qpackageName(Main.class)+".Version");
			StringBuffer ret=new StringBuffer(20);
		ret.append(resVersion.getString("version"));
			ret.append('-');
		ret.append(resVersion.getString("versionCommit"));
			version=ret.toString();
		  }
		  return version;
	}
	
	/** Gets main folder of program.
	 * 
	 * @return folder Main folder of program.
	 */
	static public String getAppHome()
	{
	  String[] classpaths = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
	  for(String classpath:classpaths) {
		  if(classpath.toLowerCase().endsWith(".jar")) {
			  File file = new File(classpath);
			  String jarName=file.getName();
			  if(jarName.toLowerCase().startsWith(APP_NAME)) {
				  file=new File(file.getAbsolutePath());
				  if(file.exists()) {
					  return file.getParent();
				  }
			  }
		  }
	  }
	  return null;
	}

	
    /** Name of file with the list of filenames.
     */
    public static final String SETTING_REMOTEFILE_LIST="org.interlis2.validator.filelist";
    /** Dataset ID of the data.
     */
    public static final String SETTING_DATASETID_TO_UPDATE = "org.interlis2.validator.datasetIDToUpdate";
    /** Path with folders of Interlis model files. Multiple entries are separated by semicolon (';'). 
     * Might contain "http:" URLs which should contain model repositories. 
     * Might include placeholders ITF_DIR or JAR_DIR. 
     * @see #ITF_DIR
     * @see #JAR_DIR
     */
    public static final String SETTING_ILIDIRS="org.interlis2.validator.ilidirs";
    /** Placeholder, that will be replaced by the folder of the current to be validated transfer file. 
     * @see #SETTING_ILIDIRS
     */
    public static final String ITF_DIR="%ITF_DIR";
    /** Placeholder, that will be replaced by the folder of the validator program. 
     * @see #SETTING_ILIDIRS
     */
    public static final String JAR_DIR="%JAR_DIR";
    /** Default path with folders of Interlis model files.
     * @see #SETTING_ILIDIRS
     */
    public static final String SETTING_DEFAULT_ILIDIRS = ITF_DIR+";http://models.interlis.ch/;"+JAR_DIR+"/ilimodels";
    /** the main folder of program.
     */
    public static final String SETTING_APPHOME="org.interlis2.validator.appHome";
    /** Name of the folder that contains jar files with plugins.
     */
    public static final String SETTING_PLUGINFOLDER = "org.interlis2.validator.pluginfolder";
    /** Name of the log file that receives the validation results.
     */
    public static final String SETTING_LOGFILE = "org.interlis2.validator.log";
    /** model names. Multiple model names are separated by semicolon (';'). 
     */
    public static final String SETTING_MODELNAMES="org.interlis2.validator.modelNames";

    public static TransferDescription compileIli(String iliVersion,List<String> modelNames,File ilifile,String itfDir,String appHome,Settings settings) {
        ch.interlis.ilirepository.IliManager modelManager=createRepositoryManager(itfDir,appHome,settings);
        return compileIli(modelManager,iliVersion, modelNames, ilifile, settings);
    }
    public static TransferDescription compileIli(ch.interlis.ilirepository.IliManager modelManager,String iliVersion,List<String> modelNames,File ilifile,Settings settings) {
        TransferDescription td=null;
        ch.interlis.ili2c.config.Configuration ili2cConfig=null;
        if(ilifile!=null){
            try {
                //ili2cConfig=ch.interlis.ili2c.ModelScan.getConfig(modeldirv, modelv);
                ArrayList<String> ilifiles=new ArrayList<String>();
                ilifiles.add(ilifile.getPath());
                ili2cConfig=modelManager.getConfigWithFiles(ilifiles);
                ili2cConfig.setGenerateWarnings(false);
            } catch (Ili2cException ex) {
                EhiLogger.logError(ex);
                return null;
            }
        }else{
            ArrayList<String> modelv=new ArrayList<String>();
            if(modelNames!=null){
                modelv.addAll(modelNames);
            }
            try {
                double version=0.0;
                if(iliVersion!=null) {
                    version=Double.parseDouble(iliVersion);
                }
                ili2cConfig=modelManager.getConfig(modelv, version);
                ili2cConfig.setGenerateWarnings(false);
            } catch (Ili2cException ex) {
                EhiLogger.logError(ex);
                return null;
            }
            
        }
        
    
        try {
            ch.interlis.ili2c.Ili2c.logIliFiles(ili2cConfig);
            td=ch.interlis.ili2c.Ili2c.runCompiler(ili2cConfig);
        } catch (Ili2cFailure ex) {
            EhiLogger.logError(ex);
            return null;
        }
        return td;
    }
    public static ch.interlis.ilirepository.IliManager createRepositoryManager(String itfDir,String appHome,Settings settings) {
        ArrayList modeldirv=new ArrayList();
        String ilidirs=settings.getValue(Main.SETTING_ILIDIRS);
        if(ilidirs==null){
            ilidirs=Main.SETTING_DEFAULT_ILIDIRS;
        }
    
        EhiLogger.logState("modeldir <"+ilidirs+">");
        String modeldirs[]=ilidirs.split(";");
        HashSet ilifiledirs=new HashSet();
        for(int modeli=0;modeli<modeldirs.length;modeli++){
            String m=modeldirs[modeli];
            if(m.contains(Main.ITF_DIR)){
                m=m.replace(Main.ITF_DIR, itfDir);
                if(m!=null && m.length()>0){
                    if(!modeldirv.contains(m)){
                        modeldirv.add(m);               
                    }
                }
            }else if(m.contains(Main.JAR_DIR)){
                if(appHome!=null){
                    m=m.replace(Main.JAR_DIR,appHome);
                    modeldirv.add(m);               
                }else {
                    // ignore it
                }
            }else{
                if(m!=null && m.length()>0){
                    modeldirv.add(m);               
                }
            }
        }       
        
        ch.interlis.ili2c.Main.setHttpProxySystemProperties(settings);
        ch.interlis.ilirepository.IliManager repositoryManager = (ch.interlis.ilirepository.IliManager) settings
                .getTransientObject(UserSettings.CUSTOM_ILI_MANAGER);
        if(repositoryManager==null) {
            repositoryManager=new ch.interlis.ilirepository.IliManager();
            settings.setTransientObject(UserSettings.CUSTOM_ILI_MANAGER,repositoryManager);
        }
        repositoryManager.setRepositories((String[])modeldirv.toArray(new String[]{}));
        return repositoryManager;
    }
	
}
