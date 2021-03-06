package cassiokf.industrialrenewal.util.compat.jei.lathe;

import cassiokf.industrialrenewal.References;
import cassiokf.industrialrenewal.init.ModBlocks;
import cassiokf.industrialrenewal.util.compat.jei.RecipeCategories;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

public class LatheRecipeCategory extends AbstractLatheRecipeCategory<JEILatheRecipe>
{
    private final IDrawable background;
    private final String name;

    public LatheRecipeCategory(IGuiHelper helper)
    {
        super(helper);
        this.background = helper.createDrawable(TEXTURE, 4, 7, 150, 73);
        this.name = ModBlocks.latheMachine.getLocalizedName();
    }

    @Override
    public void drawExtras(Minecraft minecraft)
    {
        cutterStatic.draw(minecraft, 80, 44);
        energyBar.draw(minecraft, 4, 2);
    }

    @Override
    public String getUid()
    {
        return RecipeCategories.LATHE;
    }

    @Override
    public String getTitle()
    {
        return name;
    }

    @Override
    public String getModName()
    {
        return References.NAME;
    }

    @Override
    public IDrawable getBackground()
    {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, JEILatheRecipe recipeWrapper, IIngredients ingredients)
    {
        IGuiItemStackGroup groups = recipeLayout.getItemStacks();
        groups.init(input, true, 39, 22);
        groups.init(output, false, 129, 22);
        groups.set(ingredients);
    }

    @Nullable
    @Override
    public IDrawable getIcon()
    {
        return icon;
    }
}
