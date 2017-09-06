#!/bin/bash

# Horrible bash checker...

FOUND_FILENAME=`pwd`/success
rm -f "$FOUND_FILENAME"
ATTEMPT=0
while [ ! -f "$FOUND_FILENAME" ] ;
do

  fn calls list "test-5" | while read k v
  do
    if [[ "$k" = "ID:" ]]; then id="$v"; fi
    if [[ -z "$k" ]]; then
      LOG=`fn logs get "test-5" "$id"`
      if [[ $LOG == *"Ran the hook."* ]]; then
         touch "$FOUND_FILENAME"
      fi
    fi
  done

sleep 5
ATTEMPT=$((ATTEMPT + 1))
if [ $ATTEMPT -ge 12 ];
then
  echo "Did not find termination hook output"
  exit 1
fi

done
