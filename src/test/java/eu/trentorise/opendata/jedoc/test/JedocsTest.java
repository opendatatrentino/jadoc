package eu.trentorise.opendata.jedoc.test;

import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.jedoc.Jedocs;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class JedocsTest {
    
        private static final Logger LOG = Logger.getLogger(JedocsTest.class.getName());

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(JedocsTest.class);
    }   
    
    @Test
    public void testNotMeaningfulString(){
        
        Jedocs.checkNotMeaningful(" a", "");
        Jedocs.checkNotMeaningful(" a\n", "");
        Jedocs.checkNotMeaningful(" a\t", "");
        
        try {
            Jedocs.checkNotMeaningful("", "");
        } catch (IllegalArgumentException ex){
                
        }
        
        try {
            Jedocs.checkNotMeaningful(" ", "");
        } catch (IllegalArgumentException ex){
                
        }
        try {
            Jedocs.checkNotMeaningful("\\t", "");
        } catch (IllegalArgumentException ex){
                
        }
        
        try {
            Jedocs.checkNotMeaningful("\\n", "");
        } catch (IllegalArgumentException ex){
                
        }
                        

    }
}
