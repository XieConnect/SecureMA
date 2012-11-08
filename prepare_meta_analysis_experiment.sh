#!/bin/bash

# Prepare a separate dir for current experiment containing necessary files
# Usage:
#   ./prepare_experiment.sh <experiment_name> <number_of_processes>
# Note:
#   - no slash in experiment_name
dataDir=data/paper_overEstiamte_moreTaylor

if [ $# -ne 1 ]
then
  echo "./prepare_experiment.sh <experiment_name>"
  exit
fi

basedir=experiment/$1
mkdir -p $basedir

cp out/MetaAnalysis.jar $basedir
cp conf.properties $basedir
cp -r run $basedir
cp -r start $basedir
cp prepare_data $basedir

newDataDir=$basedir/"data"
mkdir -p $newDataDir

cp -r $dataDir $newDataDir
cp data/raw_data_sorted.csv $newDataDir
cp data/encrypted_data.csv $newDataDir
