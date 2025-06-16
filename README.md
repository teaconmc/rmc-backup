# rmc-backup

Backup Minecraft saves with the awesome [restic](https://github.com/restic/restic) backup tool.

Features:

- Schedule back up with fixed interval
- Skip backups when no players online
- Incremental backup & deduplication thanks to restic
- Backup to local/s3/gcloud/rclone/... thanks to restic
- Restore easily with one command

## Usage

***Warning: YOU NEED TO BACK UP THE REPOSITORY PASSWORD IN THE CONFIGURATION FILE, OR YOU WILL LOSE
ACCESS TO ALL YOUR BACKUPS.***

### Command

#### `/rmc schedule`

Schedule a backup immediately.

#### `/rmc setInterval <interval>`

Modify the backup interval.

#### `/rmc snapshots`

List all backups, click on the result to generate corresponding restore command.

#### `/rmc restore <snapshot>`

Restore to the specified backup and stop the server.

### Configuration

Config file is located at `$SERVER_FOLDER/config/rmc-common.toml`.

#### `useBundledRestic`

- **Type**: Boolean
- **Default**: True if bundled executable is available
- **Description**: Whether to use bundled restic executable.

#### `resticExecutablePath`

- **Type**: String
- **Default**: `"restic"`
- **Description**: Path to the restic executable. Can be a full path or just the executable name if
  it's in the system PATH.
- **Example**: `"/usr/local/bin/restic"`

#### `repositoryPath`

- **Type**: String
- **Default**: `"rmc-backup"`
- **Description**: Path to the restic repository. Can be a local path or an S3-compatible storage
  path.
- **Example**: `"s3:s3.amazonaws.com/bucket-name"` for S3 storage

#### `repositoryPassword`

- **Type**: String
- **Default**: Randomly generated UUID
- **Description**: Password for the restic repository. If left as default, a new random password
  will be generated on first run.

#### `awsAccessKeyId`

- **Type**: String
- **Default**: `""` (empty string)
- **Description**: AWS access key ID for S3-backed repositories. Required when using S3 storage.
- **Example**: `"AKIAIOSFODNN7EXAMPLE"`

#### `awsSecretAccessKey`

- **Type**: String
- **Default**: `""` (empty string)
- **Description**: AWS secret access key for S3-backed repositories. Required when using S3 storage.
- **Example**: `"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"`

#### `backupTag`

- **Type**: String
- **Default**: `""` (empty string)
- **Description**: Tag to apply to backups. Useful for identifying different servers.
- **Example**: `"all-the-mods-10"`

#### `enableForget`

- **Type**: Boolean
- **Default**: `true`
- **Description**: Whether to run forget & prune after backups to clean up old
  snapshots.

#### `forgetPolicy`

- **Type**: String
- **Default**: `"--keep-daily 7 --keep-hourly 12 --keep-last 3"`
- **Description**: Policy for forget/prune operations. Uses restic's forget command syntax.
- **Example**: `"--keep-daily 14 --keep-weekly 8"`

#### `backupBeforeRestore`

- **Type**: Boolean
- **Default**: `true`
- **Description**: Whether to create a backup before performing a restore operation.

#### `backupInterval`

- **Type**: String (ISO-8601 Duration)
- **Default**: `"PT1H"` (1 hour)
- **Description**: Interval between automatic backups. Must be a positive duration.
- **Valid Formats**: ISO-8601 duration format (e.g., `"PT30M"` for 30 minutes, `"P1D"` for 1 day)
- **Validation**: Must be a positive duration

#### `skipWhenNoPlayers`

- **Type**: Boolean
- **Default**: `true`
- **Description**: Whether to skip scheduled backups when no players are online.

