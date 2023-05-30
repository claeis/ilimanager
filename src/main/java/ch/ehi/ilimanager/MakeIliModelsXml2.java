package ch.ehi.ilimanager;

import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.settings.Settings;
import ch.ehi.basics.tools.StringUtility;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.config.GenerateOutputKind;
import ch.interlis.ili2c.metamodel.Ili2cMetaAttrs;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.modelscan.IliFile;
import ch.interlis.ili2c.modelscan.IliModel;
import ch.interlis.ilirepository.IliFiles;
import ch.interlis.ilirepository.impl.ModelMetadata;
import ch.interlis.ilirepository.impl.RepositoryAccess;
import ch.interlis.iom_j.xtf.XtfModel;
import ch.interlis.iom_j.xtf.XtfWriterBase;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.EndBasketEvent;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.ObjectEvent;
import ch.interlis.iox_j.StartBasketEvent;
import ch.interlis.iox_j.StartTransferEvent;
import ch.interlis.models.ILIREPOSITORY20;

/** Tool to create an ilimodels.xml file in a given local folder tree.
 */
public class MakeIliModelsXml2 {

	private long newtid=0;
	
	public boolean mymain(boolean doUpdate,File out,String repositoryRoot,Settings settings) {
		
		try {
			
			if(out==null){
				out=new File(repositoryRoot,"ilimodels.xml");
			}
			
			// if file exists
			HashMap<String,ArrayList<ModelMetadata>> oldfiles=null;
			if(doUpdate && out.exists()){
				// read existing file
				oldfiles=readIliModelsXml(out);
			}
			if(oldfiles==null){
				oldfiles=new HashMap<String,ArrayList<ModelMetadata>>();
			}
			
			// scan folder
			ArrayList<ModelMetadata> models=new ArrayList<ModelMetadata>(scanIliFileDir(new File(repositoryRoot)));
			HashMap<String,ArrayList<ModelMetadata>> newfiles=createFilelist(models);
			
			// first pass
			HashSet<ModelMetadata> newModels=new HashSet<ModelMetadata>();
			if(true){
					ArrayList<ModelMetadata> updatedModels=new ArrayList<ModelMetadata>();
					Iterator modeli=models.iterator();
					while(modeli.hasNext()){
						ModelMetadata model=(ModelMetadata)modeli.next();
						ModelMetadata theOldModel=null;
						// if model exists in oldfilelist
						if(oldfiles.containsKey(model.getFile())){
							// use old entry
							// update it
							ArrayList<ModelMetadata> oldmodels=oldfiles.get(model.getFile());
							for(ModelMetadata oldmodel : oldmodels){
								if(oldmodel.getName().equals(model.getName())){
									theOldModel=oldmodel;
									break;
								}
							}
							if(theOldModel!=null){
								theOldModel.setMd5(model.getMd5());
								theOldModel.setDependsOnModel(null);
								String[] deps=model.getDependsOnModel();
								for(String dep:deps){
									theOldModel.addDependsOnModel(dep);
								}
								model=theOldModel;
							}else{
								// new entry (but in a old file)
								newModels.add(model);
								// give it a new tid
								model.setOid(Long.toString(newtid));
								newtid++;
							}
						}else{
							// new entry
							newModels.add(model);
							// give it a new tid
							model.setOid(Long.toString(newtid));
							newtid++;
						}
						updatedModels.add(model);
					}
					models=updatedModels;
			}
			
			// second pass
			{
				// create temp IliFiles
				HashMap<String, ArrayList<ModelMetadata>> files=createFilelist(newModels);
				for(String theNewFile : files.keySet()){
					IliFiles tempIliFiles=RepositoryAccess.createIliFiles2(repositoryRoot,newModels);
					// compile
					Ili2cSettings ili2cSettings = new Ili2cSettings();
					ili2cSettings.setHttpProxyHost(settings.getValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_HOST));
					ili2cSettings.setHttpProxyPort(settings.getValue(ch.interlis.ili2c.gui.UserSettings.HTTP_PROXY_PORT));
					String settingsIliDirs=settings.getValue(Main.SETTING_ILIDIRS);
					if(settingsIliDirs!=null) {
	                    ili2cSettings.setIlidirs(repositoryRoot+";"+settingsIliDirs);
					}else {
	                    ili2cSettings.setIlidirs(repositoryRoot);
					}
					ili2cSettings.setTransientObject(Ili2cSettings.TEMP_REPOS_ILIFILES, tempIliFiles);
					ili2cSettings.setTransientObject(Ili2cSettings.TEMP_REPOS_URI, repositoryRoot);
					Configuration config = new Configuration();
					FileEntry file = new FileEntry(new File(repositoryRoot,theNewFile).getAbsolutePath(),
							FileEntryKind.ILIMODELFILE);
					config.addFileEntry(file);
					config.setAutoCompleteModelList(true);
					config.setGenerateWarnings(false);
					config.setOutputKind(GenerateOutputKind.NOOUTPUT);

					// compile models
					TransferDescription td = ch.interlis.ili2c.Main.runCompiler(config, ili2cSettings);
					if (td == null) {
						// compiler failed; skip file
					}else{
						// update all model infos of this file
						ArrayList<ModelMetadata> modelv=files.get(theNewFile);
						for(ModelMetadata model: modelv){
							// find model in td
							ch.interlis.ili2c.metamodel.Model modelDef=(ch.interlis.ili2c.metamodel.Model)td.getElement(ch.interlis.ili2c.metamodel.Model.class, model.getName());
							if(modelDef!=null){
								String title=modelDef.getDocumentation();
								if(title!=null){
									int titleEnd=title.indexOf('.');
									if(titleEnd>0){
										String doc=StringUtility.purge(title.substring(titleEnd+1));
										title=StringUtility.purge(title.substring(0,titleEnd+1));
										if(doc!=null){
											model.setShortDescription(doc);
										}
										if(title!=null){
											model.setTitle(title);
										}
									}else{
										model.setTitle(title);
									}
								}
								String technicalContact=modelDef.getMetaValue(Ili2cMetaAttrs.ILIMODELSXML_TECHNICAL_CONTACT);
								if(technicalContact!=null){
									model.setTechnicalContact(technicalContact);
								}
								String precursorVersion=modelDef.getMetaValue(Ili2cMetaAttrs.ILIMODELSXML_PRECURSOR_VERSION);
								if(precursorVersion!=null){
									model.setPrecursorVersion(precursorVersion);
								}
								String furtherInfo=modelDef.getMetaValue(Ili2cMetaAttrs.ILIMODELSXML_FURTHER_INFORMATION);
								if(furtherInfo!=null){
									model.setFurtherInformation(furtherInfo);
								}
								String furtherMetadata=modelDef.getMetaValue(Ili2cMetaAttrs.ILIMODELSXML_FURTHER_METADATA);
								if(furtherMetadata!=null){
									model.setFurtherMetadata(furtherMetadata);
								}
								String idGeoIV=modelDef.getMetaValue(Ili2cMetaAttrs.ILIMODELSXML_ID_GEO_IV);
								String tags=modelDef.getMetaValue(Ili2cMetaAttrs.ILIMODELSXML_TAGS);
								if(tags==null){
									tags=idGeoIV;
								}else if(idGeoIV!=null){
									tags=tags+","+idGeoIV;
								}
								if(tags!=null){
									model.setTags(tags);
								}
								String original=modelDef.getMetaValue(Ili2cMetaAttrs.ILIMODELSXML_ORIGINAL);
								if(original!=null){
									model.setOriginal(original);
								}
								if(model.getSchemaLanguage().equals(ModelMetadata.ili2_3) || model.getSchemaLanguage().equals(ModelMetadata.ili2_4)){
									String issuer=modelDef.getIssuer();
									model.setIssuer(issuer);
									String version=modelDef.getModelVersion();
									model.setVersion(version);
									String versionExpl=modelDef.getModelVersionExpl();
									if(versionExpl!=null){
										model.setVersionComment(versionExpl);
									}
								}
                                String lang=modelDef.getLanguage();
                                if(lang!=null) {
                                    model.setNameLanguage(lang);
                                }
							}
						}
					}
					
				}
			}
			
			// write metadata
			if(true){
				java.io.OutputStream outStream=null;
				XtfWriterBase ioxWriter=null;
				try{
					outStream=new java.io.FileOutputStream(out);
					ioxWriter = new XtfWriterBase( outStream,  ILIREPOSITORY20.getIoxMapping(),"2.3");
					ioxWriter.setModels(new XtfModel[]{ILIREPOSITORY20.getXtfModel()});
					StartTransferEvent startTransferEvent = new StartTransferEvent();
					startTransferEvent.setSender( Main.APP_NAME+"-"+Main.getVersion() );
					ioxWriter.write( startTransferEvent );
					StartBasketEvent startBasketEvent = new StartBasketEvent( ILIREPOSITORY20.RepositoryIndex, "b1");
					ioxWriter.write( startBasketEvent );
					Iterator modeli=models.iterator();
					while(modeli.hasNext()){
						ModelMetadata model=(ModelMetadata)modeli.next();
						ioxWriter.write(new ObjectEvent(RepositoryAccess.mapToIom20(model)));
					}
					
					ioxWriter.write( new EndBasketEvent() );
					ioxWriter.write( new EndTransferEvent() );
					
					ioxWriter.flush();
				}catch(java.io.FileNotFoundException ex){
					EhiLogger.logError(ex);
					return false;
				}finally{
					if(ioxWriter!=null){
						ioxWriter.close();
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
			}
		} catch (Exception ex) {
			EhiLogger.logError(Main.APP_NAME+": failed",ex);
			return false;
		}
		return true;
	}
	/** scans a directory for ili-files.
	 * @param startdir directory to scan
	 * @return set<ModelMetadata>
	 */
	public static HashSet<ModelMetadata> scanIliFileDir(File startdir)
	throws IOException
	{
		int oid=1;
		if(!startdir.isDirectory()){
			throw new IllegalArgumentException(startdir+" is not a folder/directory");
		}
		java.net.URI starturi=startdir.toURI();
		HashSet<ModelMetadata> ret=new HashSet<ModelMetadata>();
		ArrayList<File> dirs=new ArrayList<File>();
		dirs.add(startdir);
		ch.ehi.basics.view.GenericFileFilter filter=new ch.ehi.basics.view.GenericFileFilter("INTERLIS models (*.ili)","ili");
		while(!dirs.isEmpty()){
			File dir=dirs.remove(0);
			File filev[]=dir.listFiles();
			for(int i=0;i<filev.length;i++){
				if(filev[i].isDirectory()){
					dirs.add(filev[i]);
					continue;
				}
				if(filter.accept(filev[i])){
					IliFile iliFile=ch.interlis.ili2c.ModelScan.scanIliFile(filev[i]);
					if(iliFile!=null){
						java.net.URI relPath=starturi.relativize(filev[i].toURI());
						String md5=RepositoryAccess.calcMD5(filev[i]);
						String fileVersion = getFileVersion(filev[i]);
						Iterator<IliModel> modeli=iliFile.iteratorModel();
						while(modeli.hasNext()){
							IliModel iliModel=modeli.next();
							ModelMetadata model=new ModelMetadata();
							model.setOid(Integer.toString(oid++));
							model.setFile(relPath.toString());
							model.setName(iliModel.getName());
							double cslVersion=iliModel.getIliVersion();
							if(cslVersion==1.0){
								model.setSchemaLanguage(ModelMetadata.ili1);
							}else if(cslVersion==2.2){
								model.setSchemaLanguage(ModelMetadata.ili2_2);
							}else if(cslVersion==2.3){
								model.setSchemaLanguage(ModelMetadata.ili2_3);
                            }else if(cslVersion==2.4){
                                model.setSchemaLanguage(ModelMetadata.ili2_4);
							}else{
								throw new IllegalStateException("unexpected ili version");
							}
							model.setPublishingDate(fileVersion);
							model.setVersion(fileVersion);
							model.setMd5(md5);
							Iterator<String> depi=iliModel.getDependencies().iterator();
							while(depi.hasNext()){
								String depModelName=depi.next();
								model.addDependsOnModel(depModelName);
							}
							ret.add(model);
						}
					}
				}
			}
		}
		return ret;
	}
	private static String getFileVersion(File file) {
		String fileVersion= new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(file.lastModified()));
		String fileName=ch.ehi.basics.view.GenericFileFilter.stripFileExtension(file.getName());
		if(fileName.length()>=10){
			String dateString = fileName.substring(fileName.length()-10);
			java.util.Date date=null;
			date=new java.text.SimpleDateFormat("yyyy-MM-dd").parse(dateString,new ParsePosition(0));
			if(date==null){
				date=new java.text.SimpleDateFormat("yyyyMMdd").parse(dateString,new ParsePosition(2));
			}
			if(date!=null){
				fileVersion= new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
			}
		}
		
		return fileVersion;
	}

