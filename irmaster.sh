#!/bin/sh

# This wrapper is used to start both IrMaster and IrpMaster,
# depending on what name it is called.

# Intended for Unix-like systems, may need
# to be locally adapted.

# Preferred Java VM
JAVA=java
#JAVA=/opt/jdk1.6.0_30/bin/java

# Where IrMaster is installed
IRMASTERHOME=/usr/local/irmaster

# Path to DecodeIR
# If the code below does not work, just set LIBRARY_PATH to the directory
# containing the shared lib to use, like in the commented-out example lines.
if [ `uname -m` = "x86_64" ] ; then
    ARCH=amd64
else
    ARCH=i386
fi
LIBRARY_PATH=${IRMASTERHOME}/`uname -s`-${ARCH}
#LIBRARY_PATH=/usr/local/irmaster/Linux-amd64
#LIBRARY_PATH=/usr/local/lib

if [ `basename $0` = "irpmaster" ] ; then
    # Run IrpMaster from the current directory
    ${JAVA} -Djava.library.path=${LIBRARY_PATH} -jar ${IRMASTERHOME}/IrMaster.jar IrpMaster --config ${IRMASTERHOME}/IrpProtocols.ini "$@"

else
    # cd to the installation director to get the relative path names in
    # the default properties to fit, can be omitted if making file names
    # in the properties absolute.

    cd ${IRMASTERHOME}
    ${JAVA} -Djava.library.path=${LIBRARY_PATH} -jar ${IRMASTERHOME}/IrMaster.jar -p ${HOME}/.IrMaster.properties.xml "$@"

fi
