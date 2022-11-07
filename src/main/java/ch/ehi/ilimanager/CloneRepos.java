package ch.ehi.ilimanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.settings.Settings;
import ch.interlis.models.ILIREPOSITORY20;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.modelscan.IliFile;
import ch.interlis.ili2c.modelscan.IliModel;
import ch.interlis.ilirepository.IliFiles;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.ilirepository.impl.RepositoryAccess;
import ch.interlis.ilirepository.impl.RepositoryAccessException;
import ch.interlis.ilirepository.impl.ModelMetadata;
import ch.interlis.iom_j.xtf.XtfModel;
import ch.interlis.iom_j.xtf.XtfWriterBase;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.EndBasketEvent;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.ObjectEvent;
import ch.interlis.iox_j.StartBasketEvent;
import ch.interlis.iox_j.StartTransferEvent;

public class CloneRepos {

	public boolean cloneRepos(File destFolder,String srcRepos[],
	        Settings settings) {
		boolean failed=false;
		ch.interlis.ili2c.Main.setHttpProxySystemProperties(settings);
		if(destFolder==null) {
			EhiLogger.logError("no output folder given");
			return true;
		}
		ArrayList<ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata> mergedModelMetadatav = new ArrayList<ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata>();
		destFolder.mkdir();
		EhiLogger.logState("outputFolder <"+destFolder.getPath()+">");
		RepositoryAccess reposAccess=new RepositoryAccess();
		// TODO reposAccess.setCache(localdir);
		for(String repos:srcRepos) {
			if (true) {
				String localReposPath = escapeReposUri(repos);
				// get list of current files in repository
				File ilimodelsFile;
				try {
					ilimodelsFile = reposAccess.getLocalFileLocation(repos,IliManager.ILIMODELS_XML,0,null);
				} catch (RepositoryAccessException e2) {
					EhiLogger.logError(e2);
					failed=true;
					continue;
				}
				if(ilimodelsFile==null){
					EhiLogger.logAdaption("URL <"+repos+"> contains no "+IliManager.ILIMODELS_XML+"; ignored");
					continue;
				}
				// read file
				IliFiles files=null;
				List<ModelMetadata> modelMetadatav = null;
				try {
					modelMetadatav = RepositoryAccess.readIliModelsXml2(ilimodelsFile);
					modelMetadatav = RepositoryAccess.getLatestVersions2(modelMetadatav);
					files = RepositoryAccess.createIliFiles2(repos, modelMetadatav);
				} catch (RepositoryAccessException e2) {
					EhiLogger.logError(e2);
					failed=true;
					continue;
				}
				for(Iterator<IliFile> filei=files.iteratorFile();filei.hasNext();){
					IliFile file=filei.next();
					List<String> ilimodels=new ArrayList<String>();
					for(Iterator modeli=file.iteratorModel();modeli.hasNext();){
						IliModel model=(IliModel)modeli.next();
						ilimodels.add(model.getName());
					}
						try {
							File srcFile = reposAccess.getLocalFileLocation(repos,file.getPath(),0,file.getMd5());
							// clone file
							File destFile=new File(new File(destFolder,localReposPath),file.getPath());
							RepositoryAccess.copyFile(destFile, srcFile);
						} catch (RepositoryAccessException e1) {
							EhiLogger.logError(e1);
							failed=true;
						}

				}
				for(ModelMetadata md:modelMetadatav) {
					String file=md.getFile();
					md.setFile(new File(localReposPath,file).getPath());
					mergedModelMetadatav.add(RepositoryAccess.mapToIom20(md));
				}
			}
		}
		// write new ilimodels.xml
		java.io.OutputStream outStream=null;
		XtfWriterBase ioxWriter=null;
		try{
			outStream=new java.io.FileOutputStream(new File(destFolder,IliManager.ILIMODELS_XML));
			ioxWriter = new XtfWriterBase( outStream,  ILIREPOSITORY20.getIoxMapping(),"2.3");
			ioxWriter.setModels(new XtfModel[]{ILIREPOSITORY20.getXtfModel()});
			StartTransferEvent startTransferEvent = new StartTransferEvent();
			startTransferEvent.setSender( ch.interlis.ili2c.Main.APP_NAME+"-"+ch.interlis.ili2c.metamodel.TransferDescription.getVersion() );
			ioxWriter.write( startTransferEvent );
			StartBasketEvent startBasketEvent = new StartBasketEvent( ILIREPOSITORY20.RepositoryIndex, "b1");
			ioxWriter.write( startBasketEvent );
			for(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata model:mergedModelMetadatav){
				ioxWriter.write(new ObjectEvent(model));
			}
			
			ioxWriter.write( new EndBasketEvent() );
			ioxWriter.write( new EndTransferEvent() );
			
			ioxWriter.flush();
		}catch(java.io.FileNotFoundException ex){
			EhiLogger.logError(ex);
			failed=true;
		} catch (IoxException ex) {
			EhiLogger.logError(ex);
			failed=true;
		}finally{
			if(ioxWriter!=null){
				try {
					ioxWriter.close();
				} catch (IoxException ex) {
					EhiLogger.logError(ex);
				}
				ioxWriter=null;
			}
			if(outStream!=null){
				try{
					outStream.close();				
				}catch(java.io.IOException ex){
					EhiLogger.logError(ex);
				}
				outStream=null;
			}
		}
		return failed;
	}

	private String escapeReposUri(String uri) {
		String urilc=uri.toLowerCase();
		boolean isHttps= urilc.startsWith("https://");
		boolean isHttp=urilc.startsWith("http://");
		if(isHttp || isHttps){
			if(isHttps){
				uri=uri.substring("https://".length()); 
			}else{
				uri=uri.substring("http://".length()); 
			}
		}
		return RepositoryAccess.escapeUri(uri);
	}
}
