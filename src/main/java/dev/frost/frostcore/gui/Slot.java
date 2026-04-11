package dev.frost.frostcore.gui;

import java.util.Arrays;

/**
 * Utility class for slot index arithmetic.
 * <p>
 * Minecraft inventory slots use a flat array — row 0 is slots 0–8,
 * row 1 is slots 9–17, etc.  All methods here help you think in
 * (row, column) terms instead of raw integers.
 *
 * <pre>{@code
 * // Place an item in row 2, column 4
 * gui.setItem(Slot.of(2, 4), myItem);
 *
 * // Fill the border of a 6-row chest
 * for (int s : Slot.borderSlots(6)) gui.setItem(s, filler);
 *
 * // Used a content rect for PagedGui
 * int[] content = Slot.rectangle(1, 0, 4, 8); // rows 1-4, all columns
 * }</pre>
 */
public final class Slot {

    private Slot() {}

    // ── Conversion ───────────────────────────────────────────────────────────

    /** Convert (row, col) to a flat slot index. */
    public static int of(int row, int col) {
        return row * 9 + col;
    }

    /** Get the row of a flat slot index. */
    public static int row(int slot) {
        return slot / 9;
    }

    /** Get the column of a flat slot index. */
    public static int col(int slot) {
        return slot % 9;
    }

    // ── Row helpers ──────────────────────────────────────────────────────────

    /** Return all 9 slots in a given row. */
    public static int[] row(int rowIndex, int unusedOverload) {
        int[] slots = new int[9];
        for (int c = 0; c < 9; c++) slots[c] = of(rowIndex, c);
        return slots;
    }

    /** Return all slots in the given row. Sugar for {@code rowSlots(n)}. */
    public static int[] rowSlots(int rowIndex) {
        int[] slots = new int[9];
        for (int c = 0; c < 9; c++) slots[c] = of(rowIndex, c);
        return slots;
    }

    // ── Region helpers ───────────────────────────────────────────────────────

    /**
     * Return all slots in a rectangular region (inclusive on both ends).
     *
     * @param startRow first row (0-indexed)
     * @param startCol first column (0-indexed)
     * @param endRow   last row (inclusive)
     * @param endCol   last column (inclusive)
     */
    public static int[] rectangle(int startRow, int startCol, int endRow, int endCol) {
        int rows = endRow - startRow + 1;
        int cols = endCol - startCol + 1;
        int[] slots = new int[rows * cols];
        int idx = 0;
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                slots[idx++] = of(r, c);
            }
        }
        return slots;
    }

    /**
     * Return all border slots for a chest of the given row count.
     * <p>
     * Border = top row + bottom row + leftmost + rightmost columns.
     */
    public static int[] borderSlots(int rows) {
        if (rows <= 0) return new int[0];
        if (rows == 1) return rowSlots(0);

        java.util.Set<Integer> set = new java.util.LinkedHashSet<>();
        // Top row
        for (int c = 0; c < 9; c++) set.add(of(0, c));
        // Bottom row
        for (int c = 0; c < 9; c++) set.add(of(rows - 1, c));
        // Left & right columns (middle rows only)
        for (int r = 1; r < rows - 1; r++) {
            set.add(of(r, 0));
            set.add(of(r, 8));
        }
        return set.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Return all non-border slots for a chest of the given row count.
     * Complements {@link #borderSlots(int)}.
     */
    public static int[] innerSlots(int rows) {
        if (rows <= 2) return new int[0];
        return rectangle(1, 1, rows - 2, 7);
    }

    // ── Checks ───────────────────────────────────────────────────────────────

    /** Returns true if the slot lies on the border of a chest with the given row count. */
    public static boolean isBorder(int slot, int rows) {
        int r = row(slot);
        int c = col(slot);
        return r == 0 || r == rows - 1 || c == 0 || c == 8;
    }

    /** Returns true if the given raw slot index is inside the top inventory (GUI). */
    public static boolean isInTopInventory(int rawSlot, int guiSize) {
        return rawSlot >= 0 && rawSlot < guiSize;
    }

    // ── Common preset indices ────────────────────────────────────────────────

    public static final int TOP_LEFT     = of(0, 0);
    public static final int TOP_CENTER   = of(0, 4);
    public static final int TOP_RIGHT    = of(0, 8);

    /** Bottom row offsets — add `(rows - 1) * 9` to get the actual slot for your chest size. */
    public static int bottomLeft(int rows)   { return of(rows - 1, 0); }
    public static int bottomCenter(int rows) { return of(rows - 1, 4); }
    public static int bottomRight(int rows)  { return of(rows - 1, 8); }
}
