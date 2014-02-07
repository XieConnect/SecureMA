#!/usr/bin/Rscript
# simulate the binary circuit for estimating n and epsilon

args <- commandArgs(TRUE)

# Input two shares for X
x1 <- as.integer(args[1])
x2 <- as.integer(args[2])

# to customize inputs
maxN <- 80
bitLength <- 80
# input randomization parameters
randa <- 0
randb <- 0
 
## init
est <- 1
n <- 0
x <- x1 + x2
print(paste("Input x:  ", x))

# Estimate n and est
for (i in 1:maxN) {
  if (est < x) {
    est = 2*est
    n = n+1
  }
}

print(paste("[Rough estimate] est:  ", est))
print(paste("[Rough estimate] n:    ", n))

## scale epsilon
jEnd <- maxN - n
est <- x - est

for (i in 1:maxN) {
  if (i <= jEnd) {
    est <- 2 * est
  }

}

print(paste("Scaled epsilon:  ", est))

print("-- Server: ---")
print(paste("est - randa:  ", est - randa))
print(paste("   or (est + fieldSize):  ", est - randa + 2^bitLength))
print(paste("n - randb:  ", n - randb))

print("")
print("-- Client: ---")
print(paste("- randa:  ", randa))
print(paste("- randb:  ", randb))
