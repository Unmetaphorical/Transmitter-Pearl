# Transmitter Pearl

Transmitter Pearl is a utility mod that introduces a specialized tool for tactical repositioning. It allows you to establish a link between two living entities and instantaneously swap their positions, regardless of the distance or dimension between them.

### How it Works

The Transmitter Pearl functions by storing the unique signatures of two targets. Once both targets are locked in, a simple activation triggers a spatial swap.

*   **Linking Targets:** Right-click any living entity to set it as a target. You can shift-right-click into the air or on a block to set yourself as one of the targets.
*   **Activation:** Left-click with the pearl to swap the positions of your two linked targets. This works even if one target is in the Nether and the other is in the Overworld.
*   **Management:** Shift-left-click at any time to terminate all active links and reset the pearl.

### Durability and Maintenance

A Transmitter Pearl is a delicate instrument. Each transmission consumes internal energy, and the pearl has a total of 10 uses before it destabilizes.

*   **Repair:** You can maintain the pearl's integrity by repairing it with Redstone Blocks.
*   **Failure:** If the pearl's durability is completely depleted, it will shatter and revert into a standard, non-functional Ender Pearl.

### Trust and Security

To prevent misuse on multiplayer servers, the mod includes a robust trust system and several administrative controls.

*   **Trust System:** When the `linkTrust` gamerule is enabled, players cannot be linked by others unless they have explicitly granted permission.
*   **Commands:** 
    *   `/linkpearl trust <player> <true|false>`: Manage your personal trust list.
    *   `/linkpearl clear`: Instantly breaks any active links currently targeting you.
*   **Gamerules:** Administrators can toggle `linkTrust` to enforce permissions or `linkPearlCrafting` to control the availability of the item.

### Development Status and Open Source

**Exclusive Release Notice**
This is an exclusive, one-time release from Yellow Stack Development and its contributors. Please be aware that this mod is provided as-is; there are no planned updates, feature additions, or ports to other versions or loaders.

**Open Source**
The Transmitter Pearl was created using logic and content derived from the Pocket Lint mod. In the spirit of its origins, the project is entirely open-source, allowing the community to study, modify, or maintain the code independently.
