# Distributed Secure Meta-analysis #
Distributed secure meta-analysis for GWAS.


## Description ##

- Owner.scala : data owners encrypt and contribute their data;
- Mediator.scala : secure computation (aggregation); run as Bob in Fairplay in estimating ln(x);
- Manager.scala : collects data encryptions from data owners; run as Alice in Fairplay;


## Dependencies ##
- Java 1.6 or higher
- Other required jars are included in lib/

### To build documentation, you also need: ###
- UMLGraph: for sequence diagram generation in doc/*
- texlive: for LaTeX (large package!)
- texlive-fonts-recommended: required fonts


## Directory structure ##
- doc/* : documentary for whole protocol
- data/raw_data_sorted.csv: input raw data for meta-analysis (delimitered by comma). This should be sorted with first line as header. Important columns include:
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
- Run in parallel


## For Developers
- Generate documents
  - doc/draw_graph : draw sequence diagram for whole protocol;
  - details.tex: technical details for whole protocol;


## Authors
- Wei Xie <wei.xie@vanderbilt.edu>