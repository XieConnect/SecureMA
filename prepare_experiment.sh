#!/bin/bash

# Prepare a separate dir for current experiment containing necessary files
# Usage:
#   ./prepare_experiment.sh <experiment_name> <number_of_processes>
# Note:
#   - no slash in experiment_name

basedir=experiment/$1
mkdir -p $basedir

expdir=$basedir/process0/
mkdir -p $expdir

cp out/MetaAnalysis.jar $expdir
cp conf.properties $expdir
cp -r run $expdir
cp -r start $expdir
mkdir -p $expdir"data"

# generate multiple copies if needed
if [ $2 -gt 1 ]
then
    endIndx=$2
    let endIndx=endIndx-1
    for indx in $(eval echo {1..$endIndx})
    do
        cp -r $expdir $basedir/process"$indx"
    done
fi