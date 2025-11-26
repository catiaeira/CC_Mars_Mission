# Usage:
# ./mothership.sh
# ./mothership.sh sf

if [ "$1" = "sf" ]
then
  cd /media/sf_CC/ || exit #path to repo as a shared folder (sf)
else
  cd /home/core/CC/ || exit
fi

java -cp target/classes/ Mothership.Mothership