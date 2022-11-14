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

public class MakeIliModelsXmlTest {
    private static final String TEST_DATA="src/test/data/makeIliModels/";
    private static final String ILIMODELS_XML = "build/ilimodels.xml";

    @Test
    public void localFolder() throws IoxException {
        Settings settings = new Settings();
        settings.setValue(Main.SETTING_ILIDIRS, TEST_DATA+"repos1");
        MakeIliModelsXml2 makeIliModelsXml=new MakeIliModelsXml2();
        boolean ret=makeIliModelsXml.mymain(false,new File(ILIMODELS_XML),TEST_DATA+"localfolder",settings);
        assertTrue(ret);
        
        validateResult();
    }

    
    private void validateResult() throws IoxException {
        XtfReader reader = new XtfReader(new File(ILIMODELS_XML));
        try {
            
            IoxEvent event = null;
            HashMap<String,IomObject> objs=new HashMap<String,IomObject>();
            do {
                event = reader.read();
                if (event instanceof ObjectEvent) {
                    IoxEvent event1 = event;
                    IomObject iomObject = ((ObjectEvent) event1).getIomObject();
                    String id=iomObject.getobjectoid();
                    objs.put(id, iomObject);
                }
            } while (!(event instanceof EndTransferEvent));
            assertEquals(1, objs.size());
            {
                IomObject iomObject = objs.get("0");
                assertNotNull(iomObject);
                assertEquals("Model",iomObject.getattrvalue(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_Name));
                assertEquals("ili2_3",iomObject.getattrvalue(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_SchemaLanguage));
                assertEquals("Model23.ili",iomObject.getattrvalue(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_File));
                assertEquals("2020-03-23",iomObject.getattrvalue(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_Version));
                assertEquals("2022-11-14",iomObject.getattrvalue(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_publishingDate));
                assertEquals("mailto:ce@eisenhutinformatik.ch",iomObject.getattrvalue(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_Issuer));
                assertEquals("false",iomObject.getattrvalue(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_browseOnly));
                IomObject dependsOn = iomObject.getattrobj(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_dependsOnModel, 0);
                assertEquals("Basis", dependsOn.getattrvalue(ch.interlis.models.IliRepository20.ModelName_.tag_value));
                assertNotNull(iomObject.getattrvalue(ch.interlis.models.IliRepository20.RepositoryIndex.ModelMetadata.tag_md5));
            }
        }finally {
            reader.close();
        }
        // Validate generated IliDataXml
        boolean runValidation = Validator.runValidation(new String[] { ILIMODELS_XML }, null);
        assertTrue(runValidation);
    }
    
}
