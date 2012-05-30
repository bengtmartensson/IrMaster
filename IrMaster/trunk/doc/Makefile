APPLICATION=IrMaster
MMX=/local/mm/bin/mmx
TOOLS=../../IrpMaster/tools
BROWSER=firefox

$(APPLICATION).html: $(APPLICATION).xml $(TOOLS)/document2html.mm
	$(MMX) -M $(TOOLS)/document2html.mm  -F xml -S $< -F sgml -b UTF-8 -T $@ -A title "$(APPLICATION) Documentation"

preview: $(APPLICATION).html
	$(BROWSER) $<
