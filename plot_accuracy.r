#!/usr/bin/Rscript

# to customize: input data
datadir <- "experiment/eagle_T2D_3_Taylor_downto_8/data/"

mydata <- read.csv(paste(datadir, "final_result.csv", sep=""), skip=1)
experiment_indices=34

# plot accuracy
png(paste(datadir, "accuracy.png", sep=""),
    width = 800, height = 600, units = "px", pointsize = 12)

## absolute error
plot(mydata[, 3],
     #type="l",
     #xlim=c(1, experiment_indices),
     main="error of secure meta-analysis",
     xlab="experiment index",
     ylab="error of secure meta-analysis",
     pch=22,
     col="blue")

## relative error
points(mydata[, 4],
       pch=20,
      col="red")

legend("topright",
       legend=c("absolute error", "relative error"),
       col=c("blue", "red"),
       #ncol=2,
       cex=0.8,
       #bty="n",
       lwd=2)


## plot runtime (over all experiments)
png(paste(datadir, "runtime_total.png", sep=""),
    width = 800, height = 600, units = "px", pointsize = 12)
plot((mydata[, 8] + mydata[, 7]),
     col="blue",
     #xlim=c(1, experiment_indices),
     main="runtime of different experiments",
     xlab="index of experiment",
     #type="l",
     ylab="runtime (seconds)")


## plot runtime breakdown
mytime <- read.csv(paste(datadir, "division_time_breakdown.csv", sep=""), skip=1)

png(paste(datadir, "runtime_breakdown_of_lnx_for_numerator.png", sep=""),
    width = 800, height = 600, units = "px", pointsize = 12)
barplot(t(data.frame(mytime[, 2:3])),
        col=c("blue", "red"),
        legend.text=c("Fairplay", "Taylor expansion"),
        args.legend=list(x="center"),
        main="ln(x) runtime breakdown for numerator"
        )


png(paste(datadir, "runtime_breakdown_of_lnx_for_denominator.png", sep=""),
    width = 800, height = 600, units = "px", pointsize = 12)
barplot(t(data.frame(mytime[, 4:5])),
        col=c("blue", "red"),
        legend.text=c("Fairplay", "Taylor expansion"),
        args.legend=list(x="center"),
        main="ln(x) runtime breakdown for denominator"
        )

# close output
dev.off()
