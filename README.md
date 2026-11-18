# GestionEcoleE — Gestion scolaire

Application de bureau JavaFX avec une base MySQL pour la gestion d'une école.

## Fonctionnalités

- Authentification et tableaux de bord administrateur, enseignant et élève.
- Gestion des élèves, des enseignants, des classes et des niveaux.
- Préinscriptions, inscriptions annuelles et listes d'attente.
- Affectation des enseignants aux classes et aux matières.
- Saisie des notes, consultation et génération des bulletins.
- Journalisation des actions dans la console et dans la base.

## Prérequis

- Windows pour les scripts `.bat` fournis.
- JDK 21 ou supérieur ; le script compile avec `--release 21`.
- JavaFX SDK **21.0.10**, adapté à votre système et à son architecture.
- MySQL 8.0 ou un serveur compatible avec le script `database_init.sql`.
- MySQL Connector/J **9.6.0**, à installer selon [lib/README.md](lib/README.md).

Le projet utilise les dossiers `src/` et `resources/`, sans Maven ni Gradle.
Les SDK, les dépendances binaires installées et les classes compilées sont exclus de Git.

## Installation et configuration

1. Installer le JDK et définir `JAVA_HOME`, ou rendre `java` et `javac` accessibles
   dans le `PATH`. `ECLIPSE_JAVA_HOME`, si défini, a priorité sur `JAVA_HOME`.
2. Extraire le SDK JavaFX. Le chemin par défaut est
   `C:\javafx-sdk-21.0.10\lib` ; la variable `JAVAFX_LIB` permet de le remplacer.
3. Placer `mysql-connector-j-9.6.0.jar` dans `lib/`.
4. Démarrer MySQL et réserver une base locale de démonstration à cette application.
5. Depuis la racine du projet, importer le schéma et les données de démonstration :

   ```powershell
   Get-Content -Raw -Encoding UTF8 .\database_init.sql | mysql --default-character-set=utf8mb4 -u root -p
   ```

   Le script crée et utilise la base **`db_school_management`**. Pour XAMPP installé
   dans `C:\xampp`, `init-db-xampp.bat` fournit également une initialisation locale.
   Ces scripts SQL créent des comptes et des données de démonstration.

La connexion lit les propriétés Java `-DDB_*`, puis les variables d'environnement,
puis les valeurs par défaut :

| Variable | Valeur par défaut | Usage |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Serveur MySQL |
| `DB_PORT` | `3306` | Port MySQL |
| `DB_NAME` | `db_school_management` | Base utilisée par l'application |
| `DB_USER` | `root` | Utilisateur MySQL local |
| `DB_PASSWORD` | vide | Mot de passe MySQL local |

Définir les valeurs propres à votre poste dans le terminal ou dans la configuration
locale de l'IDE. Ne pas inscrire de véritables identifiants dans le code ou les scripts.
[.env.example](.env.example) décrit les variables sans secret ; l'application
**ne charge pas automatiquement les fichiers `.env`**.

Exemple PowerShell pour une base locale sans mot de passe :

```powershell
$env:DB_HOST = 'localhost'
$env:DB_PORT = '3306'
$env:DB_NAME = 'db_school_management'
$env:DB_USER = 'root'
$env:DB_PASSWORD = ''
$env:JAVAFX_LIB = 'C:\javafx-sdk-21.0.10\lib'
.\run-javafx.bat
```

Si un mot de passe est nécessaire, le définir localement dans `DB_PASSWORD` avant
le lancement. Le script conserve les valeurs de l'environnement.
Si le nom de base est personnalisé, adapter l'import SQL local à ce nom.

## Compilation et lancement

Depuis le dossier du projet :

```powershell
.\run-javafx.bat
```

Le script compile les sources avec les modules `javafx.controls` et `javafx.fxml`,
copie les ressources dans `out_run/`, synchronise `bin/`, puis lance
`application.Main`. Il nettoie les anciennes sorties de compilation.
Les vues de `src/views/` sont copiées après celles de `resources/views/` et ont
priorité lorsqu'un fichier porte le même nom. Les deux dossiers sont conservés.

Dans un IDE, configurer les dossiers sources `src/` et `resources/`, les mêmes
modules JavaFX et le pilote JDBC, puis exécuter `application.Main` depuis la racine
du projet afin que `database_init.sql` soit accessible.

## Données de démonstration

`database_init.sql` contient explicitement des données de démonstration et ces
comptes publics destinés aux essais locaux :

| Rôle | Utilisateur | Mot de passe de démonstration |
| --- | --- | --- |
| Administrateur | `admin` | `admin123` |
| Enseignant | `teacher01` | `pass123` |
| Élève | `student01` | `pass123` |

D'autres comptes fictifs sont créés pour la liste d'attente. Au premier accès,
`DatabaseConnection` peut initialiser une base absente, ajouter des données de
démonstration et normaliser des classes ou des affectations existantes. Utiliser
une base dédiée aux essais. L'authentification actuelle compare directement les
mots de passe stockés ; la présence de `PasswordUtils` ne signifie pas que ce
hachage est appliqué par `UserDAO`.

## Organisation

```text
src/application/    Point d'entrée JavaFX
src/controllers/    Contrôleurs des écrans
src/dao/            Accès MySQL et initialisation
src/models/         Modèles métier
src/services/       Logique métier
src/utils/          Utilitaires et gestion du thème
src/views/          Vues FXML prioritaires au lancement par script
resources/         Feuilles de style et vues FXML
lib/README.md      Version et installation du pilote JDBC
.env.example       Exemple public de configuration
logging.properties Configuration de la journalisation
database_init.sql  Schéma et données de démonstration
diagramme_classe_uml.puml Diagramme UML PlantUML
```

Les paramètres personnels des IDE, fichiers `.env` réels, clés privées,
dépendances installées, caches et sorties de compilation sont exclus par
[.gitignore](.gitignore).
