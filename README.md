# SecureMA: secure meta-analysis across multiple sites #

SecureMA is a secure framework for supporting genetic association studies (via meta-analysis) over distributed data sites while preserving data privacy at both person- and site-level. Specifically, person-level data and site-level summary statistics are fully protected (encrypted/randomized) throughout the computation. And the final result (association p-value) of meta-analysis is only made known to the designated recipient.


## Technical Overview ##

This demonstration package is primarily implemented in Scala/Java. We provide native API to any JVM-based languages (such as Java).

The whole project can be deployed as one single self-contained JAR package and run on any system where JVM is available.


## How to Get ##

- Source code available at: http://github.com/XieConnect/SecureMA
- Pre-compiled JAR package: (TO ANNOUNCE SOON)


## Lazy User's Guide ##

Assume you already downloaded the pre-packaged SecureMA.jar .

1. Organize input according to required format;
2. Run circuit evaluation backends (e.g., <i>circuit_servers</i>);
3. Run the experiment;


## Description ##

- Owner.scala : data owners (e.g., local sites) encrypt and contribute their data to entrusted parties;
- Manager.scala : collects data encryptions from data owners and act as data
  delegates;
- Mediator.scala : secure computation (aggregation);


## Dependencies ##
- JDK 1.6+
- Other required jars are included in lib/ or specified as Maven dependencies;
- (optional) scalatest jar package (for automated tests only);


## Directory structure ##
- data/raw_data_sorted.csv: input raw data for meta-analysis (delimitered by comma). This should be sorted by experiment identifiers (to distinguish between different experiments); the first row should be the header. Important columns include:
    * standard error (or se., 12th column)
    * beta (11th column)
    * the other columns (1 to 4th, and 6 to 10th) will be combined to form an identifier to distinguish different experiments (in other words, you can safely put site names in column 5).


## Notes ##
- API for Alice and Bob has changed a little from original one (to config server and port to run socket)
- Check if current port available: lsof -i :3496  (3496 is port you desire)
- When customizing MaxN (as in Fairplay), remember to :
    * modify two related parameters in Fairplay script;
    * modify max_exponent_n in .properties file;
- When customizing multiplier in SMC, remember to:
    * modify .properties file;
    * re-generate input data (encrypted_data.csv);


## To-Do ##
- Parallel evaluation of circuits


## Copyright & License ##
Refer to the LICENSE and authors.


## Authors
- Wei Xie  (XieConnect@gmail.com)