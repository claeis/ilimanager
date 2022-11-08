package ch.ehi.ilimanager;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;

import org.interlis2.validator.Validator;
import org.junit.Test;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.ObjectEvent;
import ch.interlis.iox_j.jts.Iox2jtsException;

public class CreateIliDataToolTest {
    private static final String TEST_DATA="src/test/data/createIliDataTool/";
    private static final String ILIDATA_XML = "build/ilidata.xml";

    @Test
    public void localFolder() throws IoxException {
        Settings settings = new Settings();
        settings.setValue(Main.SETTING_ILIDIRS, TEST_DATA);
        boolean ret = CreateIliDataTool.start(new File(ILIDATA_XML),TEST_DATA+"localfolder",null,settings);
        assertTrue(ret);
        
        validateResult();
    }

    @Test
    public void localFolderNonExisiting_Fail() throws IoxException {
        Settings settings = new Settings();
        settings.setValue(Main.SETTING_ILIDIRS, TEST_DATA);
        boolean ret = CreateIliDataTool.start(new File(ILIDATA_XML),TEST_DATA+"nonExistingFolder",null,settings);
        assertFalse(ret);
    }
    
    @Test
    public void repository() throws Iox2jtsException, IoxException {
        Settings settings = new Settings();
        settings.setValue(Main.SETTING_ILIDIRS, TEST_DATA);
        
        boolean ret = CreateIliDataTool.start(new File(ILIDATA_XML),TEST_DATA+"repos1",new File(TEST_DATA+"filelist.txt"),settings);
        assertTrue(ret);
        
        validateResult();
    }
    
    private void validateResult() throws IoxException {
        XtfReader reader = new XtfReader(new File(ILIDATA_XML));
        
        IoxEvent event = null;
        HashMap<String,IomObject> objs=new HashMap<String,IomObject>();
        do {
            event = reader.read();
            if (event instanceof ObjectEvent) {
                IoxEvent event1 = event;
                IomObject iomObject = ((ObjectEvent) event1).getIomObject();
                String id=iomObject.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_id);
                objs.put(id, iomObject);
            }
        } while (!(event instanceof EndTransferEvent));
        assertEquals(3, objs.size());
        {
            IomObject iomObject = objs.get("Beispiel1a");
            assertNotNull(iomObject);
            
            // File/FileFormat
            IomObject files = iomObject.getattrobj(ch.interlis.models.DatasetIdx16.DataIndex.DatasetMetadata.tag_files, 0);
            IomObject file = files.getattrobj(ch.interlis.models.DatasetIdx16.DataFile.tag_file, 0);
            assertEquals("sub/Beispiel1a.itf", file.getattrvalue(ch.interlis.models.DatasetIdx16.File.tag_path));
            assertNotNull(file.getattrvalue(ch.interlis.models.DatasetIdx16.File.tag_md5));
            assertEquals("application/interlis+txt;version=1.0", files.getattrvalue(ch.interlis.models.DatasetIdx16.DataFile.tag_fileFormat));
            
            // Owner
            assertEquals(CreateIliDataTool.getOwner(null), iomObject.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_owner));
            
