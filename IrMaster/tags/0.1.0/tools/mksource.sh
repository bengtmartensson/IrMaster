#!/bin/sh

VERSION=0.1.0
ZIPFILE=IrMaster-src-$VERSION.zip

rm -f $ZIPFILE
zip $ZIPFILE docs/* src/IrMaster/*.java src/IrMaster/*.form nbproject/*  build.xml manifest.mf irmaster.sh
