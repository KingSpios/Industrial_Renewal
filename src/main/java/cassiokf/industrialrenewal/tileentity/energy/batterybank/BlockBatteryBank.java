package cassiokf.industrialrenewal.tileentity.energy.batterybank;

import cassiokf.industrialrenewal.blocks.BlockTileEntity;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nullable;

public class BlockBatteryBank extends BlockTileEntity<TileEntityBatteryBank> {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool WEST = PropertyBool.create("west");

    public BlockBatteryBank(String name, CreativeTabs tab) {
        super(Material.IRON, name, tab);
    }

    private boolean CanConnect(IBlockAccess world, BlockPos pos, EnumFacing correspondentFacing) {
        if (world.getBlockState(pos).getValue(FACING).getOpposite() == correspondentFacing) return false;
        TileEntity te = world.getTileEntity(pos.offset(correspondentFacing));
        return te != null && te.hasCapability(CapabilityEnergy.ENERGY, correspondentFacing.getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getActualState(IBlockState state, final IBlockAccess world, final BlockPos pos) {
        return state.withProperty(SOUTH, CanConnect(world, pos, EnumFacing.SOUTH)).withProperty(NORTH, CanConnect(world, pos, EnumFacing.NORTH))
                .withProperty(EAST, CanConnect(world, pos, EnumFacing.EAST)).withProperty(WEST, CanConnect(world, pos, EnumFacing.WEST));
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, SOUTH, NORTH, WEST, EAST);
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @Deprecated
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public Class<TileEntityBatteryBank> getTileEntityClass() {
        return TileEntityBatteryBank.class;
    }

    @Nullable
    @Override
    public TileEntityBatteryBank createTileEntity(World world, IBlockState state) {
        return new TileEntityBatteryBank();
    }
}
