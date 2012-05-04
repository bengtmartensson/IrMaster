#!/bin/sh

VERSION=`sed -e "s/IrMaster version //" irmaster.version`
ZIPFILE=IrMaster-bin-$VERSION.zip

cd dist
rm -f $ZIPFILE
zip $ZIPFILE IrMaster.jar lib/*
mv $ZIPFILE ..
cd ..
zip $ZIPFILE IrpProtocols.ini irmaster.sh docs/*  Linux-amd64/* Linux-i386/* Mac/*/* Windows/* irps/*.irp
