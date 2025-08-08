# OmegaT Import/Export Credentials Plugin

**Features**

- **Import** credentials from a `.properties` file (in standard Java properties format).
- **Export** selected credentials in bulk to a `.properties` file.
- **Delete** selected credentials from the OmegaT repository credentials store.
- **Manually add** new repository credentials, with an option to strip whitespace from inputs.

## Building

To build the plugin, run:

```bash
./gradlew installDist
```

## Dependencies

OmegaT libraries and other dependencies are resolved from remote Maven repositories.  
Ensure you have an internet connection when compiling the project.

This plugin is compatible with **OmegaT 5.7.0** and newer versions.

## Where to find the built artifact?

After building, the distribution files can be found at:
`build/distributions/install`
within your local repository copy.

Prebuilt binaries are also available from the [Releases](https://github.com/capstanlqc/omegat-plugin-importExportCredentials/releases) page.

## Installation

To install the plugin:

1. Extract the plugin JAR file from the ZIP distribution.
2. Copy the JAR into the `plugins` subfolder of your OmegaT user configuration directory  
   (in OmegaT, open **Options → Access Configuration Folder**; create a `plugins` folder if it doesn't exist).
3. OmegaT scans plugins recursively in that folder. You may place plugin JARs in subfolders for organization, but this is optional.

## License

This project is distributed under the **GNU General Public License version 3 or later**.


