package com.riftbound.world;

import com.riftbound.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public class RiftMonolithGenerator {
    public static void buildMonolith(ServerLevel level, BlockPos center) {
        // 7x7 Taban (Obsidyen ve Polished Blackstone)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.OBSIDIAN.defaultBlockState());
                }
            }
        }
        
        // 4 Kose Kirik Sutunlari (Crying Obsidian & Cyan Glass)
        int[][] corners = {{-3,-3}, {3,-3}, {-3,3}, {3,3}};
        for (int[] c : corners) {
            level.setBlockAndUpdate(center.offset(c[0], 0, c[1]), Blocks.CRYING_OBSIDIAN.defaultBlockState());
            level.setBlockAndUpdate(center.offset(c[0], 1, c[1]), Blocks.CYAN_STAINED_GLASS.defaultBlockState());
            level.setBlockAndUpdate(center.offset(c[0], 2, c[1]), Blocks.END_ROD.defaultBlockState());
        }

        // Giris Merdiveni
        level.setBlockAndUpdate(center.offset(0, -1, -4), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState());
        level.setBlockAndUpdate(center.offset(-1, -1, -4), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState());
        level.setBlockAndUpdate(center.offset(1, -1, -4), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState());

        // Merkez Sunagi
        level.setBlockAndUpdate(center.offset(0, 0, 0), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(center.offset(0, 1, 0), ModBlocks.RIFT_ALTAR.defaultBlockState());
    }
}