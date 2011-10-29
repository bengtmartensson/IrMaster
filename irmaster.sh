#!/bin/sh

# Wrapper for IrMaster for Unix-like systems, feel free to adapt to your needs.

# Preferred Java VM
JAVA=/opt/jdk1.6.0_25/bin/java

# Where IrMaster is installed
IRMASTERHOME=/usr/local/irmaster

# cd to the installation director to get the relative path names in
# the default properties to fit, can be omitted if making file names
# in the properties absolute.
cd ${IRMASTERHOME}

${JAVA} -jar ${IRMASTERHOME}/IrMaster.jar -p ${HOME}/.irmaster.properties.xml "$@"
