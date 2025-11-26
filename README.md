# Comunicações por Computador - Projeto 25/26

A Java program that simulates the communication between a mothership, one or more simultaneous rovers performing missions on Mars, and a ground control entity.

---
## Recommended Software:
 - Oracle VirtualBox
 - Virtual Machine with ubuntu and core #(are we supposed to give the actual one we have ?)
 - Developed and tested on Java 21. Compatibility with earlier versions is not guaranteed.
 - Maven is recommended, but manual javac compilation instructions are provided.

## Setting up the Virtual Machine (VM)
You may either keep the project files in a shared folder accessible from your VM, or clone the repository directly inside the VM.

#### Using a Shared Folder

1. Open VirtualBox → Settings → Shared Folders

2. Add the folder that contains the project

3. Enable Auto-mount

Inside the VM, grant your user access to shared folders by using:

`sudo usermod -aG vboxsf $USER`

Log out and back in for the change to take effect.

#### Installing the recommended software

You can install Java 21 and Maven manually, or use the provided configuration script:
`./scripts/vmconfig.sh`

This script will automatically install the required packages.

## Available Scripts:

> Note: only `setup.sh` must be run as `./scripts/setup.sh`, `vmconfig.sh` may be used inside or outside scripts/ using the appropriate execution command.
---

`vmconfig.sh`  
Installs Java 21 and Maven inside the VM  
#### Usage:
```
./scripts/vmconfig.sh
```
---
`setup.sh`  
Compiles program files using maven or javac, and copies the remaining scripts to their proper directories in core.  
#### Usage options:

``` bash
./scripts/setup.sh #to compile with javac
./scripts/setup.sh mvn #to compile with maven
```
---
`mothership.sh`  
Launches the mothership process.  
#### Usage options:
``` bash
./scripts/mothership.sh #if the repo is inside the VM
./scripts/mothership.sh sf #if the repo is a shared folder
```
---
`rover.sh`  
Launches a rover instance with ID = [ID], where [ID] is a natural number.
#### Usage options:
``` bash
./scripts/rover.sh [ID] #if the repo is inside the VM
./scripts/rover.sh [ID] sf #if the repo is a shared folder
```
---
`groundcontrol.sh`  
Launches the ground control process.
#### Usage options:
``` bash
./scripts/groundcontrol.sh #if the repo is inside the VM
./scripts/groundcontrol.sh sf #if the repo is a shared folder
```
---

## Running the System (Typical Workflow)
> These steps assume the VM is configured to your preference, and running.
1. Start core.
2. Load the provived **Topology.xml** file.
3. Start the session.
4. Open a VM terminal (not on core!), navigate to your project folder, and run `./scripts/setup.sh`.
5. Open a terminal in the Mothership node, and run `./mothership.sh`.
6. Open a terminal in a Rover node, and run `./rover.sh [ID]`.
7. Repeat step 6 for as many rovers as desired.
8. Open a terminal in the Ground Control node, and run `./groundcontrol.sh`.

> Note: if any rovers are added to the topology, `setup.sh` must be run again to ensure the new rovers have the `rover.sh` script available.
