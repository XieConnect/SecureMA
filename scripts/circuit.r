#!/usr/bin/Rscript
# simulate the binary circuit for estimating n and epsilon
args <- commandArgs(TRUE)

x1 <- as.integer(args[1])
x2 <- as.integer(args[2])

x <- x1 + x2

# to customize inputs
maxN <- 80
bitLength <- 128
 
randa <- 11
randb <- 22

## init
est <- 1
n <- 0

print(paste("x:  ", x))

for (i in 1:maxN) {
  if (est < x) {
    est = 2*est
    n = n+1
  }
}

print(paste("est:  ", est))
print(paste("n:    ", n))


## scale epsilon
jEnd <- maxN - n
est <- x - est

for (i in 1:maxN) {
  if (i <= jEnd) {
    est <- 2 * est
  }

}

#print(paste("est:  ", est))

print("-- Server: ---")
print(paste("est - randa:  ", est - randa))
print(paste("n - randb:  ", n - randb))

print("")
print("-- Client: ---")
print(paste("- randa:  ", - randa))
print(paste("- randb:  ", - randb))
