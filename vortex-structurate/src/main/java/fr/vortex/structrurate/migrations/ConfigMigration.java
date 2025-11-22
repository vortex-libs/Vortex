package fr.vortex.structrurate.migrations;

import fr.vortex.structrurate.node.ConfigNode;

public interface ConfigMigration {
    void migrate(ConfigNode root);
}