            // Baskets
            IomObject baskets = iomObject.getattrobj(ch.interlis.models.DatasetIdx16.DataIndex.DatasetMetadata.tag_baskets, 0);
            // ModelName 
            IomObject model = baskets.getattrobj(ch.interlis.models.DatasetIdx16.Metadata.tag_model, 0);
            assertEquals("Beispiel1.Bodenbedeckung", model.getattrvalue(ch.interlis.models.DatasetIdx16.ModelLink.tag_name));
            assertEquals(CreateIliDataTool.getOwner(null), baskets.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_owner));
            assertEquals("itf0", baskets.getattrvalue(ch.interlis.models.DatasetIdx16.DataIndex.BasketMetadata.tag_localId));
            assertEquals("1", baskets.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_version));
            
            
        }
        {
            IomObject iomObject = objs.get("Beispiel2a");
            assertNotNull(iomObject);
            
            // File/FileFormat
            IomObject files = iomObject.getattrobj(ch.interlis.models.DatasetIdx16.DataIndex.DatasetMetadata.tag_files, 0);
            IomObject file = files.getattrobj(ch.interlis.models.DatasetIdx16.DataFile.tag_file, 0);
            assertEquals("sub/Beispiel2a.xtf", file.getattrvalue(ch.interlis.models.DatasetIdx16.File.tag_path));
            assertEquals("application/interlis+xml;version=2.3", files.getattrvalue(ch.interlis.models.DatasetIdx16.DataFile.tag_fileFormat));
            
            // Owner
            assertEquals(CreateIliDataTool.getOwner(null), iomObject.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_owner));
            
            // Baskets
            IomObject baskets = iomObject.getattrobj(ch.interlis.models.DatasetIdx16.DataIndex.DatasetMetadata.tag_baskets, 0);
            // ModelName
            IomObject model = baskets.getattrobj(ch.interlis.models.DatasetIdx16.Metadata.tag_model, 0);
            assertEquals("Beispiel2.Bodenbedeckung", model.getattrvalue(ch.interlis.models.DatasetIdx16.ModelLink.tag_name));
            assertEquals(CreateIliDataTool.getOwner(null), baskets.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_owner));
            assertEquals("b1", baskets.getattrvalue(ch.interlis.models.DatasetIdx16.DataIndex.BasketMetadata.tag_localId));
            assertEquals("1", baskets.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_version));
            
            // 2. Basket
            IomObject baskets2 = iomObject.getattrobj(ch.interlis.models.DatasetIdx16.DataIndex.DatasetMetadata.tag_baskets, 1);
            assertEquals("cb3817b2-ebb9-4346-a406-0e30c81eff7d", baskets2.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_id));
            IomObject model2 = baskets2.getattrobj(ch.interlis.models.DatasetIdx16.Metadata.tag_model, 0);
            assertEquals("Beispiel2.GebaeudeRegister", model2.getattrvalue(ch.interlis.models.DatasetIdx16.ModelLink.tag_name));
            assertEquals(CreateIliDataTool.getOwner(null), baskets2.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_owner));
            assertEquals("1", baskets2.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_version));
            
        }
        {
            IomObject iomObject = objs.get("Beispiel3a");
            assertNotNull(iomObject);
            
            // File/FileFormat
            IomObject files = iomObject.getattrobj(ch.interlis.models.DatasetIdx16.DataIndex.DatasetMetadata.tag_files, 0);
            IomObject file = files.getattrobj(ch.interlis.models.DatasetIdx16.DataFile.tag_file, 0);
            assertEquals("Beispiel3a.itf", file.getattrvalue(ch.interlis.models.DatasetIdx16.File.tag_path));
            assertNotNull(file.getattrvalue(ch.interlis.models.DatasetIdx16.File.tag_md5));
            assertEquals("application/interlis+txt;version=1.0", files.getattrvalue(ch.interlis.models.DatasetIdx16.DataFile.tag_fileFormat));
            
            // Owner
            assertEquals(CreateIliDataTool.getOwner(null), iomObject.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_owner));
            
            // Baskets
            IomObject baskets = iomObject.getattrobj(ch.interlis.models.DatasetIdx16.DataIndex.DatasetMetadata.tag_baskets, 0);
            // ModelName 
            IomObject model = baskets.getattrobj(ch.interlis.models.DatasetIdx16.Metadata.tag_model, 0);
            assertEquals("Beispiel1.Bodenbedeckung", model.getattrvalue(ch.interlis.models.DatasetIdx16.ModelLink.tag_name));
            assertEquals(CreateIliDataTool.getOwner(null), baskets.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_owner));
            assertEquals("itf0", baskets.getattrvalue(ch.interlis.models.DatasetIdx16.DataIndex.BasketMetadata.tag_localId));
            assertEquals("1", baskets.getattrvalue(ch.interlis.models.DatasetIdx16.Metadata.tag_version));
            
            
        }
        // Validate generated IliDataXml
        boolean runValidation = Validator.runValidation(new String[] { ILIDATA_XML }, null);
        assertTrue(runValidation);
    }
    
}
