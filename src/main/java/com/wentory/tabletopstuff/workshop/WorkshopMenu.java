package com.wentory.tabletopstuff.workshop;

import com.wentory.tabletopstuff.ModContent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

public final class WorkshopMenu extends AbstractContainerMenu {
    public record Material(Item item,int count){}
    public static final List<Material> NULL_MATERIALS=List.of(new Material(Items.PAPER,10),
        new Material(Items.RED_DYE,1),new Material(Items.GREEN_DYE,1),new Material(Items.BLUE_DYE,1));
    private final ContainerLevelAccess access;
    private final Inventory inventory;
    private final DataSlot craftRevision=DataSlot.standalone();
    public WorkshopMenu(int id,Inventory inventory){this(id,inventory,ContainerLevelAccess.NULL);}
    public WorkshopMenu(int id,Inventory inventory,ContainerLevelAccess access){
        super(ModContent.WORKSHOP_MENU.get(),id);this.inventory=inventory;this.access=access;
        addDataSlot(craftRevision);
        for(int i=0;i<36;i++)addSlot(new Slot(inventory,i,-1000,-1000));
    }
    public int craftRevision(){return craftRevision.get();}
    public int owned(Item item){
        int count=0;
        for(int i=0;i<36;i++)if(inventory.getItem(i).is(item))count+=inventory.getItem(i).getCount();
        return count;
    }
    public boolean canCraft(){return NULL_MATERIALS.stream().allMatch(material->owned(material.item())>=material.count());}
    @Override public boolean clickMenuButton(Player player,int button){
        if(button!=0||player.level().isClientSide||!stillValid(player)||!canCraft())return false;
        for(var material:NULL_MATERIALS){
            int remaining=material.count();
            for(int i=0;i<36&&remaining>0;i++){
                ItemStack stack=inventory.getItem(i);
                if(!stack.is(material.item()))continue;
                int taken=Math.min(remaining,stack.getCount());stack.shrink(taken);remaining-=taken;
            }
        }
        ItemStack result=new ItemStack(ModContent.LOBBY_ITEM.get());
        if(!inventory.add(result))player.drop(result,false);
        craftRevision.set((short)(craftRevision.get()+1));
        inventory.setChanged();broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){return stillValid(access,player,ModContent.WORKSHOP.get());}
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    @Override public void clicked(int slot,int button,ClickType type,Player player){}
}
