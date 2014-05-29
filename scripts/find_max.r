#!/usr/bin/Rscript
# Output max value for numerator and denominator in future division
# This runs on Data Owners' side, since it needs access to raw data

# to customize: input data
datadir <- "data/raw_data_sorted.csv"
mydata <- read.csv(datadir, sep=",", head=TRUE)

# s.e. is in column 12
# beta is in column 11
weight_i <- (1 / mydata[, 12])^2
beta_weight <- mydata[, 11] * weight_i

# max of numerator (without square)
print( max(weight_i) )
# max of denominator
print( max(beta_weight) )
