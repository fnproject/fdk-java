#!/bin/bash

# Horrible bash checker...

FOUND_FILENAME=`pwd`/success
rm -f "$FOUND_FILENAME"
ATTEMPT=0
while [ ! -f "$FOUND_FILENAME" ] ;
do
  sleep 1
  calls_found=`fn list calls "test-5" | grep "Status: success" | wc -l`
  echo "$calls_found successful function calls found"

  # TODO: Remove this check when `fn logs` becomes reliable
  if [[ -n `echo $calls_found | grep "3"` ]]; then
    touch "$FOUND_FILENAME"
  fi

  # TODO: Use this check instead when `fn logs` becomes reliable
  # fn list calls "test-5" | while read k v
  # do
  #   if [[ "$k" = "ID:" ]]; then id="$v"; fi
  #   if [[ -z "$k" ]]; then
  #     LOG=`fn get log "test-5" "$id"`
  #     echo $LOG
  #     if [[ $LOG == *"Ran the hook."* ]]; then
  #        touch "$FOUND_FILENAME"
  #     fi
  #   fi
  # done

  ATTEMPT=$((ATTEMPT + 1))
  if [ $ATTEMPT -ge 120 ];
  then
    # echo "Did not find termination hook output"
    echo "Termination hook was not called or failed"
    exit 1
  fi
done
