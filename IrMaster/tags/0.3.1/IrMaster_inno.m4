changecom(`', `')dnl
#define MyAppName "IrMaster"
#define MyAppVersion "VERSION"
#define MyAppPublisher "Bengt Martensson"
#define MyAppURL "http://www.harctoolbox.org/"
#define MyAppExeName "IrMaster.jar"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{AC1B3ACE-5FFD-472A-A379-D97CE9ED3DE9}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
LicenseFile=doc\LICENSE_gpl.txt
InfoBeforeFile=tools\pre_install.txt
InfoAfterFile=tools\post_install.txt
OutputBaseFilename={#MyAppName}-{#MyAppVersion}
Compression=lzma
SolidCompression=yes
OutputDir=.
ChangesEnvironment=true
PrivilegesRequired=none

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: modifypath; Description: &Add installation directory to path

[Files]
Source: "dist\IrMaster.jar"; DestDir: "{app}"; Flags: ignoreversion; AfterInstall: CreateWrapper
Source: "dist\lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "irps\*"; DestDir: "{app}\irps"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "decodeir\Windows-x86\*"; DestDir: "{app}\Windows-x86"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "decodeir\Windows-amd64\*"; DestDir: "{app}\Windows-amd64"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "IrpProtocols.ini"; DestDir: "{app}"; Flags: ignoreversion
Source: "doc\*.html"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "doc\*.pdf"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "doc\*.txt"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\IrpMaster\doc\IrpMaster.releasenotes.txt"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "doc\*.java"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "doc\images\*"; DestDir: "{app}\doc\images"
Source: "src\icons\crystal\64x64\apps\remote.ico";  DestDir: "{app}"
Source: "doc\IrMaster.html"; DestDir: "{app}\doc"; Flags: isreadme


[Icons]
Name: "{group}\{#MyAppName} Easy"; Filename: "{app}\{#MyAppExeName}"; Parameters: "--easy"; IconFilename: "{app}\remote.ico";
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\remote.ico";
Name: "{group}\HTML-Doc\IrMaster"; Filename: "{app}\doc\IrMaster.html"
Name: "{group}\HTML-Doc\IrpMaster"; Filename: "{app}\doc\IrpMaster.html"
Name: "{group}\HTML-Doc\Release Notes"; Filename: "{app}\doc\IrMaster.releasenotes.txt"
Name: "{group}\HTML-Doc\Release Notes IRPmaster"; Filename: "{app}\doc\IrpMaster.releasenotes.txt"
Name: "{group}\PDF Doc\IrMaster"; Filename: "{app}\doc\IrMaster.pdf"
Name: "{group}\PDF Doc\IrpMaster"; Filename: "{app}\doc\IrpMaster.pdf"
Name: "{group}\{cm:ProgramOnTheWeb,{#MyAppName}}"; Filename: "{#MyAppURL}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\remote.ico";
Name: "{commondesktop}\{#MyAppName} Easy"; Filename: "{app}\{#MyAppExeName}"; Parameters: "--easy"; Tasks: desktopicon; IconFilename: "{app}\remote.ico"
dnl Name: "{commonstartmenu}\{#MyAppName}"; Filename: "{group}"
dnl Name: "{commonstartmenu}\{#MyAppName} Ezy"; Filename: "{app}\{#MyAppExeName}";
dnl Name: "{userstartmenu}\{#MyAppName} Ewy"; Filename: "{app}\{#MyAppExeName}";
dnl Name: "{userstartup}\{#MyAppName} startup"; Filename: "{app}";
dnl Name: "{commonstartmenu}\{#MyAppName}ccc"; Filename: "{group}"
dnl Name: "{userappdata}\{#MyAppName}userappdata"; Filename: "{group}"
dnl Name: "{userfavorites}\{#MyAppName}userfavorites"; Filename: "{group}"
dnl Name: "{commonstartup}\{#MyAppName}zzz"; Filename: "{group}"
dnl Name: "{commontemplates}\{#MyAppName}templates"; Filename: "{group}"
dnl Name: "{localappdata}\{#MyAppName}localappdata"; Filename: "{group}"
dnl Name: "{userappdata}\{#MyAppName}userappdata"; Filename: "{group}"
dnl Name: "{userprograms}\{#MyAppName}userprograms"; Filename: "{group}"
dnl Name: "{userstartup}\{#MyAppName}userstartupp"; Filename: "{group}"
dnl Name: "{userstartmenu}\{#MyAppName}userstartmenu"; Filename: "{group}"
dnl Name: "{usertemplates}\{#MyAppName}usertemplatesss"; Filename: "{group}"

[UninstallDelete]
Type: files; Name: "{app}\irpmaster.bat"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, "&", "&&")}}"; Parameters: ; Flags: shellexec postinstall skipifsilent

[Code]
procedure CreateWrapper;
var
   wrapperFilename: String;
begin
   wrapperFilename := ExpandConstant('{app}') + '\IrpMaster.bat';
   SaveStringToFile(wrapperFilename, '@ECHO off' + #13#10, false);
   SaveStringToFile(wrapperFilename, 'set IRPMASTERHOME=' + ExpandConstant('{app}') + #13#10, true);
   SaveStringToFile(wrapperFilename, 'set JAVA=java' + #13#10, true);
   SaveStringToFile(wrapperFilename, '"%JAVA%" "-Djava.library.path=%IRPMASTERHOME%\Windows-x86" -jar "%IRPMASTERHOME%\IrMaster.jar" IrpMaster -c "%IRPMASTERHOME%\IrpProtocols.ini" %*', true);
end;

const
   ModPathName = 'modifypath';
   ModPathType = 'user';

function ModPathDir(): TArrayOfString;
begin
   setArrayLength(Result, 1);
   Result[0] := ExpandConstant('{app}');
 end;

#include "tools\modpath.iss"
