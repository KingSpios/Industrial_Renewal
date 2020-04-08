package cassiokf.industrialrenewal.handlers;

import cassiokf.industrialrenewal.config.IRConfig;
import cassiokf.industrialrenewal.init.FluidInit;
import cassiokf.industrialrenewal.tileentity.abstracts.TileEntitySyncable;
import cassiokf.industrialrenewal.util.Utils;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class SteamBoiler
{
    private final TileEntitySyncable tiedTE;
    private final int maxHeat = 32000;
    private final String steamName = I18n.format(FluidInit.STEAM.getUnlocalizedName());
    public FluidTank waterTank = new FluidTank(32000)
    {
        @Override
        public boolean canFillFluidType(FluidStack fluid)
        {
            return fluid != null && fluid.getFluid().equals(FluidRegistry.WATER);
        }

        @Override
        public boolean canDrain()
        {
            return false;
        }

        @Override
        public void onContentsChanged()
        {
            SteamBoiler.this.tiedTE.Sync();
        }
    };
    public FluidTank steamTank = new FluidTank(320000)
    {
        @Override
        public boolean canFill()
        {
            return false;
        }

        @Override
        public boolean canDrain()
        {
            return false;
        }

        @Override
        public void onContentsChanged()
        {
            SteamBoiler.this.tiedTE.Sync();
        }
    };
    private boolean useSolid;
    private int amountPerTick;
    private int heat;
    private int oldHeat;
    private int waterPtick = IRConfig.MainConfig.Main.steamBoilerWaterPerTick;
    private int fuelTime;
    private String fuelName = "";
    private int maxFuelTime;
    public FluidTank fuelTank = new FluidTank(32000)
    {
        @Override
        public boolean canFillFluidType(FluidStack fluid)
        {
            return fluid != null && IRConfig.MainConfig.Main.fluidFuel.containsKey(fluid.getFluid().getName());
        }

        @Override
        public int fill(FluidStack resource, boolean doFill)
        {
            return SteamBoiler.this.updateLiquidFuel(resource, doFill);
        }
    };
    public ItemStackHandler solidFuelInv = new ItemStackHandler(1)
    {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack)
        {
            if (stack.isEmpty()) return false;
            return TileEntityFurnace.isItemFuel(stack);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
        {
            return SteamBoiler.this.updateSolidFuel(stack, simulate);
        }
    };
    private int oldFuelTime;

    private boolean needSync;

    public SteamBoiler(TileEntitySyncable tiedTE, BoilerType useSolid, int amountPerTick)
    {
        this.tiedTE = tiedTE;
        this.useSolid = useSolid == BoilerType.Solid;
        this.amountPerTick = amountPerTick;
    }

    public void onTick()
    {
        if (tiedTE.getWorld().isRemote) return;

        updateHeat();

        if (heat >= 10000 && this.waterTank.getFluidAmount() >= waterPtick && this.steamTank.getFluidAmount() < this.steamTank.getCapacity())
        {
            int amount = waterPtick;
            float factor = (heat / 100f) / (maxHeat / 100f);
            amount = Math.round(amount * factor);
            this.waterTank.drainInternal(amount, true);
            FluidStack steamStack = new FluidStack(FluidRegistry.getFluid("steam"), amount * IRConfig.MainConfig.Main.steamBoilerConversionFactor);
            this.steamTank.fillInternal(steamStack, true);
            heat -= 2;
        }

        heat -= 2;
        heat = MathHelper.clamp(heat, 2420, maxHeat);
        fuelTime = Math.max(0, fuelTime);

        outPutSteam();

        //IF no heat turn steam to water
        if (this.steamTank.getFluidAmount() > 0 && heat < 9000)
        {
            this.steamTank.drainInternal(10, true);
        }

        if (needSync || oldHeat != heat || fuelTime != oldFuelTime)
        {
            needSync = false;
            oldHeat = heat;
            oldFuelTime = fuelTime;
            tiedTE.Sync();
        }
    }

    public void outPutSteam()
    {
        if (tiedTE.getWorld().isRemote || this.steamTank.getFluidAmount() <= 0) return;
        BlockPos pos = tiedTE.getPos().up(2); //TODO this needs to change
        TileEntity tileEntity = tiedTE.getWorld().getTileEntity(pos);
        if (tileEntity != null && tileEntity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.DOWN))
        {
            IFluidHandler upTank = tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.DOWN);
            if (upTank != null)
                this.steamTank.drainInternal(upTank.fill(this.steamTank.drainInternal(10000, false), true), true);
        }

    }

    public void setType(BoilerType type, int amountPerTick)
    {
        switch (type)
        {
            case Solid:
                useSolid = true;
                break;
            case Liquid:
                useSolid = false;
                break;
        }
        this.amountPerTick = amountPerTick;
        resetFuelTime();
    }

    public int getFuelTime()
    {
        return fuelTime;
    }

    public int getMaxFuelTime()
    {
        return maxFuelTime;
    }

    public String getFuelName()
    {
        return fuelName;
    }

    public int getHeat()
    {
        return heat;
    }

    public int getMaxHeat()
    {
        return maxHeat;
    }

    public int getWaterTankAmount()
    {
        return waterTank.getFluidAmount();
    }

    public void dropItemsOnGround(BlockPos pos)
    {
        Utils.dropInventoryItems(tiedTE.getWorld(), pos, solidFuelInv);
    }

    private ItemStack updateSolidFuel(ItemStack stack, boolean simulate)
    {
        if (!useSolid) return stack;
        if (fuelTime > 0) return stack;
        int fuel = TileEntityFurnace.getItemBurnTime(stack);
        if (fuel > 0)
        {
            if (!simulate)
            {
                fuelTime = fuel;
                maxFuelTime = fuelTime;
                fuelName = stack.getDisplayName();
                stack.shrink(1);
            }
            ItemStack stack1 = stack.copy();
            stack1.shrink(1);
            return simulate ? stack1 : stack;
        }
        return stack;
    }

    private void updateHeat()
    {
        if (fuelTime >= amountPerTick)
        {
            heat += 8;
            fuelTime -= amountPerTick;
        } else heat -= 2;
    }

    public void coolDown()
    {
        if (tiedTE.getWorld().isRemote) return;
        if (this.steamTank.getFluidAmount() > 0 && heat < 9000)
        {
            this.steamTank.drainInternal(10, true);
        }
        if (heat > 2420)
        {
            heat -= 6;
            tiedTE.Sync();
        }
    }

    private int updateLiquidFuel(FluidStack resource, boolean doFill)
    {
        if (useSolid) return 0;
        if (fuelTime > 0) return 0;
        int fuel = IRConfig.MainConfig.Main.fluidFuel.get(resource.getFluid().getName()) != null ? IRConfig.MainConfig.Main.fluidFuel.get(resource.getFluid().getName()) : 0;
        if (fuel > 0)
        {
            int amount = Math.min(Fluid.BUCKET_VOLUME, resource.amount);
            float norm = Utils.normalize(amount, 0, Fluid.BUCKET_VOLUME);
            if (doFill)
            {
                fuelTime = (int) (fuel * norm);
                maxFuelTime = fuelTime;
                fuelName = resource.getLocalizedName();
            }
            return amount;
        }
        return 0;
    }

    public void resetFuelTime()
    {
        fuelTime = 0;
    }

    public void serialize(NBTTagCompound compound)
    {
        NBTTagCompound newTag = new NBTTagCompound();
        newTag.setInteger("fuelTime", fuelTime);
        newTag.setInteger("maxFuelTime", maxFuelTime);
        newTag.setInteger("amountPerTick", amountPerTick);
        newTag.setInteger("heat", heat);
        newTag.setBoolean("useSolid", useSolid);
        newTag.setString("fuelName", fuelName);
        newTag.setTag("steam", steamTank.writeToNBT(new NBTTagCompound()));
        newTag.setTag("water", waterTank.writeToNBT(new NBTTagCompound()));
        compound.setTag("boiler", newTag);
    }

    public void deserialize(NBTTagCompound compound)
    {
        NBTTagCompound nbt = compound.getCompoundTag("boiler");
        fuelTime = nbt.getInteger("fuelTime");
        maxFuelTime = nbt.getInteger("maxFuelTime");
        amountPerTick = nbt.getInteger("amountPerTick");
        heat = nbt.getInteger("heat");
        useSolid = nbt.getBoolean("useSolid");
        fuelName = nbt.getString("fuelName");
        steamTank.readFromNBT(nbt.getCompoundTag("steam"));
        waterTank.readFromNBT(nbt.getCompoundTag("water"));
    }

    public boolean isBurning()
    {
        return fuelTime > 0;
    }

    //FOR RENDER
    public String getWaterText()
    {
        return FluidRegistry.WATER.getName();
    }

    public String getSteamText()
    {
        return steamName;
    }

    public String getFuelText()
    {
        return fuelTime > 0 ? fuelName : "No Fuel";
    }

    public String getHeatText()
    {
        return Utils.getConvertedTemperatureString(heat / 100F);
    }

    public float getFuelFill() //0 ~ 180
    {
        return Utils.normalize(fuelTime, 0, maxFuelTime) * 180f;
    }

    public float GetWaterFill() //0 ~ 180
    {
        return Utils.normalize(waterTank.getFluidAmount(), 0, waterTank.getCapacity()) * 180f;
    }

    public float GetSteamFill() //0 ~ 180
    {
        return Utils.normalize(steamTank.getFluidAmount(), 0, steamTank.getCapacity()) * 180f;
    }

    public float getHeatFill() //0 ~ 140
    {
        return Utils.normalize(getHeat(), 0, getMaxHeat()) * 140f;
    }

    public enum BoilerType
    {
        Solid,
        Liquid
    }
}
