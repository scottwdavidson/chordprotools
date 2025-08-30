# Pour Choices CLI: OnSong ChordPro Catalog Manager
This command-line application was developed to streamline the management and standardization of ChordPro (.cho) files for the [Pour Choices band](https://pourchoicesmusic.com). It addresses the specific need to ensure consistency across the band's song library, particularly for performance-critical metadata used with the OnSong music application.

The tool works by creating a unified catalog of all songs from a designated directory, allowing band members to easily update song information in a familiar spreadsheet format. It then applies these changes back to the individual ChordPro files, ensuring everyone has the most up-to-date and complete versions.

## Key Features
- Song Cataloging: Automatically scans and catalogs all ChordPro (.cho) files within a specified directory.

- Standardized Metadata: Enforces the inclusion of essential OnSong metadata fields such as title, author, key, and duration, which is crucial for features like auto-scrolling.

- Custom Performance Metadata: Allows for the management of performance-specific information that can be displayed at the top of the chord sheet, including:

  - nord_voice: The specific voice to use on the Nord piano.

  - capo: Whether a capo is needed on the guitar.

  - backing_track: The associated backing track number from an RC-500 Looper.

  - countin: The type of count-in (e.g., from the backing track or Beat Buddy drum machine).

- CSV-based Editing: Generates a .csv file for easy, bulk editing of song data using spreadsheet applications like Google Sheets or Microsoft Excel.

- File Application: Applies all changes from the edited .csv file back to the individual ChordPro files.

## How It Works
The CLI tool scans your local song directory (e.g., ./cho) and generates a __catalog-index.csv__ catalog file containing all the current song metadata.

You and your bandmates can open and edit this __catalog-index.csv__ file, making changes to metadata fields in a collaborative, spreadsheet-based environment.

Once the edits are complete, you run the tool again to apply the changes.

The tool reads the updated __catalog-index.csv__ and automatically writes the new metadata to the header of each corresponding .cho file.

## Installation
TBD 

## Usage
Basic usage examples will be provided here, such as:

### Generate the initial song catalog

### Update the song catalog 

### Update a song(s) files
pour-choices-cli catalog --dir ./songs

### (new feature) Generate set list

## Future Plans
The initial version of this application is complete, and we plan to expand its functionality to provide other valuable gig and practice support features for the band.

## Contributing
We welcome contributions from fellow band members and developers. If you have an idea for a new feature or find a bug, please open an issue or submit a pull request.

## License
This project is licensed under the MIT License.