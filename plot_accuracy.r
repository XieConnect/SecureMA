#!/usr/bin/Rscript

# to customize: input data
inputdir <- c(
              "~/data/foxe1_second_run/foxe1_second_run/data/",
              "~/data/foxe1_second_run/foxe1_second_run_multiplier_downto_6/data/",
              "~/data/foxe1_second_run/foxe1_second_run_multiplier_downto_8/data/",
              "~/data/foxe1_second_run/foxe1_second_run_multiplier_upto_12/data/"
              )

runtime_y_max <- 17500

for(datadir in inputdir){
  mydata <- read.csv(paste(datadir, "final_result.csv", sep=""), sep=",", head=TRUE)
  #experiment_indices=34

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

  legend("topright",
         legend=c("absolute error", "relative error"),
         col=c("blue", "red"),
         pch=c(22,20),
         #ncol=2,
         cex=0.8,
         #bty="n"
         )

  # Plot relative error separately
  svg(paste(datadir, "accuracy_relative.svg", sep=""),
      width=8, height=6)
  plot(mydata[, 4],
       main="relative error of secure meta-analysis",
       xlab="experiment index",
       ylab="relative error",
       pch=20,
       col="red")

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
  mytime <- read.csv(paste(datadir, "division_time_breakdown.csv", sep=""), sep=",",head=TRUE)

  svg(paste(datadir, "runtime_breakdown_of_lnx_for_numerator.svg", sep=""),
      width=8, height=6)

  barplot(t(data.frame(mytime[1:15, 2:3])),
          col=c("cyan", "red"),
          xlab="experiment index",
          ylab="runtime breakdown (ms)",
          yaxp=c(0,18000,6),
          yaxs="i",
          #ylim=c(0, 18000),
          legend.text=c("Fairplay", "Taylor expansion"),
          args.legend=list(x="center"),
          main="ln(x) runtime breakdown for numerator"
          )


  svg(paste(datadir, "runtime_breakdown_of_lnx_for_denominator.svg", sep=""),
             width=8, height=6)
  barplot(t(data.frame(mytime[1:15, 4:5])),
          col=c("cyan", "red"),
          xlab="experiment index",
          ylab="runtime breakdown (ms)",
          yaxp=c(0,18000,6),
          legend.text=c("Fairplay", "Taylor expansion"),
          args.legend=list(x="center"),
          main="ln(x) runtime breakdown for denominator"
          )

  # close output
  dev.off()
}
