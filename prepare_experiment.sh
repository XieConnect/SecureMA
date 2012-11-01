#!/bin/bash

# Prepare a separate dir for current experiment containing necessary files
# Usage:
#   ./prepare_experiment.sh <experiment_name> <number_of_processes>
# Note:
#   - no slash in experiment_name

if [ $# -ne 1 ]
then
  echo "./prepare_experiment.sh <experiment_name>"
  exit
fi

basedir=experiment/$1
mkdir -p $basedir
cp copy_processes.sh $basedir/

expdir=$basedir/process0/
mkdir -p $expdir

cp out/MetaAnalysis.jar $expdir
cp conf.properties $expdir
cp -r run $expdir
cp -r start $expdir
mkdir -p $expdir"data"
