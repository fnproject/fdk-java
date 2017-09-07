#!/bin/bash

# Horrible bash checker...

FOUND_FILENAME=`pwd`/success
rm -f "$FOUND_FILENAME"
ATTEMPT=0
while [ ! -f "$FOUND_FILENAME" ] ;
do
  sleep 1
  calls_found=`fn calls list "test-6" | grep "Status: success" | wc -l`
  echo "$calls_found successful function calls found"

  fn calls list "test-6" | while read k v
  do
    if [[ "$k" = "ID:" ]]; then id="$v"; fi
    if [[ -z "$k" ]]; then
      LOG=`fn logs get "test-6" "$id"`
      echo $LOG
      if [[ $LOG == *"Caught timeout"* ]]; then
         touch "$FOUND_FILENAME"
      fi
    fi
  done

  ATTEMPT=$((ATTEMPT + 1))
  if [ $ATTEMPT -ge 120 ];
  then
    echo "Did not find termination hook output"
    exit 1
  fi
done
