package ru.liahim.mist.block;

import com.google.common.collect.Sets;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ru.liahim.mist.api.block.MistBlocks;
import ru.liahim.mist.common.Mist;
import ru.liahim.mist.common.MistTeleporter;
import ru.liahim.mist.util.PortalCoordData;

public class MistPortal extends BlockBreakable {
    protected static final AxisAlignedBB XZ_AABB = new AxisAlignedBB(0.125D, -0.6875D, 0.125D, 0.875D, 1.6875D, 0.875D);

    public MistPortal() {
        super(Material.PORTAL, false);
        this.setLightLevel(0.8125F);
        this.setLightOpacity(0);
        this.setSoundType(SoundType.SNOW);
    }

    @Override
    public String getUnlocalizedName() {
        return "tile.mist." + super.getUnlocalizedName().substring(5);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return XZ_AABB;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    /*
     * @Override
	 * public boolean isBurning(IBlockAccess world, BlockPos pos) {
     *     return true;
     * }
     */

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!entity.isRiding() && !entity.isBeingRidden() && entity.isNonBoss()) {
            if (entity instanceof EntityPlayerMP) {
                EntityPlayerMP playerMP = (EntityPlayerMP) entity;
                if (pos.getX() == MathHelper.floor(entity.posX) && pos.getZ() == MathHelper.floor(entity.posZ)) {
                    BlockPos immutablePos = pos.toImmutable();
                    MinecraftServer server = playerMP.getServer();
                    PlayerList playerList = server.getPlayerList();
                    if (playerMP.dimension != Mist.getID()) {
                        playerList.transferPlayerToDimension(
                            playerMP,
                            Mist.getID(),
                            new MistTeleporter(server.getWorld(Mist.getID()), immutablePos)
                        );
                    } else {
                        int dimId = PortalCoordData.get(world).getDim(Mist.getID(), immutablePos);
                        playerList.transferPlayerToDimension(
                            playerMP,
                            dimId,
                            new MistTeleporter(server.getWorld(dimId), immutablePos)
                        );
                    }
                }
            }
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!world.isRemote && Sets.newHashSet(MistBlocks.PORTAL_WORK, Blocks.AIR).contains(blockIn)) {
            world.scheduleUpdate(pos, state.getBlock(), 1);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!world.isRemote) {
            IBlockState up = world.getBlockState(pos.up());
            IBlockState down = world.getBlockState(pos.down());
            if (
                !(up.getBlock() == MistBlocks.PORTAL_WORK && up.getValue(MistPortalStone.ISUP)) ||
                !(down.getBlock() == MistBlocks.PORTAL_WORK && !down.getValue(MistPortalStone.ISUP))
            ) {
                world.setBlockToAir(pos);
            }
        }
    }

    @Override
    public ItemStack getPickBlock(
        IBlockState state,
        RayTraceResult target,
        World world,
        BlockPos pos,
        EntityPlayer player
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        PortalCoordData.get(world).removeCoords(world.provider.getDimension(), pos);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(
        IBlockState blockState,
        IBlockAccess blockAccess,
        BlockPos pos,
        EnumFacing side
    ) {
        return !(side == EnumFacing.UP || side == EnumFacing.DOWN);
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.AIR;
    }
}
