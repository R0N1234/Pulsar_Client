package dev.pulsarclient.mods;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Descobre se uma entidade está escondida atrás de blocos sólidos, para o Motor Pulsar não desenhar.
 * Só blocos opacos e inteiros contam como parede: vidro, folhas no modo bonito, lajes e escadas não escondem nada.
 * O resultado fica guardado alguns ticks, então o custo é poucos raios por entidade a cada 4 ticks.
 */
final class Oclusao {
    private static final int RECHECK_TICKS = 4;
    private static final double MAX_DISTANCE = 96;
    private static final double NEAR = 3;

    private final Map<Integer, int[]> cache = new HashMap<Integer, int[]>();
    private int ticks;

    void tick() {
        ticks++;
        if (ticks % 200 != 0) return;
        for (Iterator<int[]> it = cache.values().iterator(); it.hasNext(); ) {
            if (ticks - it.next()[0] > 200) it.remove();
        }
    }

    void clear() {
        cache.clear();
    }

    boolean hidden(World world, Vec3 eye, Entity entity) {
        int[] entry = cache.get(entity.getEntityId());
        if (entry != null && ticks - entry[0] < RECHECK_TICKS) return entry[1] == 1;
        boolean hidden = compute(world, eye, entity);
        cache.put(entity.getEntityId(), new int[]{ticks, hidden ? 1 : 0});
        return hidden;
    }

    private static boolean compute(World world, Vec3 eye, Entity entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double cx = (box.minX + box.maxX) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        Vec3 center = new Vec3(cx, (box.minY + box.maxY) / 2, cz);
        double dist = eye.distanceTo(center);
        if (dist < NEAR || dist > MAX_DISTANCE) return false;
        // Visível se qualquer um dos pontos (topo, meio, pé e as laterais) puder ser visto.
        return blocked(world, eye, new Vec3(cx, box.maxY - 0.05, cz))
                && blocked(world, eye, center)
                && blocked(world, eye, new Vec3(cx, box.minY + 0.1, cz))
                && blocked(world, eye, new Vec3(box.minX, box.maxY - 0.05, box.minZ))
                && blocked(world, eye, new Vec3(box.maxX, box.maxY - 0.05, box.maxZ));
    }

    /** Percorre os blocos da linha de "from" até "to" (algoritmo DDA de voxels). */
    private static boolean blocked(World world, Vec3 from, Vec3 to) {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;
        int x = MathHelper.floor_double(from.xCoord);
        int y = MathHelper.floor_double(from.yCoord);
        int z = MathHelper.floor_double(from.zCoord);
        int endX = MathHelper.floor_double(to.xCoord);
        int endY = MathHelper.floor_double(to.yCoord);
        int endZ = MathHelper.floor_double(to.zCoord);
        int stepX = dx > 0 ? 1 : -1;
        int stepY = dy > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;
        double tDeltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx);
        double tDeltaY = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy);
        double tDeltaZ = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
        double tMaxX = dx == 0 ? Double.MAX_VALUE : (stepX > 0 ? x + 1 - from.xCoord : from.xCoord - x) * tDeltaX;
        double tMaxY = dy == 0 ? Double.MAX_VALUE : (stepY > 0 ? y + 1 - from.yCoord : from.yCoord - y) * tDeltaY;
        double tMaxZ = dz == 0 ? Double.MAX_VALUE : (stepZ > 0 ? z + 1 - from.zCoord : from.zCoord - z) * tDeltaZ;
        for (int i = 0; i < 300; i++) {
            // Chegou no bloco do ponto final sem bater em parede.
            if (Math.min(tMaxX, Math.min(tMaxY, tMaxZ)) > 1) return false;
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
            if (x == endX && y == endY && z == endZ) return false;
            if (world.getBlockState(new BlockPos(x, y, z)).getBlock().isOpaqueCube()) return true;
        }
        return false;
    }
}
