
<p align="center">
<img alt="Jackan" src="https://github.com/opendatatrentino/jackan/wiki/img/jackan-logo-200px.png" width="150px">
</p>

[Introduction](Home#introduction)

[Usage](Home#usage)

[Serialization](Home#serialization)

[Logging](Home#logging)
___________________________

### Introduction

Jackan is a Java client library for accessing [CKAN](http://ckan.org/) catalogs. Current release supports reading and searching from CKAN, see [usage section](Home#usage). 

For a roadmap see [project issues](https://github.com/opendatatrentino/Jackan/issues). In the wiki you can also find notes about [[other java clients | OtherResources]]. For contributing/testing, see [[Contributing]] page.

#### Compatibility

**Latest integration report is <a href="http://opendatatrentino.github.io/jackan/reports/latest/" target="_blank">here</a>**.

Jackan supports installations of CKAN >= 2.2a. Although officially the web api version used is always the _v3_, unfortunately CKAN instances behave quite differently from each other according to their software version. So we periodically test Jackan against a list of existing catalogs all over the world. If you're experiencing problems with Jackan, [let us know](https://github.com/opendatatrentino/jackan/issues), if the error occurs in several catalogs we might devote some time to fix it.


### Usage

Jackan is available on Maven Central. To use it, put this in the dependencies section of your _pom.xml_:

```
    <dependency>
	<groupId>eu.trentorise.opendata</groupId>
	<artifactId>jackan</artifactId>
	<version>0.3.1</version>            
    </dependency>
```

In case updates are available, version numbers follows [semantic versioning](http://semver.org/) rules.

#### Get the dataset list of dati.trentino.it:

Test code can be found in [TestApp.java](https://github.com/opendatatrentino/Jackan/blob/master/src/test/java/eu/trentorise/opendata/jackan/test/ckan/TestApp.java)

```

	import eu.trentorise.opendata.jackan.JackanException;
	import eu.trentorise.opendata.jackan.ckan.CkanClient;

	public class App {
		public static void main( String[] args )
		{        
			CkanClient cc = new CkanClient("http://dati.trentino.it");        
			System.out.println(cc.getDatasetList());               
		}
	}

```

#### Get list of first 10 datasets of dati.trentino.it and print their resources:

```
	import eu.trentorise.opendata.jackan.ckan.CkanClient;
	import eu.trentorise.opendata.jackan.ckan.CkanDataset;
	import eu.trentorise.opendata.jackan.ckan.CkanResource;
	import java.util.List;

	public class App 
	{
		public static void main( String[] args )
		{
			
			CkanClient cc = new CkanClient("http://dati.trentino.it");
			
			List<String> ds = cc.getDatasetList(10, 0);
			
			for (String s : ds){
				System.out.println();
				System.out.println("DATASET: " + s);
				CkanDataset d = cc.getDataset(s);            
				System.out.println("  RESOURCES:");
				for (CkanResource r : d.getResources()){                
					System.out.println("    " + r.getName());
					System.out.println("    FORMAT: " + r.getFormat());
					System.out.println("       URL: " + r.getUrl());
				}
			}
		}
	}
```
Should give something like this:

```

DATASET: abitazioni
  RESOURCES:
    abitazioni
    FORMAT: JSON
       URL: http://www.statweb.provincia.tn.it/INDICATORISTRUTTURALISubPro/exp.aspx?idind=133&info=d&fmt=json
    abitazioni
    FORMAT: CSV
       URL: http://dati.trentino.it/storage/f/2013-06-16T113651/_lcmGkp.csv
    numero-di-abitazioni
    FORMAT: JSON
       URL: http://www.statweb.provincia.tn.it/INDICATORISTRUTTURALISubPro/exp.aspx?ntab=Sub_Numero_Abitazioni&info=d&fmt=json
    numero-di-abitazioni
    FORMAT: CSV
       URL: http://dati.trentino.it/storage/f/2013-06-16T113652/_yWBmJG.csv

DATASET: abitazioni-occupate
  RESOURCES:
    abitazioni-occupate
    FORMAT: JSON
       URL: http://www.statweb.provincia.tn.it/INDICATORISTRUTTURALISubPro/exp.aspx?idind=134&info=d&fmt=json
    abitazioni-occupate
    FORMAT: CSV
       URL: http://dati.trentino.it/storage/f/2013-06-16T113653/_iaMMc2.csv
    numero-di-abitazioni-occupate
    FORMAT: JSON
       URL: http://www.statweb.provincia.tn.it/INDICATORISTRUTTURALISubPro/exp.aspx?ntab=Sub_Numero_Abitazioni_Occupate&info=d&fmt=json
    numero-di-abitazioni-occupate
    FORMAT: CSV
       URL: http://dati.trentino.it/storage/f/2013-06-16T113654/__lLACk.csv

...

```

#### Search datasets filtering by tags and groups:

```
import eu.trentorise.opendata.jackan.ckan.CkanClient;
import eu.trentorise.opendata.jackan.ckan.CkanDataset;
import eu.trentorise.opendata.jackan.ckan.CkanQuery;

public class TestApp 
{
          
    public static void main( String[] args )
    {
        CkanQuery query = CkanQuery.filter().byTagNames("settori economici", "agricoltura").byGroupNames("conoscenza");
        
        List<CkanDataset> filteredDatasets = cc.searchDatasets(query, 10, 0).getResults();
        
        for (CkanDataset d : filteredDatasets){
            System.out.println();
            System.out.println("DATASET: " + d.getName());           
        } 
    }
}
```

Should give something like this:

```

DATASET: produzione-di-mele

DATASET: produzione-di-uva-da-vino

DATASET: produzione-lorda-vendibile-frutticoltura

DATASET: produzione-lorda-vendibile-viticoltura

DATASET: produzione-lorda-vendibile-zootecnia

DATASET: produzione-lorda-vendibile-silvicoltura
```

### Serialization

For serialization Jackson library annotations are used. Notice that although field names of Java objects are camelcase (like _authorEmail_), serialized fields follows CKAN API stlye and use underscores (like author_email).

If you want to serialize to json a Java object _obj_ fetched by Jackan, you can call 

```
String json = CkanClient.getObjectMapperClone().writeValueAsString(obj);
```

### Logging

Jackan uses native Java logging system (JUL). If you also use JUL in your application and want to see Jackan logs, you can take inspiration from [jackan test logging properties](https://github.com/opendatatrentino/jackan/blob/master/src/test/resources/odt.commons.logging.properties).  If you have an application which uses SLF4J logging system, you can route logging with <a href="http://mvnrepository.com/artifact/org.slf4j/jul-to-slf4j" target="_blank">JUL to SLF4J bridge</a>, just remember <a href="http://stackoverflow.com/questions/9117030/jul-to-slf4j-bridge" target="_blank"> to programmatically install it first. </a>