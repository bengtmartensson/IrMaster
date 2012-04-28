MMX=/local/mm/bin/mmx
TOOLS=../tools

irmaster.html: irmaster.xml $(TOOLS)/document2html.mm
	$(MMX) -M $(TOOLS)/document2html.mm  -F xml -S $< -F sgml -b UTF-8 -T $@ -A title "IrMaster Documentation"
