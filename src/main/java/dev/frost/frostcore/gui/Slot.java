package dev.frost.frostcore.gui;

import java.util.Arrays;


public final class Slot {

    private Slot() {}

    
    public static int of(int row, int col) {
        return row * 9 + col;
    }

    
    public static int row(int slot) {
        return slot / 9;
    }

    
    public static int col(int slot) {
        return slot % 9;
    }

    
    public static int[] row(int rowIndex, int unusedOverload) {
        int[] slots = new int[9];
        for (int c = 0; c < 9; c++) slots[c] = of(rowIndex, c);
        return slots;
    }

    
    public static int[] rowSlots(int rowIndex) {
        int[] slots = new int[9];
        for (int c = 0; c < 9; c++) slots[c] = of(rowIndex, c);
        return slots;
    }

    
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

    
    public static int[] borderSlots(int rows) {
        if (rows <= 0) return new int[0];
        if (rows == 1) return rowSlots(0);

        java.util.Set<Integer> set = new java.util.LinkedHashSet<>();

        for (int c = 0; c < 9; c++) set.add(of(0, c));

        for (int c = 0; c < 9; c++) set.add(of(rows - 1, c));

        for (int r = 1; r < rows - 1; r++) {
            set.add(of(r, 0));
            set.add(of(r, 8));
        }
        return set.stream().mapToInt(Integer::intValue).toArray();
    }

    
    public static int[] innerSlots(int rows) {
        if (rows <= 2) return new int[0];
        return rectangle(1, 1, rows - 2, 7);
    }

    
    public static boolean isBorder(int slot, int rows) {
        int r = row(slot);
        int c = col(slot);
        return r == 0 || r == rows - 1 || c == 0 || c == 8;
    }

    
    public static boolean isInTopInventory(int rawSlot, int guiSize) {
        return rawSlot >= 0 && rawSlot < guiSize;
    }

    public static final int TOP_LEFT     = of(0, 0);
    public static final int TOP_CENTER   = of(0, 4);
    public static final int TOP_RIGHT    = of(0, 8);

    
    public static int bottomLeft(int rows)   { return of(rows - 1, 0); }
    public static int bottomCenter(int rows) { return of(rows - 1, 4); }
    public static int bottomRight(int rows)  { return of(rows - 1, 8); }

    
    public static int[] getCenteredIndices(int totalSlots, int items) {
        if (items <= 0) return new int[0];
        if (items >= totalSlots) {
            int[] res = new int[totalSlots];
            for (int i = 0; i < totalSlots; i++) res[i] = i;
            return res;
        }

        if (totalSlots == 7) {
            switch (items) {
                case 1: return new int[]{3};
                case 2: return new int[]{2, 4};
                case 3: return new int[]{1, 3, 5};
                case 4: return new int[]{1, 2, 4, 5};
                case 5: return new int[]{1, 2, 3, 4, 5};
                case 6: return new int[]{0, 1, 2, 4, 5, 6};
            }
        }

        
        int margin = (totalSlots - items) / 2;
        int[] res = new int[items];
        for (int i = 0; i < items; i++) res[i] = margin + i;
        return res;
    }
}

