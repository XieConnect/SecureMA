#!/usr/bin/Rscript
# simulate the binary circuit for estimating n and epsilon

# to customize inputs
maxN <- 80
x <- 1 + 5

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

print(paste("est:  ", est))
