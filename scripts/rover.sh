# Usage:
# ./rover.sh [ID]
# ./rover.sh [ID] sf

if [ "$2" = "sf" ]
then
  cd /media/sf_CC/ || exit #path to repo as a shared folder (sf)
else
  cd /home/core/CC/ || exit
fi

java -cp target/classes/ Rover.Rover "$1"