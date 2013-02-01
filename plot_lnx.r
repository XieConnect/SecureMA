#!/usr/bin/Rscript

# to customize: input data
datadir <- "data/Sub.txt.all.csv"
mydata <- read.csv(datadir, sep=",", skip=2)

# plot accuracy
svg("data/Sub.txt.all.absolute_error.svg", width=8, height=6)
## absolute error
plot(mydata[1:260, 4],
     #type="l",
     #xlim=c(1, experiment_indices),
     ylim=c(0,5.5e-2),
     main="Absolute error of secure ln(x)",
     xlab="x",
     ylab="Absolute error",
     pch=20,
     col="blue")

points(mydata[1:260, 9],
       pch=4,
      col="red")

abline(v=2^(2:8) - 1, col="gray60")

legend(180, 0.05,
       #"topright",
       legend=c("over-estimate", "under-estimate"),
       col=c("blue", "red"),
       pch=c(20,4),
       #ncol=2,
       cex=0.8,
       #bty="n"
       )


# plot relative error
svg("data/Sub.txt.all.relative_error.svg", width=8, height=6)
## absolute error
plot(mydata[1:260, 5],
     #type="l",
     #xlim=c(1, experiment_indices),
     ylim=c(0,1.5e-2),
     main="Relative error of secure ln(x)",
     xlab="x",
     ylab="Relative error",
     pch=20,
     col="blue")

## relative error
points(mydata[1:260, 10],
       pch=4,
      col="red")


#TODO why need an offset?
abline(v=2^(2:8) - 1, col="gray60")

legend("topright",
       legend=c("over-estimate", "under-estimate"),
       col=c("blue", "red"),
       pch=c(20,4),
       #ncol=2,
       cex=0.8,
       #bty="n"
       )


# close output
dev.off()
