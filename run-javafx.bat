@echo off
chcp 65001 >nul
setlocal

set "PROJECT_DIR=%~dp0"
set "OUT_DIR=%PROJECT_DIR%out_run"
if not defined JAVAFX_LIB set "JAVAFX_LIB=C:\javafx-sdk-21.0.10\lib"
set "MYSQL_JAR=%PROJECT_DIR%lib\mysql-connector-j-9.6.0.jar"
set "TARGET_RELEASE=21"

rem Les identifiants locaux sont transmis par l'environnement, sans secret dans ce fichier.
if not defined DB_HOST set "DB_HOST=localhost"
if not defined DB_PORT set "DB_PORT=3306"
if not defined DB_NAME set "DB_NAME=db_school_management"
if not defined DB_USER set "DB_USER=root"

rem Priorite au JDK d'Eclipse si defini
if defined ECLIPSE_JAVA_HOME (
  set "JAVA_CMD=%ECLIPSE_JAVA_HOME%\bin\java.exe"
  set "JAVAC_CMD=%ECLIPSE_JAVA_HOME%\bin\javac.exe"
) else if defined JAVA_HOME (
  set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
  set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
) else (
  set "JAVA_CMD=java"
  set "JAVAC_CMD=javac"
)

if not exist "%JAVA_CMD%" if not "%JAVA_CMD%"=="java" (
  echo Java introuvable: "%JAVA_CMD%".
  echo Definis ECLIPSE_JAVA_HOME ou JAVA_HOME correctement.
  exit /b 1
)

if not exist "%JAVAC_CMD%" if not "%JAVAC_CMD%"=="javac" (
  echo Javac introuvable: "%JAVAC_CMD%".
  echo Definis ECLIPSE_JAVA_HOME ou JAVA_HOME correctement.
  exit /b 1
)

if not exist "%JAVAFX_LIB%\javafx.controls.jar" (
    echo JavaFX introuvable dans "%JAVAFX_LIB%".
    echo Verifie le chemin du SDK JavaFX dans run-javafx.bat.
    exit /b 1
)

if not exist "%MYSQL_JAR%" (
    echo Driver MySQL introuvable: "%MYSQL_JAR%".
    exit /b 1
)

echo Nettoyage complet...
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
if exist "%PROJECT_DIR%out" rmdir /s /q "%PROJECT_DIR%out"
if exist "%PROJECT_DIR%out_check" rmdir /s /q "%PROJECT_DIR%out_check"
del /q "%PROJECT_DIR%*.class" 2>nul
for /r "%PROJECT_DIR%bin" %%F in (*.class) do del /q "%%F" 2>nul
mkdir "%OUT_DIR%"

echo Version Java utilisee:
"%JAVA_CMD%" -version
echo.
for /f "tokens=2" %%v in ('"%JAVAC_CMD%" --version 2^>^&1') do set "JAVAC_VER=%%v"
for /f "tokens=1 delims=." %%m in ("%JAVAC_VER%") do set "JAVAC_MAJOR=%%m"

if "%JAVAC_MAJOR%"=="" (
  echo Impossible de determiner la version de javac.
  exit /b 1
)

if %JAVAC_MAJOR% LSS %TARGET_RELEASE% (
  echo Erreur: javac %JAVAC_VER% detecte, mais Java %TARGET_RELEASE%+ est requis pour generer la version 65.
  echo Definis ECLIPSE_JAVA_HOME vers un JDK 21 ^(ou plus recent^) puis relance.
  exit /b 1
)

echo Compilation forcee en Java %TARGET_RELEASE% ^(class file version 65.0^) ...
"%JAVAC_CMD%" --release %TARGET_RELEASE% --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml ^
  -cp "%PROJECT_DIR%src;%PROJECT_DIR%resources;%MYSQL_JAR%" ^
  -d "%OUT_DIR%" ^
  "%PROJECT_DIR%src\application\Main.java" ^
  "%PROJECT_DIR%src\controllers\*.java" ^
  "%PROJECT_DIR%src\dao\*.java" ^
  "%PROJECT_DIR%src\models\*.java" ^
  "%PROJECT_DIR%src\services\*.java" ^
  "%PROJECT_DIR%src\utils\*.java"

if errorlevel 1 (
    echo Echec de compilation.
    exit /b 1
)

echo Copie des ressources...
xcopy /e /i /y "%PROJECT_DIR%resources\*" "%OUT_DIR%\" >nul
xcopy /e /i /y "%PROJECT_DIR%src\views" "%OUT_DIR%\views" >nul

echo Synchronisation des classes vers bin...
xcopy /e /i /y "%OUT_DIR%\*" "%PROJECT_DIR%bin\" >nul

echo Lancement de application.Main...
"%JAVA_CMD%" --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml ^
  -Dfile.encoding=UTF-8 ^
  -Dconsole.encoding=UTF-8 ^
  "-Djava.util.logging.config.file=%PROJECT_DIR%logging.properties" ^
  -cp "%OUT_DIR%;%MYSQL_JAR%" application.Main

endlocal
