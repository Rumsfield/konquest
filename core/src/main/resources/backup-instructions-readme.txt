This document explains how backups work, and how to restore your data from a backup.

## What are backups?
Konquest keeps track of kingdoms, land claims, and other player information with the files found inside the "data"
folder. Every time the server starts, Konquest will create a new backup of your data by archiving the current data
folder into a ZIP file. Konquest will delete older backups automatically, as specified in core.yml settings. By default,
Konquest will keep 10 backup archives.

## Why do I need backups?
If everything goes well, you don't need to worry about backups. You'll only need to use them if your server becomes
corrupted for some reason. The following situations may require the use of a backup archive to restore your data:
    - When using SQLite (default), all player data accidentally gets deleted.
    - All kingdoms and claimed territory on the server disappear for an unknown reason.
    - You need to revert the state of all kingdoms and players to a previous backup.

## Backup contents
The data folder contains the following files:
    camps.yml               - All barbarian camp locations, claimed land, clan information.
    kingdoms.yml            - All kingdom and town details.
    KonquestDatabase.db     - Saved player database when using SQLite (default).
    ruins.yml               - All ruin locations and claimed land, plus critical and spawn locations.
    sanctuaries.yml         - All sanctuary land info, plus monument templates.

## How to restore a backup
The backup ZIP files located at this level are copies of the data folder, named like "backup-data-(DATE).zip". The date
in each ZIP file name is the date that backup was created upon a server start. Choose your desired backup and follow
these steps.

Step 1:
    Make sure the server is stopped, or stop the server now.

Step 2:
    Get rid of the current data folder in this level (plugins/Konquest/data/).
    You can delete it, rename it, or move it to another folder.

Step 3:
    Choose your desired backup ZIP file and extract it.
    You can do this in Windows by right-clicking on the backup ZIP file and choose "Extract All...".
    In the "Extract Compressed (Zipped) Folders" menu, click the Extract button.

Step 4:
    Make sure that a new "data" folder exists with the file contents shown in the above section.

Step 5:
    Start the server and watch the console for any error messages.
    Join the game and confirm that the Konquest data is correct.
