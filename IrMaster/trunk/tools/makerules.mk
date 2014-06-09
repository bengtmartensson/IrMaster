include version.mk

.PHONY: docu clean veryclean all ant src-dist bin-dist version

all: import $(APPLICATION).version dist/$(APPLICATION).jar docu src-dist bin-dist

version.mk: $(VERSION_XML)
	$(XALAN) -XSL $(TOOLS)/mkVersionMkFile.xsl -IN $< -OUT $@

version: $(APPLICATION).version

$(APPLICATION).version: $(VERSION_XML)
	$(XALAN) -XSL $(TOOLS)/mkVersionFile.xsl -IN $< -OUT $@

SRC-DIST = $(APPLICATION)-src-$(VERSION).zip
ifeq ($(BIN_DIST_FILES),)
BIN-DIST=
else
BIN-DIST = $(APPLICATION)-bin-$(VERSION).zip
endif

ant dist/$(APPLICATION).jar: import
	$(ANT)

import: $(foreach proj,$(IMPORT_PROJS),lib/$(proj).jar)

define template =
lib/$(1).jar: ../$(1)/dist/$(1).jar
	cp $$< $$@
endef

$(foreach proj,$(IMPORT_PROJS),$(eval $(call template,$(proj))))

ifeq ($(wildcard doc),)
docu:
	@echo No documentation in $(APPLICATION)
else
docu: doc/$(APPLICATION).html

doc/$(APPLICATION).html: doc/$(APPLICATION).xml $(TOOLS)/xdoc2html.xsl
	$(XALAN) -XSL $(TOOLS)/xdoc2html.xsl -IN $< -OUT $@

doc/%.pdf: $(WWW_DIR)/build/site/en/%.pdf
	cp $< $@
endif

src-dist: $(SRC-DIST)

#$(APPLICATION).version: ant
#	$(JAVA) -classpath dist/$(APPLICATION).jar org.harctoolbox.$(APPLICATION).Version

$(SRC-DIST): $(SRC-DIST-FILES)
	$(RM) $@
	$(ZIP) $@ $(SRC-DIST-FILES)

ifneq ($(BIN_DIST_FILES),)
bin-dist: $(BIN-DIST)

$(BIN-DIST): $(BIN_DIST_FILES)
	-rm -f $@
	$(ZIP) $@ $(BIN_DIST_FILES)
#	(cd data; $(ZIP) ../$@ IrpProtocols.ini )
#	(cd dist; $(ZIP) ../$@ $(APPLICATION).jar lib/*)
else
bin-dist:
	@echo No bin-dist exists for $(APPLICATION)
endif

export: $(SRC-DIST) $(BIN-DIST)
	cp $^ $(DISTDIR)

clean:
	$(RM) -r $(SRC-DIST) $(BIN-DIST) dist doc/$(APPLICATION).html

veryclean: clean
	$(RM) $(APPLICATION).version version.mk

install-javadoc: ant
	$(RM) -r $(JAVADOC_INSTALLDIR)/$(APPLICATION)
	cp -a dist/javadoc $(JAVADOC_INSTALLDIR)/$(APPLICATION)
