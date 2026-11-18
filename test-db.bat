@echo off
echo === Test de connexion MySQL ===
echo.
echo Assurez-vous que MySQL est en cours d'execution...
echo.

echo Test 1: Verifier si MySQL est accessible...
mysql --version 2>nul
if errorlevel 1 (
    echo MySQL client non trouve, verification du service...
    sc query MySQL 2>nul | find "RUNNING" >nul
    if errorlevel 1 (
        sc query MySQL80 2>nul | find "RUNNING" >nul
        if errorlevel 1 (
            echo ERREUR: MySQL ne semble pas etre en cours d'execution!
            echo Lancez MySQL avant de lancer l'application.
            goto :end
        ) else (
            echo Service MySQL80 trouve.
        )
    ) else (
        echo Service MySQL trouve.
    )
) else (
    echo MySQL client trouve.
)

echo.
echo Test 2: Connexion a MySQL...
mysql -u root -e "SHOW DATABASES;" 2>nul
if errorlevel 1 (
    echo ERREUR: Impossible de se connecter a MySQL avec root (sans mot de passe).
    echo Verifiez vos identifiants MySQL.
    goto :end
)

echo.
echo Test 3: Base de donnees db_school_management...
mysql -u root -e "USE db_school_management; SHOW TABLES;" 2>nul
if errorlevel 1 (
    echo ERREUR: La base de donnees 'db_school_management' n'existe pas ou est inaccessible.
    echo.
    echo Pour l'initialiser, executez:
    echo mysql -u root ^< database_init.sql
    goto :end
)

echo.
echo === Tout est OK! ===

:end
pause
