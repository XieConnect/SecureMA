#!/usr/bin/Rscript

<<<<<<< HEAD
# to customize
datadir <- "~/data/eagle_T2D_without_combined/"
=======
# to customize: input data
inputdir <- c(
              "experiment/eagle_T2D_3/data/",
              "experiment/eagle_T2D_3_multiplier_downto_8/data/",
              "experiment/eagle_T2D_3_MaxN_downto_50/data/",
              "experiment/eagle_T2D_3_MaxN_downto_42/data/",
              "experiment/eagle_T2D_3_Taylor_downto_8/data/",
              "experiment/eagle_T2D_3_Taylor_to_12/data/"
              )
>>>>>>> 2593628... Plot multiple experiments simultaneously

for(datadir in inputdir){
  #datadir <- "experiment/eagle_T2D_3_Taylor_downto_8/data/"

<<<<<<< HEAD
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
=======
  mydata <- read.csv(paste(datadir, "final_result.csv", sep=""), skip=1)
  experiment_indices=34

  # plot accuracy
  svg(paste(datadir, "accuracy.svg", sep=""),
      width=8, height=6)
  ## absolute error
  plot(mydata[, 3],
       #type="l",
       #xlim=c(1, experiment_indices),
       main="error of secure meta-analysis",
       xlab="experiment index",
       ylab="error",
       pch=22,
       col="blue")

  ## relative error
  points(mydata[, 4],
         pch=20,
        col="red")
>>>>>>> 2593628... Plot multiple experiments simultaneously

  legend("topright",
         legend=c("absolute error", "relative error"),
         col=c("blue", "red"),
         #ncol=2,
         cex=0.8,
         #bty="n",
         lwd=2)

  # Plot relative error separately
  svg(paste(datadir, "accuracy_relative.svg", sep=""),
      width=8, height=6)
  plot(mydata[, 4],
       main="relative error of secure meta-analysis",
       xlab="experiment index",
       ylab="relative error",
       pch=20,
       col="red")

<<<<<<< HEAD
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
=======
  ## plot runtime (over all experiments)
  svg(paste(datadir, "runtime_total.svg", sep=""),
      width=8, height=6)
  plot((mydata[, 8] + mydata[, 7]),
       col="blue",
       #xlim=c(1, experiment_indices),
       main="runtime of different experiments",
       xlab="index of experiment",
       #type="l",
       ylab="runtime (seconds)")


  ## plot runtime breakdown
  mytime <- read.csv(paste(datadir, "division_time_breakdown.csv", sep=""), skip=1)

  svg(paste(datadir, "runtime_breakdown_of_lnx_for_numerator.svg", sep=""),
      width=10, height=6)
  barplot(t(data.frame(mytime[0:420, 2:3])),
          col=c("cyan", "red"),
          xlab="experiment index",
          ylab="runtime breakdown",
          ylim=c(0, 25000),
          legend.text=c("Fairplay", "Taylor expansion"),
          args.legend=list(x="center"),
          main="ln(x) runtime breakdown for numerator"
          )


  svg(paste(datadir, "runtime_breakdown_of_lnx_for_denominator.svg", sep=""),
             width=10, height=6)
  barplot(t(data.frame(mytime[0:420, 4:5])),
          col=c("cyan", "red"),
          xlab="experiment index",
          ylab="runtime breakdown",
          ylim=c(0, 25000),
          legend.text=c("Fairplay", "Taylor expansion"),
          args.legend=list(x="center"),
          main="ln(x) runtime breakdown for denominator"
          )
>>>>>>> 2593628... Plot multiple experiments simultaneously

  # close output
  dev.off()
}
