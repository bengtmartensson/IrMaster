PROJECT_PREFIX=org/harctoolbox
WWW_DIR := $(HOME)/harctoolbox/www
DISTDIR := $(WWW_DIR)/src/content/xdocs/downloads
JAVADOC_INSTALLDIR := /srv/www/htdocs/javadoc/$(PROJECT_PREFIX)
ANT := ant
#MAKE := make
ZIP := zip -x \*~ @ 
RM := rm -f
export JAVA_HOME := /opt/jdk1.7.0_45
export JAVA := $(JAVA_HOME)/bin/java
XALAN := $(JAVA) -jar /usr/local/apache-forrest-0.9/lib/endorsed/xalan-2.7.1.jar
INNO_COMPILER=c:\\Program Files\\Inno Setup 5\\ISCC.exe

TOOLS := tools
VERSION_XML = programdata/$(PROJECT_PREFIX)/$(APPLICATION)/Version.xml
