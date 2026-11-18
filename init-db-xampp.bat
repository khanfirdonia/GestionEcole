@echo off
echo === Initialisation de la base de donnees avec XAMPP ===
echo.

set "XAMPP_MYSQL=C:\xampp\mysql\bin\mysql.exe"

if not exist "%XAMPP_MYSQL%" (
    echo MySQL de XAMPP non trouve dans C:\xampp\mysql\bin\
    echo Verifiez le chemin d'installation de XAMPP.
    pause
    exit /b 1
)

echo Connexion a MySQL via XAMPP...
echo.

"%XAMPP_MYSQL%" -u root -e "SHOW DATABASES;" 2>nul
if errorlevel 1 (
    echo ERREUR: Impossible de se connecter a MySQL.
    echo Verifiez que MySQL est demarre dans XAMPP Control Panel.
    pause
    exit /b 1
)

echo.
echo Initialisation de la base de donnees db_school_management...
"%XAMPP_MYSQL%" -u root < "%~dp0database_init.sql" 2>nul
if errorlevel 1 (
    echo ERREUR lors de l'initialisation.
    echo Essayez avec le mot de passe root...
    set /p "password=Mot de passe root (laisser vide si aucun): "
    "%XAMPP_MYSQL%" -u root -p%password% < "%~dp0database_init.sql"
    if errorlevel 1 (
        echo Echec. Importez manuellement database_init.sql via phpMyAdmin.
        pause
        exit /b 1
    )
)

echo.
echo === Base de donnees initialisee avec succes! ===
echo.
echo Verification des tables...
"%XAMPP_MYSQL%" -u root -e "USE db_school_management; SHOW TABLES;"

pause
