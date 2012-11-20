#!/usr/bin/Rscript

# to customize
datadir <- "experiment/meta_SNP_phenotype_ethnicity/data/"

mydata <- read.csv(paste(datadir, "final_result.csv", sep=""), skip=2)
experiment_indices=15

# plot accuracy
png(paste(datadir, "accuracy.png", sep=""))
## absolute error
plot(mydata[, 9],
     type="l",
     xlim=c(1, experiment_indices),
     main="accuracy of secure meta-analysis",
     xlab="experiment index",
     ylab="error of secure meta-analysis",
     col="blue")

## relative error
lines(mydata[, 10],
      col="red")

legend("topright",
       legend=c("absolute error", "relative error"),
       col=c("blue", "red"),
       #ncol=2,
       cex=0.8,
       #bty="n",
       lwd=2)


## plot runtime (over all experiments)
png(paste(datadir, "runtime_for_all_experiments.png", sep=""))
plot((mydata[, 11] + mydata[, 12])/1000,
     col="blue",
     xlim=c(1, experiment_indices),
     main="runtime with varying participating sites",
     xlab="experiment index",
     ylab="runtime (seconds)",
     type="l")


## plot runtime breakdown
mytime <- read.csv(paste(datadir, "division_time_breakdown.csv", sep=""), skip=2)

png(paste(datadir, "lnx_runtime_breakdown_for_numerator.png", sep=""))
barplot(t(data.frame(mytime[, 2:3])),
        col=c("blue", "red"),
        legend=c("Fairplay", "poly. eval."),
        main="ln(x) runtime breakdown for numerator"
        )

png(paste(datadir, "lnx_runtime_breakdown_for_denominator", sep=""))
barplot(t(data.frame(mytime[, 4:5])),
        col=c("blue", "red"),
        legend=c("Fairplay", "poly. eval."),
        main="ln(x) runtime breakdown for denominator"
        )


# close output
dev.off()
