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
XALAN=$(JAVA) -jar /usr/local/apache-forrest-0.9/lib/endorsed/xalan-2.7.1.jar

JAVADOCROOT=/srv/www/htdocs/javadoc

SRC-DIST=$(APPLICATION)-src-$(VERSION).zip
BIN-DIST=$(APPLICATION)-bin-$(VERSION).zip

SRC-DIST-FILES=doc/IRPMasterAPIExample.java doc/$(APPLICATION).xml doc/$(APPLICATION).html doc/LICENSE_gpl.txt doc/LICENSE_makehex.txt doc/ANTLR3_license_bsd.txt doc/Makefile doc/images/* Makefile irmaster.sh src/org/harctoolbox/$(APPLICATION)/*.java
BIN-DIST-FILES=irmaster.sh doc/IRPMasterAPIExample.java doc/IrpMaster.html doc/$(APPLICATION).html doc/LICENSE_gpl.txt doc/LICENSE_makehex.txt doc/ANTLR3_license_bsd.txt doc/*.releasenotes.txt doc/images/* IrpProtocols.ini exportformats_IrMaster.xml irps/* 

.PHONY: doc clean

all: import ant $(APPLICATION).version doc src-dist bin-dist $(APPLICATION)_inno.iss run_inno.bat

ant:
	$(ANT)

#$(APPLICATION).version: src/org/harctoolbox/IrMaster/Version.java | dist/$(APPLICATION).jar
#	$(JAVA) -classpath dist/$(APPLICATION).jar org.harctoolbox.$(APPLICATION).Version

$(APPLICATION).version: programdata/org/harctoolbox/IrMaster/Version.xml
	$(XALAN) -XSL tools/mkVersionFile.xsl -IN $< -OUT $@

$(APPLICATION)_inno.iss: $(APPLICATION)_inno.m4 $(APPLICATION).version dist
	m4 --define=VERSION=$(VERSION) $< > $@

run_inno.bat: $(APPLICATION).version
	echo del $(APPLICATION)-$(VERSION).exe > $@
	echo \"$(INNO_COMPILER)\" $(APPLICATION)_inno.iss >> $@
	echo $(APPLICATION)-$(VERSION) >> $@
	unix2dos $@

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
	(cd decodeir; $(ZIP) ../$@ Linux-*/* Mac*/* Windows*/*)

clean:
	$(RM) -r $(SRC-DIST) $(BIN-DIST) dist doc/$(APPLICATION).html doc/irpmaster.html doc/IRPMasterAPIExample.java IrpProtocols.ini $(APPLICATION)_inno.iss $(APPLICATION).properties.xml doc/*.pdf doc/IrpMaster.releasenotes.txt $(APPLICATION)-$(VERSION).exe run_inno.bat

distclean: clean
	$(RM) $(APPLICATION).version

import:
	cp -p ../IrpMaster/dist/IrpMaster.jar lib
	cp -p ../IrCalc/dist/IrCalc.jar lib
	cp -p ../HarcHardware/dist/HarcHardware.jar lib
	rm -f IrpProtocols.ini
	cp -p ../IrpMaster/data/IrpProtocols.ini .
#	cp -p ../IrpMaster/doc/IrpMaster.html doc
#	cp -p ../IrpMaster/doc/IrpMaster.releasenotes.txt doc
	cp -p ../IrpMaster/doc/IRPMasterAPIExample.java doc
	-cp -p ../www.harctoolbox.org/build/site/en/Ir*Master.pdf doc

install-javadoc: ant
	rm -rf $(JAVADOCROOT)/org/harctoolbox/$(APPLICATION)
	cp -a dist/javadoc $(JAVADOCROOT)/org/harctoolbox/$(APPLICATION)
