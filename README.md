# SecureMA: secure meta-analysis for genetic association studies #

SecureMA is a secure framework for performing multi-site genetic association studies across large consortia, without violating privacy/confidentiality of individual participants or substudy sites. It aims at addressing various attacks on genomic privacy, and provides comprehensive protection over genomic data without impeding scientific endeavor.


## Technical Overview ##

This demonstration software is implemented in Scala/Java, and provides native API for JVM-based languages (such as Java, Scala, etc).

The whole project can be deployed as a stand-alone JAR package and run on any computer with JVM.


## Lazy User's Guide ##

Assume you have already built or downloaded our SecureMA.jar package. Please follow the instructions below to get SecureMA running.

1. Perform local sub-studies, and organize site-level inputs according to required format;
    (for demonstration on a single machine, you can place them in file: *data/raw_data_sorted.csv*);

2. Encrypt local site summaries (each site run the helper functions to encrypt their private summary data);

3. Now launch the backend services for executing garbled circuits (via the provided script: *circuit_servers*);
    
    This will spawn four background Java processes awaiting incoming computations;

4. Run the secure meta-analysis experiments (via script: *start*);

    Currently experiments will be run in sequential in order to to accurately measure runtime.


(The system is tunable via the configuration file *conf.properties*. More details will be provided later)


## Directory structure ##
- *data/raw_data_sorted.csv*: input raw data for meta-analysis (delimitered by comma). This should be sorted by experiment identifiers (to distinguish between different experiments); the first row should be the header. Important columns include:
    * standard error (or se., 12th column)
    * beta (11th column)
    * the other columns (1 to 4th, and 6 to 10th) will be combined to form an identifier to distinguish different experiments (in other words, you can safely put site names in column 5).

- *conf.properties*: configuration file for various sytem parameters;

- *src/*: source code folder;


## Notes ##
- Our backend service require 4 vacant socket ports. You can use this command to check ports: lsof -i :3496  (3496 is port you desire)

- When customizing MaxN (as in Fairplay), remember to :
    * modify max_exponent_n in .properties file;
- When customizing multiplier in SMC, remember to:
    * modify .properties file;
    * re-generate input data (encrypted_data.csv);


## How to Get ##

- Pre-compiled binaries (prepared on JVM 1.7): https://github.com/XieConnect/SecureMA/releases/tag/v0.2

- Source code available at: http://github.com/XieConnect/SecureMA


## Dependencies ##
- JVM 1.6+ (our pre-compiled binaries require JVM 1.7);
- The key file *NPOTKey* is required by the circuit backend service;
- (*Optional*) The provided helper scripts require a Shell runtime (such as Bash);


## Copyright & License ##
Refer to the LICENSE and authors.


## Authors
- Wei Xie  (XieConnect@gmail.com)