# Developer's Guide to SecureMA (secure meta-analysis) #
This guide targets advanced developers interested in knowing the technical details of our implementation. (For a general description of the software, please refer to *README.md* file)


## Technical Overview ##
This package is implemented in Scala/Java. Dependencies are handled via Maven (some private libraries are provided separately).

Using the default config, the resulting compiled package (*SecureMA.jar*) will be stand-alone (all dependencies included).


## Dependencies ##
- JDK 1.6+ (version 1.7 recommended);
- Scala 2.10 (version 2.11 recommended);
- Maven
- A few private external libraries (listed in *lib/* folder);
- Please also add to your Java CLASSPATH our enhanced version of *FastGC*;
- Key file *NPOTKey* is required by the FastGC package;
- (*Optional*) The provided helper scripts require a Shell runtime (such as Bash);


## To-Do ##
- Parallel evaluation of circuits



## How to Get ##
- Source code available at: http://github.com/XieConnect/SecureMA


## Copyright & License ##
Refer to the LICENSE and authors.


## Authors
- Wei Xie  (XieConnect@gmail.com)