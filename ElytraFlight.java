/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  ru.ntfhack.obfuscator.annotations.FlowObfuscation
 *  ru.ntfhack.obfuscator.annotations.FlowObfuscation$Type
 */
package a;


@Module.registerModule(name ="ElytraFlight", alias="Elytra Flight", description="Улучшает полет на элитре с фейрверками", category= ModuleCategory.MOVEMENT)
public class ElytraFlight
extends Module {
    private final NumberSetting speedXZ = new NumberSetting("Скорость по XZ", 1.2f, 0.5f, 1.9f, 0.01f);
    private final NumberSetting speedY = new NumberSetting("Скорость по Y", 0.1f, 0.1f, 1.0f, 0.01f);
    private final NumberSetting fireworkSlot = new NumberSetting("Слот с фейрверком", 9.0f, 1.0f, 9.0f, 1.0f);
    private final NumberSetting fireworkDelay = new NumberSetting("Задержка фейрверка", 1.5f, 0.5f, 1.5f, 0.1f);
    private final BooleanSetting keepFlying = new BooleanSetting("Продолжать лететь", false);
    private final BooleanSetting stayOffGroung = new BooleanSetting("Не приземляться", true);
    private final BooleanSetting superBow = new BooleanSetting("Супер лук", false);

    private ItemStack prevArmorItemCopy;
    private Item prevArmorItem = Items.AIR;
    private int prevElytraSlot = -1;
    private ItemStack getStackInSlotCopy;
    private Item prevItemInHand = Items.AIR;
    private int slotWithFireWorks = -1;
    private long lastFireworkTime;
    private boolean elytraEquiped;
    private boolean flying;
    private int currentSpeed;
    private int ticksInAir;
    private boolean startFallFlying;
    private boolean starting;

    public ElytraFlight() {
        register(speedXZ, speedY, fireworkSlot, fireworkDelay, stayOffGroung, keepFlying, superBow);
    }

    private boolean canControl() {
        return !Celestial.INSTANCE.moduleManager.targetStrafe.isEnabled();
    }

    public boolean isActive() {
        return isEnabled() && starting;
    }

    private int getFireWorks(boolean hotbar) {
        return InventoryUtil.getItem(Items.FIREWORKS, hotbar);
    }

    private void noFireworks() {
        CelestialManager.sendMessage("Нету феерверков в инвентаре!");
        Celestial.INSTANCE.notificationManager.publicy("ElytraFlight", "Нету феерверков в инвентаре!", 5, NotificationType.Info);
        disable(false);
        onDisable(this);
        flying = false;
        ticksInAir = 0;
    }

    private void noElytra() {
        CelestialManager.sendMessage("Нету элитр в инвентаре!");
        Celestial.INSTANCE.notificationManager.publicy("ElytraFlight", "Нету элитр в инвентаре!", 5, NotificationType.Info);
        disable(false);
        onDisable(this);
        flying = false;
        ticksInAir = 0;
    }

    private void reset() {
        slotWithFireWorks = -1;
        prevItemInHand = Items.AIR;
        getStackInSlotCopy = null;
        starting = false;
        ticksInAir = 0;
    }

    private void resetPrevItems() {
        prevElytraSlot = -1;
        prevArmorItem = Items.AIR;
        prevArmorItemCopy = null;
    }

    private void moveFireworksToHotbar(int n2) {
        mc.playerController.windowClick(0, n2, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(0, fireworkSlot.getIntValue() - 1 + 36, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(0, n2, 0, ClickType.PICKUP, mc.player);
    }

    private void returnItem() {
        if (slotWithFireWorks == -1 || getStackInSlotCopy == null || prevItemInHand == Items.FIREWORKS || prevItemInHand == Items.AIR) {
            return;
        }
        int n2 = findInInventory(getStackInSlotCopy, prevItemInHand);
        n2 = n2 < 9 && n2 != -1 ? n2 + 36 : n2;
        mc.playerController.windowClick(0, n2, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(0, fireworkSlot.getIntValue() - 1 + 36, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(0, n2, 0, ClickType.PICKUP, mc.player);
    }

    public static int findInInventory(ItemStack stack, Item item) {
        if (stack == null) {
            return -1;
        }
        for (int i2 = 0; i2 < 45; ++i2) {
            ItemStack is = InventoryUtil.mc.player.inventory.getStackInSlot(i2);
            if (!ItemStack.areItemsEqual(is, stack) || is.getItem() != item) continue;
            return i2;
        }
        return -1;
    }

    private int getFireworks() {
        if (mc.player.getHeldItemOffhand().getItem() == Items.FIREWORKS) {
            return -2;
        }
        int n2 = getFireWorks(true);
        int n3 = getFireWorks(false);
        if (n3 == -1) {
            noFireworks();
            return -1;
        }
        if (n2 == -1) {
            moveFireworksToHotbar(n3);
            return fireworkSlot.getIntValue() - 1;
        }
        return n2;
    }

    private boolean canFly() {
        if (shouldSwapToElytra()) {
            return false;
        }
        return getFireworks() != -1;
    }

    private boolean shouldSwapToElytra() {
        ItemStack is = mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (is.getItem() != Items.ELYTRA) {
            return true;
        }
        return !ItemElytra.isUsable(is);
    }

    private void fly(boolean started) {
        if (started && (float)(System.currentTimeMillis() - lastFireworkTime) < fireworkDelay.getNumberValue() * 1000.0f) {
            return;
        }
        if (started && !mc.player.isElytraFlying()) {
            return;
        }
        if (!started && ticksInAir > 1) {
            return;
        }
        int n2 = getFireworks();
        if (n2 == -1) {
            slotWithFireWorks = -1;
            return;
        }
        slotWithFireWorks = n2;
        boolean bl3 = mc.player.getHeldItemOffhand().getItem() == Items.FIREWORKS;
        if (!bl3) {
            mc.player.connection.sendPacket(new CPacketHeldItemChange(n2));
        }
        mc.player.connection.sendPacket(new CPacketPlayerTryUseItem(bl3 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND));
        if (!bl3) {
            mc.player.connection.sendPacket(new CPacketHeldItemChange(mc.player.inventory.currentItem));
        }
        ++ticksInAir;
        flying = true;
        lastFireworkTime = System.currentTimeMillis();
    }

    private void pickPrevElytraSlot() {
        if (prevElytraSlot != -1) mc.playerController.windowClick(mc.player.inventoryContainer.windowId, prevElytraSlot, 0, ClickType.PICKUP, mc.player);
    }

    private void equipElytra() {
        int n2 = getElytraSlot();
        if (n2 == -1 && mc.player.inventory.getItemStack().getItem() != Items.ELYTRA) {
            noElytra();
            return;
        }
        if (!shouldSwapToElytra()) {
            return;
        }
        if (prevElytraSlot == -1) {
            ItemStack is = mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
            prevElytraSlot = n2;
            prevArmorItem = is.getItem();
            prevArmorItemCopy = is.copy();
        }
        mc.playerController.windowClick(mc.player.inventoryContainer.windowId, n2, 1, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(mc.player.inventoryContainer.windowId, 6, 1, ClickType.PICKUP, mc.player);
        pickPrevElytraSlot();
        elytraEquiped = true;
    }

    public static int getElytraSlot() {
        if (InventoryUtil.mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            return -2;
        }
        if (mc.currentScreen instanceof GuiContainer && !(mc.currentScreen instanceof GuiInventory) && !(mc.currentScreen instanceof GuiContainerCreative)) {
            return -1;
        }
        for (int i = 0; i < 45; ++i) {
            ItemStack is = InventoryUtil.mc.player.inventory.getStackInSlot(i);
            if (is.getItem() != Items.ELYTRA || !ItemElytra.isUsable(is)) continue;
            return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    private void returnChestPlate() {
        if (Celestial.INSTANCE.moduleManager.elytraFix.isEnabled()) {
            return;
        }
        if (prevElytraSlot != -1 && prevArmorItem != Items.AIR) {
            if (!elytraEquiped) {
                return;
            }
            ItemStack is = mc.player.inventoryContainer.getSlot(prevElytraSlot).getStack();
            boolean bl2 = is != ItemStack.EMPTY && !ItemStack.areItemsEqual(is, prevArmorItemCopy);
            int n2 = findInInventory(prevArmorItemCopy, prevArmorItem);
            n2 = n2 < 9 && n2 != -1 ? n2 + 36 : n2;
            int n3 = mc.player.inventoryContainer.windowId;
            if (mc.player.inventory.getItemStack().getItem() != Items.AIR) {
                mc.playerController.windowClick(n3, 6, 0, ClickType.PICKUP, mc.player);
                pickPrevElytraSlot();
                return;
            }
            if (n2 == -1) {
                return;
            }
            mc.playerController.windowClick(n3, n2, 0, ClickType.PICKUP, mc.player);
            mc.playerController.windowClick(n3, 6, 0, ClickType.PICKUP, mc.player);
            if (!bl2) {
                mc.playerController.windowClick(n3, n2, 0, ClickType.PICKUP, mc.player);
            } else {
                int n4 = findEmpty(false);
                if (n4 != -1) {
                    mc.playerController.windowClick(n3, n4, 0, ClickType.PICKUP, mc.player);
                }
            }
        }
        resetPrevItems();
    }

    public static int findEmpty(boolean hotbar) {
        for (int i2 = hotbar ? 0 : 9; i2 < (hotbar ? 9 : 45); ++i2) {
            if (!InventoryUtil.mc.player.inventory.getStackInSlot(i2).isEmpty()) continue;
            return i2;
        }
        return -1;
    }

    public boolean isAboveWater() {
        return mc.player.isInWater() || mc.world.isMaterialInBB(mc.player.getBoundingBox().expand(-0.1f, -0.4f, -0.1f), Material.WATER);
    }

    @Override
    public void onEvent(Event event2) {
        Event event3;
        if (event2 instanceof UseFireworkEvent) {
            event3 = (UseFireworkEvent) event2;
            event3.cancel();
        }
        if (event2 instanceof StartFallFlyingEvent) { // -> EntityPlayerSP (fB:731)
            fly(false);
        }
        if (event2 instanceof EventUpdate) {
            boolean bl2;
            boolean bl3 = mc.player.isInsideOfMaterial(Material.AIR);
            boolean bl4 = isAboveLiquid(0.1f) && bl3 && mc.player.motionY < 0.0;
            bl2 = mc.player.fallDistance > 0.0f && bl3 || bl4;
            if (bl2) {
                equipElytra();
            } else if (mc.player.onGround) {
                startFallFlying = false;
                ticksInAir = 0;
                if (!isAboveWater()) {
                    mc.gameSettings.keyBindJump.setPressed(false);
                    mc.player.jump();
                }
                return;
            }
            if (mc.player.movementInput.moveForward == 0.0f && mc.player.movementInput.moveStrafe == 0.0f) {
                currentSpeed = 0;
            }

            if (!canFly()) return;

            if (!mc.player.isElytraFlying() && !startFallFlying && mc.player.motionY < 0.0) {
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                startFallFlying = true;
            }
            if (mc.player.getTicksElytraFlying() < 4) {
                mc.gameSettings.keyBindJump.setPressed(false);
            }
            fly(true);
        }
        if (event2 instanceof EntitySyncEvent) {
            event3 = (EntitySyncEvent) event2;
            if (((EntitySyncEvent) event3).getType() != EntitySyncEvent.Types.Pre) {
                return;
            }
            if (!(mc.player.movementInput.moveForward != 0.0f || mc.player.movementInput.moveStrafe != 0.0f) && mc.player.movementInput.jump && mc.player.isElytraFlying() && flying) {
                ((EntitySyncEvent) event3).setPitch(-90.0f);
            }
            if (mc.player.getTicksElytraFlying() < 5) {
                ((EntitySyncEvent) event3).setPitch(-90.0f);
                starting = true;
            } else {
                starting = false;
            }
        }
        if (event2 instanceof MoveEvent) {
            event3 = (MoveEvent) event2;
            if (mc.player.isElytraFlying() && flying) {
                if (mc.player.getTicksElytraFlying() < 4) {
                    event3.getMoveVector().Y = 1.0;
                }
                float f2 = speedXZ.getNumberValue() - 0.017f;
                float f3 = speedY.getNumberValue();
                if (mc.gameSettings.keyBindJump.isPressed()) {
                    event3.getMoveVector().Y += (double)f3;
                } else if (mc.gameSettings.keyBindSneak.isPressed()) {
                    event3.getMoveVector().Y -= (double)f3;
                } else if (superBow.getValue() && aaY.ep(2.0f)) {
                    event3.getMoveVector().Y = mc.player.ticksExisted % 2 == 0 ? (double)0.42f : (double)-0.42f;
                } else {
                    event3.getMoveVector().Y = mc.player.ticksExisted % 2 == 0 ? (double)0.08f : (double)-0.08f;
                }
                mc.player.motionY = event3.getMoveVector().Y * (double)f3;
                if (canControl()) {
                    strafe((MoveEvent) event3, f2 *= Math.min((float)(currentSpeed += 9) / 100.0f, 1.0f));
                }
                if (stayOffGroung.getValue() && !aaY.ep(3.0f)) {
                    event3.getMoveVector().Y = 0.42f;
                    mc.player.motionY = 0.42f;
                }
            }
        }
    }

    public static void strafe(MoveEvent vA2, float f2) {
        float f3 = mc.player.rotationYaw;
        float f4 = mc.player.movementInput.moveForward;
        float f5 = mc.player.movementInput.moveStrafe;
        if (f4 != 0.0f) {
            if (f5 > 0.0f) {
                f3 += (float)(f4 > 0.0f ? -45 : 45);
            } else if (f5 < 0.0f) {
                f3 += (float)(f4 > 0.0f ? 45 : -45);
            }
            f5 = 0.0f;
            if (f4 > 0.0f) {
                f4 = 1.0f;
            } else if (f4 < 0.0f) {
                f4 = -1.0f;
            }
        }
        double d2 = Math.cos(Math.toRadians(f3 + 90.0f));
        double d3 = Math.sin(Math.toRadians(f3 + 90.0f));
        vA2.getMoveVector().X = (double)(f4 * f2) * d2 + (double)(f5 * f2) * d3;
        vA2.getMoveVector().Z = (double)(f4 * f2) * d3 - (double)(f5 * f2) * d2;
    }

    public static boolean isAboveLiquid(float offset) {
        if (mc.player == null) {
            return false;
        }
        return mc.world.getBlockState(new BlockPos(mc.player.posX, mc.player.posY - (double)offset, mc.player.posZ)).getBlock() instanceof BlockLiquid;
    }


    @Override
    public void onEnable(Module module) {
        if (module == this) {
            int n2;
            if (mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() != Items.ELYTRA && mc.player.inventory.getItemStack().getItem() != Items.ELYTRA && (n2 = getElytraSlot()) == -1) {
                noElytra();
                return;
            }
            if (getFireWorks(false) == -1) {
                noFireworks();
                return;
            }
            if (getFireWorks(true) != -1) {
                return;
            }
            n2 = fireworkSlot.getIntValue() - 1;
            getStackInSlotCopy = mc.player.inventory.getStackInSlot(n2).copy();
            prevItemInHand = mc.player.inventory.getStackInSlot(n2).getItem();
        }
    }

    @Override
    public void onDisable(Module module) {
        if (module == this) {
            currentSpeed = 0;
            startFallFlying = false;
            new Thread(() -> {
                returnItem();
                reset();
                try {
                    Thread.sleep(200L);
                }
                catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                returnChestPlate();
                resetPrevItems();
            }).start();
            if (!keepFlying.getValue() && mc.player.isElytraFlying()) {
                mc.player.setVelocity(0.0, 0.0, 0.0);
                mc.player.cancelUpdatePosition(true, true); // -> EntityPlayerSP moveEntity fB:816
                new Thread(() -> {
                    try {
                        Thread.sleep(200L);
                    }
                    catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    mc.player.setVelocity(0.0, 0.0, 0.0);
                    mc.player.cancelUpdatePosition(false, false); // -> EntityPlayerSP moveEntity fB:816
                }).start();
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
            }
        }
    }
}

