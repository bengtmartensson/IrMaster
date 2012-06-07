JAVA=/opt/jdk1.6.0_30/bin/java
XALAN=$(JAVA) -jar /usr/local/apache-forrest-0.9/lib/endorsed/xalan-2.7.1.jar
APPLICATION=IrMaster
MMX=/local/mm/bin/mmx
TOOLS=../../IrpMaster/tools
BROWSER=firefox

#$(APPLICATION).html: $(APPLICATION).xml $(TOOLS)/document2html.mm
#	$(MMX) -M $(TOOLS)/document2html.mm  -F xml -S $< -F sgml -b UTF-8 -T $@ -A title "$(APPLICATION) Documentation"

$(APPLICATION).html: $(APPLICATION).xml $(TOOLS)/xdoc2html.xsl
	$(XALAN) -XSL $(TOOLS)/xdoc2html.xsl -IN $< -OUT $@

preview: $(APPLICATION).html
	$(BROWSER) $<

clean:
	rm $(APPLICATION).html
