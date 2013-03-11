#!/usr/bin/Rscript

# to customize: input data
inputdir <- c(
              "foxe1_20130217/foxe1_fourth_run_multiplier_8",
              "foxe1_20130217/foxe1_fourth_run_multiplier_6",
              "foxe1_20130217/foxe1_fourth_run_multiplier_10",
              "foxe1_20130218/foxe1_fourth_run_multiplier_8",
              "foxe1_20130218/foxe1_fourth_run_multiplier_6",
              "foxe1_20130218/foxe1_fourth_run_multiplier_10"
              )

for(datadir in inputdir){
  mydata <- read.csv(paste(datadir, "/data/final_result.csv", sep=""), sep=",", head=TRUE)
  #experiment_indices=34

  #print(mydata)
  mydata["p_value_secure"] <- NA
  mydata$p_value_secure <- 2 * pnorm(-abs(mydata[, 1]))

  mydata["p_value_plain"] <- NA
  mydata$p_value_plain <- 2 * pnorm(-abs(mydata[, 2]))

  write.table(mydata, file=paste(datadir, "/data/p_value.csv", sep=""), sep=",", quote=F, row.names=F, col.names=T)
}
