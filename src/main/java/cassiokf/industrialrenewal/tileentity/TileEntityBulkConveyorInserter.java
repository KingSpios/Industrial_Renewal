package cassiokf.industrialrenewal.tileentity;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class TileEntityBulkConveyorInserter extends TileEntityBulkConveyor
{
    @Override
    public void update()
    {
        if (!world.isRemote)
        {
            insertItem();
        }
        super.update();
    }

    private void insertItem()
    {
        if (!inventory.getStackInSlot(frontNumber).isEmpty())
        {
            EnumFacing facing = getBlockFacing();
            TileEntity te = world.getTileEntity(pos.offset(facing));
            if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()))
            {
                IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
                if (itemHandler != null)
                {
                    for (int j = 0; j < itemHandler.getSlots(); j++)
                    {
                        ItemStack stack = this.inventory.extractItem(frontNumber, 64, true);
                        if (!stack.isEmpty() && itemHandler.isItemValid(j, stack))
                        {
                            ItemStack left = itemHandler.insertItem(j, stack, false);
                            if (!ItemStack.areItemStacksEqual(stack, left))
                            {
                                int toExtract = stack.getCount() - left.getCount();
                                this.inventory.extractItem(frontNumber, toExtract, false);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void dropFrontItem(EnumFacing facing, ItemStack frontPositionItem, BlockPos frontPos)
    {
    }
}
