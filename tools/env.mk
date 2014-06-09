PROJECT_PREFIX=org/harctoolbox
WWW_DIR := $(HOME)/harc/www.harctoolbox.org
PDF_DIR := $(WWW_DIR)/build/site/en
DISTDIR := $(WWW_DIR)/src/content/xdocs/downloads
JAVADOC_INSTALLDIR := /srv/www/htdocs/javadoc/$(PROJECT_PREFIX)
ANT := ant
#MAKE := make
ZIP := zip -9 -r -x \*~ \*\*/.svn\*\* @ 
RM := rm -f
TAR := tar
export JAVA_HOME := /opt/jdk1.7.0_45
export JAVA := $(JAVA_HOME)/bin/java
XALAN := $(JAVA) -jar /usr/local/apache-forrest-0.9/lib/endorsed/xalan-2.7.1.jar
INNO_COMPILER=c:\\Program Files\\Inno Setup 5\\ISCC.exe

TOOLS := tools
VERSION_XML = programdata/$(PROJECT_PREFIX)/$(PACKAGE)/Version.xml
