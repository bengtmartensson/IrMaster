# One day I am going to use ant (or something...) for this.
# That day is not today...

ANT=ant
MAKE=make
ZIP=zip
VERSION=$(shell sed -e "s/IrMaster version //" irmaster.version)
RM=rm -f

SRC-DIST=IrMaster-src-$(VERSION).zip
BIN-DIST=IrMaster-bin-$(VERSION).zip

SRC-DIST-FILES=doc/IRPMasterAPIExample.java doc/irmaster.*ml doc/LICENSE_gpl.txt doc/LICENSE_makehex.txt doc/TODO doc/Makefile doc/images/* Makefile tools/document2html.mm irmaster.sh src/org/harctoolbox/IrMaster/*.java
BIN-DIST-FILES=irmaster.sh doc/IRPMasterAPIExample.java doc/irpmaster.html doc/irmaster.html doc/LICENSE_gpl.txt doc/LICENSE_makehex.txt  IrpProtocols.ini

.PHONY: doc clean

all: import ant doc src-dist bin-dist irmaster_inno.iss

ant:
	$(ANT)

irmaster_inno.iss: irmaster_inno.m4 irmaster.version
	m4 --define=VERSION=$(VERSION) $< > $@

doc:
	$(MAKE) -C doc

src-dist: $(SRC-DIST)

$(SRC-DIST): $(SRC-DIST-FILES)
	-rm -f $@
	$(ZIP) $@ $(SRC-DIST-FILES)

bin-dist: $(BIN-DIST)

$(BIN-DIST): $(BIN-DIST-FILES)
	-rm -f $@
	$(ZIP) $@ $(BIN-DIST-FILES)
	(cd dist; $(ZIP) ../$@ IrMaster.jar lib/*)
	(cd decodeir; $(ZIP) ../$@ Linux-amd64/* Linux-i386/* Mac*/* Windows/*)

clean:
	$(RM) -r $(SRC-DIST) $(BIN-DIST) dist doc/irmaster.html doc/irpmaster.html doc/IRPMasterAPIExample.java IrpProtocols.ini irmaster_inno.iss IrMaster.properties.xml doc/*.pdf

import:
	cp -p ../IrpMaster/dist/IrpMaster.jar lib
	cp -p ../harctoolbox/dist/harctoolbox.jar lib
	cp -p ../IrpMaster/data/IrpProtocols.ini .
	cp -p ../IrpMaster/doc/irpmaster.html doc
	cp -p ../IrpMaster/doc/IRPMasterAPIExample.java doc
	cp -p ../www.harctoolbox.org/build/site/en/ir*master.pdf doc
