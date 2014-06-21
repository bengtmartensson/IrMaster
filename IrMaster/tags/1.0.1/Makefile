APPLICATION=IrMaster
PACKAGE=IrMaster
include tools/env.mk

IMPORT_PROJS=DecodeIrCaller ExchangeIR IrpMaster Icons HarcHardware

INSTALLDIR=/usr/local/irmaster
BINDIR=/usr/local/bin

SRC_DIST_FILES=doc/$(APPLICATION).xml doc/LICENSE_gpl.txt doc/LICENSE_makehex.txt doc/ANTLR3_license_bsd.txt doc/images/* Makefile irmaster.sh exportformats_IrMaster.xml src/org/harctoolbox/$(APPLICATION)/*.java irps/* 

BIN_DIST_FILES=irmaster.sh doc/IrpMaster.html doc/$(APPLICATION).html doc/$(APPLICATION).pdf doc/LICENSE_gpl.txt doc/LICENSE_makehex.txt doc/ANTLR3_license_bsd.txt doc/*.releasenotes.txt doc/images/* IrpProtocols.ini exportformats_IrMaster.xml irps/* $(DOCUMENTATIONFILES)

DOCUMENTATIONFILES := doc/$(APPLICATION).html doc/IrpMaster.html doc/Glossary.html doc/IrpMaster.pdf doc/Glossary.pdf doc/IrpMaster.releasenotes.txt

include tools/makerules.mk

install: dist/$(APPLICATION).jar $(BIN_DIST_FILES) | $(INSTALLDIR)
	cp -pr dist/$(APPLICATION).jar dist/lib $(INSTALLDIR)
	tar cf - --dereference --exclude \.svn $(BIN_DIST_FILES) | (cd $(INSTALLDIR); tar xf -)
	tar cf - -C native --exclude \.svn . | (cd $(INSTALLDIR); tar xf -)
	sed -e "s:^IRSCRUTINIZERHOME=.*$$:IRSCRUTINIZERHOME=$(INSTALLDIR):" \
		-e "s:^JAVA=.*$$:JAVA=$(JAVA):" irmaster.sh \
			> $(INSTALLDIR)/irmaster.sh
	chmod +x $(INSTALLDIR)/irmaster.sh
	ln -sf $(INSTALLDIR)/irmaster.sh $(BINDIR)/irmaster

$(INSTALLDIR):
	mkdir -p $@

doc/IrpMaster.html: ../IrpMaster/doc/IrpMaster.xml $(TOOLS)/xdoc2html.xsl
	$(XALAN) -XSL $(TOOLS)/xdoc2html.xsl -IN $< -OUT $@

doc/Glossary.html: ../IrScrutinizer/doc/IrScrutinizer.xml $(TOOLS)/xdoc2html.xsl
	$(XALAN) -XSL $(TOOLS)/xdoc2html.xsl -IN $< -OUT $@

doc/IrpMaster.releasenotes.txt: ../IrpMaster/doc/IrpMaster.releasenotes.txt
	cp $< $@
