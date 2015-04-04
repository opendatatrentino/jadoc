package eu.trentorise.opendata.josman.test;

import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.josman.JosUtils;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class JosmansTest {
    
        private static final Logger LOG = Logger.getLogger(JosmansTest.class.getName());

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(JosmansTest.class);
    }   
    
    @Test
    public void testNotMeaningfulString(){
        
        JosUtils.checkNotMeaningful(" a", "");
        JosUtils.checkNotMeaningful(" a\n", "");
        JosUtils.checkNotMeaningful(" a\t", "");
        
        try {
            JosUtils.checkNotMeaningful("", "");
        } catch (IllegalArgumentException ex){
                
        }
        
        try {
            JosUtils.checkNotMeaningful(" ", "");
        } catch (IllegalArgumentException ex){
                
        }
        try {
            JosUtils.checkNotMeaningful("\\t", "");
        } catch (IllegalArgumentException ex){
                
        }
        
        try {
            JosUtils.checkNotMeaningful("\\n", "");
        } catch (IllegalArgumentException ex){
                
        }
                        

    }
}
