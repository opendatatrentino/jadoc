<p class="josman-to-strip">
WARNING: WORK IN PROGRESS - THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://opendatatrentino.github.io/josman/" target="_blank">PROJECT WEBSITE</a>
</p>

This #{version} release is just for testing docs and release system.

### Maven

Josman is available on Maven Central. To use it, put this in the dependencies section of your _pom.xml_:

```
    <dependency>
        <groupId>eu.trentorise.opendata</groupId>
        <artifactId>josman</artifactId>
        <version>#{version}</version>            
    </dependency>
```

In case updates are available, version numbers follows <a href="http://semver.org/" target="_blank">semantic versioning</a> rules.

### Usage

At present the project is quite underdocumented. You can find some examples in [tests](tests.md) page. Markdown to Html conversion is done by <a href="https://github.com/sirthias/pegdown" target="_blank"> PegDown </a>, and some further tweaks to the generated HTML are done with <a href="http://jodd.org/doc/jerry" target="_blank"> Jerry</a>