#!/usr/bin/Rscript

# to customize
datadir <- "~/data/eagle_T2D_without_combined/"

mydata <- read.csv(paste(datadir, "final_result.csv", sep=""), skip=1)
experiment_indices=34

# plot accuracy
png(paste(datadir, "accuracy.png", sep=""))
## absolute error
plot(mydata[, 3],
     type="l",
     #xlim=c(1, experiment_indices),
     main="error of secure meta-analysis",
     xlab="index of experiment",
     ylab="error of secure meta-analysis",
     col="blue")

## relative error
lines(mydata[, 4],
      col="red")

legend("topright",
       legend=c("absolute error", "relative error"),
       col=c("blue", "red"),
       #ncol=2,
       cex=0.8,
       #bty="n",
       lwd=2)


## plot runtime (over all experiments)
png(paste(datadir, "runtime_SMC_and_division.png", sep=""))
plot((mydata[, 8] + mydata[, 7]),
     col="blue",
     #xlim=c(1, experiment_indices),
     main="runtime of different experiments",
     xlab="index of experiment",
     ylab="runtime (seconds)",
     type="l")


## plot runtime breakdown
mytime <- read.csv(paste(datadir, "division_time_breakdown.csv", sep=""), skip=2)

png(paste(datadir, "runtime_lnx_breakdown_for_numerator.png", sep=""))
barplot(t(data.frame(mytime[, 2:3])),
        col=c("blue", "red"),
        legend.text=c("Fairplay", "poly. eval."),
        args.legend=list(x="center"),
        main="ln(x) runtime breakdown for numerator"
        )


png(paste(datadir, "runtime_lnx_breakdown_for_denominator.png", sep=""))
barplot(t(data.frame(mytime[, 4:5])),
        col=c("blue", "red"),
        legend.text=c("Fairplay", "poly. eval."),
        args.legend=list(x="center"),
        main="ln(x) runtime breakdown for denominator"
        )

# close output
dev.off()
