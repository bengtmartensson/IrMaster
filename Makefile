# One day I am going to use ant (or something...) for this.
# That day is not today...

ANT=ant
MAKE=make
ZIP=zip
VERSION=$(shell sed -e "s/IrMaster version //" irmaster.version)
RM=rm -f

SRC-DIST=IrMaster-src-$(VERSION).zip
BIN-DIST=IrMaster-bin-$(VERSION).zip

SRC-DIST-FILES=doc/IRPMasterAPIExample.java doc/irpmaster.*ml doc/LICENSE doc/Makefile Makefile tools/document2html.mm test/test.sh test/test.bat irpmaster.sh grammar/org/harctoolbox/IrpMaster/Irp.g src/org/harctoolbox/IrpMaster/*.java
BIN-DIST-FILES=irpmaster.sh doc/IRPMasterAPIExample.java doc/irpmaster.html doc/LICENSE test/test.sh test/test.bat

.PHONY: doc clean

all: ant doc src-dist bin-dist

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
	(cd data; $(ZIP) ../$@ IrpProtocols.ini)
	(cd dist; $(ZIP) ../$@ IrpMaster.jar lib/*)

clean:
	$(RM) -r $(SRC-DIST) $(BIN-DIST) dist doc/irmaster.html doc/irpmaster.html doc/IRPMasterAPIExample.java IrpProtocols.ini irmaster_inno.iss IrMaster.properties.xml

import:
	cp -p ../IrpMaster/dist/IrpMaster.jar lib
	cp -p ../harctoolbox/dist/harctoolbox.jar lib
	cp -p ../IrpMaster/data/IrpProtocols.ini .
	cp -p ../IrpMaster/doc/irpmaster.html doc
	cp -p ../IrpMaster/doc/IRPMasterAPIExample.java doc
