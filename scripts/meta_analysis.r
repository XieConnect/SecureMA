#!/usr/bin/Rscript

# to customize: input data
mydata <- read.csv("~/test/page_data/outlier.csv", sep=",", head=T)
betas <- mydata[, 3]
ses <- mydata[, 4]

weight <- 1 / (ses^2)
se <- sqrt(1 / sum(weight))
beta <- sum(betas * weight) / sum(weight)

z_score <- beta / se
p_value <- 2 * pnorm(-abs(z_score))
print(p_value)


if (F) {
mydata["p_value_secure"] <- NA
mydata$p_value_secure <- 2 * pnorm(-abs(mydata[, 1]))

mydata["p_value_plain"] <- NA
mydata$p_value_plain <- 2 * pnorm(-abs(mydata[, 2]))

write.table(mydata, file=paste(datadir, "/data/p_value.csv", sep=""), sep=",", quote=F, row.names=F, col.names=T)

}
