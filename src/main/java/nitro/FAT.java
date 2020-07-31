package nitro;

import io.BinaryWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the File Name Table
 */
public class FAT {
    /**
     * Write the FNT section in the ROM
     *
     * @param rom  ROM binary stream
     * @param root Root nitro directory of the ROM
     * @throws IOException
     */
    public static void writeFAT(BinaryWriter rom, NitroDirectory root, List<Integer> overlayStartOffsets, List<Integer> overlayEndOffsets) throws IOException {
        if (root.getId() == 0xf000) {
            for (int i = 0; i < overlayStartOffsets.size(); i++) {
                rom.writeInt(overlayStartOffsets.get(i));
                rom.writeInt(overlayEndOffsets.get(i));
            }
            writeFAT(rom, root);
        } else
            throw new IOException("This is not the root directory!");
    }

    /**
     * Pre-calculate the size of the FAT section starting from a path (overlays are excluded from calculation)
     *
     * @param path Path of the folder to calculate the FAT
     * @return Size in bytes of the FAT section
     * @implNote The overlays are not counted
     */
    public static int calculateFATSize(File path) {
        int n = Objects.requireNonNull(path.listFiles(File::isFile)).length * 8;
        for (File t : Objects.requireNonNull(path.listFiles(File::isDirectory)))
            n += calculateFATSize(t);
        return n;
    }

    /**
     * Recursively write the FAT section of the ROM
     *
     * @param rom  ROM binary stream
     * @param root Root nitro directory of the ROM
     * @throws IOException
     */
    private static void writeFAT(BinaryWriter rom, NitroDirectory root) throws IOException {
        for (NitroDirectory d : root.getDirectoryList())
            writeFAT(rom, d);
        for (NitroFile f : root.getFileList()) {
            rom.writeInt(f.getOffset());
            rom.writeInt(f.getOffset() + f.getSize());
        }
    }
}