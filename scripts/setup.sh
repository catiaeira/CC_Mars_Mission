# Usage:
# ./scripts/setup.sh
# ./scripts/setup.sh mvn

#compiles the source code
if [ "$1" = "mvn" ]
then
  mvn clean compile #compile with maven by using ´./scripts/setup.sh mvn´,  the goal is to simply use maven in the end
else
  javac -d target/classes/ src/main/java/**/*.java
  echo "Compiled with javac"
fi

#copies the mothership script to the mothership's configuration folder
cp scripts/mothership.sh /tmp/pycore.*/NaveMae.conf
echo "mothership.sh copied to the mothership node"

#copies the ground control script to the ground control's configuration folder
cp scripts/groundcontrol.sh /tmp/pycore.*/GroundControl.conf
echo "groundcontrol.sh copied to the ground control node"

#copies the rover script to every configuration folder except the mothership's, the ground control's, and the satellites'
#which is to say, copies it to the rovers' configuration folders
for d in $(find /tmp/pycore.* -maxdepth 1 -type d -name "*.conf" \
              -not -name "NaveMae.conf" \
              -not -name "GroundControl.conf" \
              -not -name "Satelite*.conf"); do
    cp scripts/rover.sh "$d/"
done
echo "rover.sh copied to the rover nodes"