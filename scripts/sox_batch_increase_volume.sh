#!/usr/bin/bash
# put this into the folder containing wav files and it will increase their volume.

WAV_FILES="*.wav"
for i in $WAV_FILES
do
  if [[ "$i" == "*.wav" ]]
  then
    echo "There are no wav files in this folder."

  else
    mkdir -p louder
    sox -v 5.0 "$i" "louder/$i"
  fi
done
