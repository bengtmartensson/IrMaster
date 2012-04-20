#!/bin/sh

# Wrapper for IrMaster for Unix-like systems, feel free to adapt to your needs.

# Preferred Java VM
JAVA=/opt/jdk1.6.0_30/bin/java

# Where IrMaster is installed
IRMASTERHOME=/usr/local/irmaster

# Use DecodeIR from the system of no private version is found
#LIBRARY_PATH=/usr/local/lib

# cd to the installation director to get the relative path names in
# the default properties to fit, can be omitted if making file names
# in the properties absolute.
cd ${IRMASTERHOME}

${JAVA} -Djava.library.path=${LIBRARY_PATH} -jar ${IRMASTERHOME}/IrMaster.jar -p ${HOME}/.irmaster.properties.xml "$@"
