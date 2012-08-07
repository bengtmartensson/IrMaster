# One day I am going to use ant (or something...) for this.
# That day is not today...

APPLICATION=IrMaster

ANT=ant
MAKE=make
ZIP=zip
VERSION=$(shell sed -e "s/$(APPLICATION) version //" $(APPLICATION).version)
RM=rm -f
JAVA=java
INNO_COMPILER=c:\\Program Files\\Inno Setup 5\\ISCC.exe

JAVADOCROOT=/srv/www/htdocs/javadoc

SRC-DIST=$(APPLICATION)-src-$(VERSION).zip
BIN-DIST=$(APPLICATION)-bin-$(VERSION).zip

SRC-DIST-FILES=doc/IRPMasterAPIExample.java doc/$(APPLICATION).xml doc/$(APPLICATION).html doc/LICENSE_gpl.txt doc/LICENSE_makehex.txt doc/TODO doc/ANTLR3_license_bsd.txt doc/Makefile doc/images/* Makefile irmaster.sh src/org/harctoolbox/$(APPLICATION)/*.java
BIN-DIST-FILES=irmaster.sh doc/IRPMasterAPIExample.java doc/IrpMaster.html doc/$(APPLICATION).html doc/LICENSE_gpl.txt doc/LICENSE_makehex.txt doc/TODO doc/ANTLR3_license_bsd.txt doc/images/* IrpProtocols.ini irps/* 

.PHONY: doc clean

all: import ant $(APPLICATION).version doc src-dist bin-dist $(APPLICATION)_inno.iss run_inno.bat

ant:
	$(ANT)

$(APPLICATION).version: src/org/harctoolbox/IrMaster/Version.java | dist/$(APPLICATION).jar
	$(JAVA) -classpath dist/$(APPLICATION).jar org.harctoolbox.$(APPLICATION).Version

$(APPLICATION)_inno.iss: $(APPLICATION)_inno.m4 $(APPLICATION).version
	m4 --define=VERSION=$(VERSION) $< > $@

run_inno.bat: $(APPLICATION).version
	echo del $(APPLICATION)-$(VERSION).exe > $@
	echo \"$(INNO_COMPILER)\" $(APPLICATION)_inno.iss >> $@
	echo $(APPLICATION)-$(VERSION) >> $@

doc:
	$(MAKE) -C doc

src-dist: $(SRC-DIST)

$(SRC-DIST): $(SRC-DIST-FILES)
	-rm -f $@
	$(ZIP) $@ $(SRC-DIST-FILES)

bin-dist: $(BIN-DIST)

$(BIN-DIST): $(BIN-DIST-FILES)  dist/$(APPLICATION).jar
	-rm -f $@
	$(ZIP) $@ $(BIN-DIST-FILES)
	(cd dist; $(ZIP) ../$@ $(APPLICATION).jar lib/*)
	(cd decodeir; $(ZIP) ../$@ Linux-amd64/* Linux-i386/* Mac*/* Windows/*)

clean:
	$(RM) -r $(SRC-DIST) $(BIN-DIST) dist doc/$(APPLICATION).html doc/irpmaster.html doc/IRPMasterAPIExample.java IrpProtocols.ini $(APPLICATION)_inno.iss $(APPLICATION).properties.xml doc/*.pdf  $(APPLICATION)-$(VERSION).exe run_inno.bat

distclean:
	$(APPLICATION).version

import:
	cp -p ../IrpMaster/dist/IrpMaster.jar lib
	cp -p ../harctoolbox/dist/harctoolbox.jar lib
	rm -f IrpProtocols.ini
	cp -p ../IrpMaster/data/IrpProtocols.ini .
	cp -p ../IrpMaster/doc/IrpMaster.html doc
	cp -p ../IrpMaster/doc/IRPMasterAPIExample.java doc
	-cp -p ../www.harctoolbox.org/build/site/en/Ir*Master.pdf doc

install-javadoc: ant
	rm -rf $(JAVADOCROOT)/org/harctoolbox/$(APPLICATION)
	cp -a dist/javadoc $(JAVADOCROOT)/org/harctoolbox/$(APPLICATION)