	/** read an ilimodels.xml file
	 * 
	 * @param uri uri of the repository without ilimodels.xml
	 * @return null if the repository doesn't exist
	 */
	private HashMap<String,ArrayList<ModelMetadata>> readIliModelsXml(File file)
	//throws RepositoryAccessException
	{
		if(file==null){
			return null;
		}
		// read file
		ArrayList<ModelMetadata> models=new ArrayList<ModelMetadata>(); // array<ModelMetadata>
		ch.interlis.iom_j.xtf.XtfReader reader=null;
		try {
			reader=new ch.interlis.iom_j.xtf.XtfReader(file);
			reader.getFactory().registerFactory(ch.ehi.iox.ilisite.ILIREPOSITORY09.getIoxFactory());
            reader.getFactory().registerFactory(ch.interlis.models.ILIREPOSITORY20.getIoxFactory());
			ch.interlis.iox.IoxEvent event=null;
			do{
				 event=reader.read();
				 if(event instanceof ch.interlis.iox.ObjectEvent){
					 ch.interlis.iom.IomObject iomObj=((ch.interlis.iox.ObjectEvent)event).getIomObject();
					 ModelMetadata model=null;
					 if(iomObj instanceof ch.ehi.iox.ilisite.IliRepository09.RepositoryIndex.ModelMetadata){
						 model=RepositoryAccess.mapFromIom09((ch.ehi.iox.ilisite.IliRepository09.RepositoryIndex.ModelMetadata)iomObj);
					 }else if(iomObj instanceof ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata){
                         model=RepositoryAccess.mapFromIom20((ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata)iomObj);
					 }
                     if(model!=null) {
                         models.add(model);
                         try {
                             long tidInt=Long.parseLong(model.getOid());
                             if(tidInt>newtid){
                                 newtid=tidInt;
                             }
                         } catch (NumberFormatException e) {
                             // ignore it
                         }
                     }
				 }
			}while(!(event instanceof ch.interlis.iox.EndTransferEvent));
		} catch (IoxException e) {
			throw new IllegalStateException(e);
		}finally{
			newtid=((newtid/10)+1)*10; // start with next 10
			if(reader!=null){
				try {
					reader.close();
				} catch (IoxException e) {
					throw new IllegalStateException(e);
				}
				reader=null;
			}
		}
		HashMap<String, ArrayList<ModelMetadata>> files = createFilelist(models);
		return files;
	}
	private HashMap<String, ArrayList<ModelMetadata>> createFilelist(
			java.util.Collection<ModelMetadata> models) {
		HashMap<String,ArrayList<ModelMetadata>> files=new HashMap<String,ArrayList<ModelMetadata>>();
		Iterator<ModelMetadata> modeli=models.iterator();
		while(modeli.hasNext()){
			ModelMetadata model=modeli.next();
				String filename=model.getFile();
				ArrayList<ModelMetadata> modelv=null;
				if(!files.containsKey(filename)){
					modelv=new ArrayList<ModelMetadata>();
					files.put(filename, modelv);
				}else{
					modelv=files.get(filename);
				}
				modelv.add(model);
		}
		return files;
	}
}
