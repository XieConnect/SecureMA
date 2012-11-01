#!/bin/bash

# Generate multiple processes
# PLEASE run this on server after you've already got the first process0 copy
#
# Usage:
#   ./copy_processes.sh <number_of_processes>
# Note:
#   the number excludes process0

# generate multiple copies
if [ $1 -gt 0 ]
then
    endIndx=$1
    let endIndx=endIndx
    for indx in $(eval echo {1..$endIndx})
    do
        cp -r process0 process"$indx"
    done
fi
