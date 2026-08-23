package dev.huskuraft.effortless.building.clipboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.huskuraft.universal.api.tag.ListTag;
import dev.huskuraft.universal.api.tag.RecordTag;
import dev.huskuraft.universal.api.tag.Tag;

/**
 * Strips stored items from copied block-entity NBT so paste cannot duplicate
 * container or machine inventories. Facing, energy, and other config remain.
 */
public final class BlockEntityTagSanitizer {

    private static final Set<String> INVENTORY_KEYS = Set.of(
            "items",
            "inventory",
            "item",
            "recorditem",
            "book",
            "armoritems",
            "handitems",
            "equipment",
            "itemhandler",
            "internalinventory",
            "iteminventory",
            "storeditems",
            "upgrades",
            "upgrade",
            "patterns",
            "contents",
            "stacks",
            "slots",
            "chargedprojectiles",
            "loottable",
            "loottableseed",
            "deathloottable"
    );

    /**
     * Formed-multiblock identity. Mekanism writes inventoryID into the world
     * MultiblockCache; pasting that UUID attaches to the original structure.
     */
    private static final Set<String> STRUCTURE_KEYS = Set.of(
            "inventoryid",
            "hasstructure",
            "volume",
            "lowervolume",
            "renderlocation",
            "rendery",
            "rendering",
            "cache",
            "originallocation",
            "chunkset",
            "valve",
            "valves",
            "locations",
            "formed",
            "structure",
            "multiblock",
            "controllerpos",
            "controller",
            "master",
            "ismaster",
            "bounds",
            "network"
    );

    private BlockEntityTagSanitizer() {
    }

    public static RecordTag sanitize(RecordTag tag) {
        if (tag == null) {
            return null;
        }
        try {
            var copy = RecordTag.of(tag.getTags());
            sanitizeRecord(copy);
            return copy;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void sanitizeRecord(RecordTag tag) {
        for (var key : List.copyOf(tag.keySet())) {
            if (isInventoryKey(key) || isStructureKey(key)) {
                tag.remove(key);
                continue;
            }
            var sanitized = sanitizeValue(tag.getTag(key));
            if (sanitized == null) {
                tag.remove(key);
            } else if (sanitized != tag.getTag(key)) {
                tag.putTag(key, sanitized);
            }
        }
    }

    private static Tag sanitizeValue(Tag tag) {
        if (tag == null) {
            return null;
        }
        return switch (tag.getId()) {
            case Tag.TAG_COMPOUND -> {
                var record = tag.asRecord();
                if (isItemStack(record)) {
                    yield null;
                }
                sanitizeRecord(record);
                yield record;
            }
            case Tag.TAG_LIST -> sanitizeList(tag.asList());
            default -> tag;
        };
    }

    private static Tag sanitizeList(ListTag list) {
        var kept = new ArrayList<Tag>();
        for (var i = 0; i < list.size(); i++) {
            var sanitized = sanitizeValue(list.getTag(i));
            if (sanitized != null) {
                kept.add(sanitized);
            }
        }
        if (kept.isEmpty()) {
            return null;
        }
        return ListTag.of(kept);
    }

    private static boolean isInventoryKey(String key) {
        var normalized = key.toLowerCase(Locale.ROOT);
        if (INVENTORY_KEYS.contains(normalized)) {
            return true;
        }
        return normalized.endsWith("items")
                || normalized.endsWith("inventory")
                || normalized.endsWith("itemhandler");
    }

    private static boolean isStructureKey(String key) {
        return STRUCTURE_KEYS.contains(key.toLowerCase(Locale.ROOT));
    }

    private static boolean isItemStack(RecordTag tag) {
        var idTag = tag.getTag("id");
        if (idTag == null || idTag.getId() != Tag.TAG_STRING) {
            return false;
        }
        var id = idTag.asString().getString();
        if (id == null || id.isEmpty()) {
            return false;
        }
        return hasNumeric(tag, "Count")
                || hasNumeric(tag, "count")
                || hasNumeric(tag, "Slot")
                || hasNumeric(tag, "slot")
                || tag.getTag("tag") != null
                || tag.getTag("components") != null;
    }

    private static boolean hasNumeric(RecordTag tag, String key) {
        var value = tag.getTag(key);
        if (value == null) {
            return false;
        }
        return switch (value.getId()) {
            case Tag.TAG_BYTE, Tag.TAG_SHORT, Tag.TAG_INT, Tag.TAG_LONG, Tag.TAG_FLOAT, Tag.TAG_DOUBLE -> true;
            default -> false;
        };
    }
}
