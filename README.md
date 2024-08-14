
# UnrealEstate: A Zillow-Inspired Minecraft Mod

UnrealEstate implements several aspects of the real-estate marketplace Zillow into Minecraft. The mod introduces a functional item that allows the user to efficiently attain the following information about their Minecraft house:

* The house's square footage
* The lot's square footage
* The direction the house is facing
* Whether or not the house is furnished
* The number of rooms
* The number of bedrooms

In creating this project and translating a real-life concept into a video game with its own rules and conventions, I was forced to make several . Here are some assumptions regarding how square footage and rooms are defined in the eyes of the mod:

* To be factored into square footage, a space must be reachable by foot, passageway, or climb from the front door.
* Square footage roughly consists of any space the player can walk to and fit at, as well as any adjacent open space the player can't walk to or fit within (i.e. livable space; e.g. a space with a fridge and open space above it).
* Spaces are evaluated as potential new rooms if they contain doors or a shift in elevation (including climbable blocks).

## Installation Instructions for Users

* If you haven't yet, install Minecraft Java Edition: https://www.minecraft.net/en-us/store/minecraft-java-bedrock-edition-pc
* Install Minecraft Forge via the installer button under 'Download Recommended' (v. 1.20.6-50.1.0): https://files.minecraftforge.net/net/minecraftforge/forge/
* Download the Minecraft Zillow Mod ZIP file via the 'Code' button near the top of this page.
* Navigate to the directory for minecraft-zillow-mod-main.zip in your computer's command-line shell (Command Prompt, Terminal, etc.) and run the following command:
    ```
    ./gradlew build
    ```
* Locate the newly created JAR file within the current directory.
* Launch Minecraft. In 'Installations,' select the Forge installation, then press the 'Open Folder' icon to the right of the 'Play' button. In the window that pops up, locate the 'mods' folder (if this folder doesn't exist, create it). 
* Add the JAR file to the 'mods' folder.
* Press 'Play.' Once Minecraft has launched, click on the 'Mods' tab and load Zillow Mod.

## Future Additions

Below is a list of ideas in consideration for future versions of UnrealEstate:

* Market value: a feature that assesses the market value of a home based on its characteristics (similar to Zillow's 'Zestimate' feature)
* Incorporation into Survival Mode via Villager trades, land protection, etc.

## Known Issues

Below is a list of known issues with the mod. Please let me know if you encounter any bugs or logical errors that do not fall within the following categories: 

* Passageways that do not contain doors or an elevation shift (e.g. woodland mansion room entrances) are not recognized as room entrances.
* In special cases, the algorithm that traces the house's perimeter will not work as intended, either disallowing full traversal of the house or throwing an exception.


