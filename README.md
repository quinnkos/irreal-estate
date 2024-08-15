# IrrealEstate: A Zillow-Inspired Minecraft Mod

![Screenshot](images/IrrealEstateSS.png)

## Table of Contents

- [About the Project](#about)
  * [Description](#description)
  * [Usage](#usage)
- [Installation Instructions](#installation)
  * [Prerequisites](#prerequisites)
  * [Instructions for Users](#instructions-for-users)
  * [Instructions for Developers](#instructions-for-developers)
- [Future Additions](#future-additions)
- [Known Issues](#known-issues)

## About

### Description

IrrealEstate implements aspects of the real-estate marketplace Zillow into Minecraft. The mod introduces a 'Property Profiler' item that allows the user to efficiently attain the following information about their Minecraft house:

* The house's square footage
* The lot's square footage
* The direction the house is facing
* Whether the house is furnished
* The number of rooms
* The number of bedrooms

In translating a real-life concept into a video game with its own rules and conventions, I had to define some of my own rules to complement the real-life practices I took inspiration from (e.g. what parts of a house legally count toward square feet). Here are some assumptions regarding how square footage and rooms are defined in the eyes of the mod:

* To be factored into square footage, a space must be reachable by foot, passageway, or climb from the front door.
* Square footage roughly consists of any space the player can walk to and fit at, as well as any adjacent open space the player can't walk to or fit within (i.e. livable space; e.g. a space with a fridge and open space above it).
* Spaces are evaluated as potential new rooms if they contain doors or a shift in elevation (including climbable blocks).

### Usage

Using the Property Profiler involves three steps:
* Firstly, right-click one corner of the house's lot (the elevation of the block you right-click does not matter).
* Next, right-click the opposite corner of the lot.
* Finally, right-click the block before the front door of the house.

After completing these three steps, the user will be provided with the information described above.

## Installation

### Prerequisites

* Minecraft Java Edition: https://www.minecraft.net/en-us/store/minecraft-java-bedrock-edition-pc
* For users:
  * Minecraft Forge (v. 1.20.6-50.1.0): https://files.minecraftforge.net/net/minecraftforge/forge/
* For developers:
  * Java Development Kit (JDK): https://www.oracle.com/java/technologies/downloads/
  * Gradle: https://gradle.org/install/

### Instructions for Users

* Download the IrrealEstate ZIP file via the 'Code' button near the top of this page.
* Decompress the ZIP file and open the resulting folder in your computer's command-line shell (Command Prompt, Terminal, etc.) and run the following command:
    ```
    ./gradlew build
    ```
* Locate the newly created JAR file within the current directory.
* Launch Minecraft. In 'Installations,' select the Forge installation, then press the 'Open Folder' icon to the right of the 'Play' button. In the window that pops up, locate the 'mods' folder (if this folder doesn't exist, create it).
* Add the JAR file to the 'mods' folder.
* Press 'Play.' Once Minecraft has launched, click the 'Mods' tab and load IrrealEstate.

### Instructions for Developers

* Download the IrrealEstate ZIP file via the 'Code' button near the top of this page.
* Decompress the ZIP file and open the resulting folder in your IDE.
* To launch Minecraft Forge, navigate to Gradle -> Tasks -> forgegradle runs -> runClient (in IntelliJ IDEA, the Gradle icon is located in the top right).

## Future Additions

Below is a list of ideas in consideration for future versions of IrrealEstate:

* Market value: a feature that assesses the market value of a home based on its characteristics (similar to Zillow's 'Zestimate' feature).
* Incorporation into Survival Mode via Villager trades, land protection, etc.

## Known Issues

Below is a list of known issues with the mod. Please let me know if you encounter any bugs or logical errors that do not fall within the following categories:

* Passageways that do not contain doors or an elevation shift (e.g. woodland mansion room entrances) are not recognized as room entrances.
* In special cases, the algorithm that traces the house's perimeter will not work as intended, either disallowing full traversal of the house or throwing an exception.
