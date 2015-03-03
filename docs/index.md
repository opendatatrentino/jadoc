<p class="jadoc-to-strip">
WARNING: WORK IN PROGRESS - THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE PROJECT WEBSITE
</p>


This #{version} release is just for testing docs and release system.


This is a custom image for this version: <img src="img/test-img.png">

This is an image for all versions: <img src="../img/jadoc-logo-200px.png" width="150px">


Links without extension:
* link to other md file as markdown: [link](other-file)
* link to other md file as html: <a href="other-file">link</a>

Great discovery: links without extension don't work...

Links with extension:
* link to other md file as markdown: [link](other-file.md)
* link to other md file as html: <a href="other-file.md">link</a>

Great discovery: links without extension don't work...


### Usage

Jadoc is available on Maven Central. To use it, put this in the dependencies section of your _pom.xml_:

```
    <dependency>
        <groupId>eu.trentorise.opendata</groupId>
        <artifactId>jadoc</artifactId>
        <version>#{version}</version>            
    </dependency>
```

In case updates are available, version numbers follows <a href="http://semver.org/" target="_blank">semantic versioning</a> rules.


Test code can be found in <a href="../../src/test/java/eu/trentorise/opendata/commons/test" target="_blank">in test directory</a> todo review

Test code can be found in [in test directory!](../../src/test/java/eu/trentorise/opendata/commons/test) todo review
