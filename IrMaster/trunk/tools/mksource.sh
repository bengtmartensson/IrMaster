#!/bin/sh

VERSION=`sed -e "s/IrMaster version //" irmaster.version`
ZIPFILE=IrMaster-src-$VERSION.zip

rm -f $ZIPFILE
zip $ZIPFILE docs/* irmaster.sh src/org/harctoolbox/IrMaster/*.java src/org/harctoolbox/IrMaster/*.form nbproject/* build.xml manifest.mf irmaster.sh
