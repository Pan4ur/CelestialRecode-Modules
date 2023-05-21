/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  org.lwjgl.opengl.GL11
 */
package a;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.lwjgl.opengl.GL11;

import static a.AttackAura.HitBox.*;

@Module.registerModule(name ="AttackAura", alias="Attack Aura", description="Бьет окружающих вас существ", category= ModuleCategory.COMBAT)
public class AttackAura extends Module {
    public static EntityLivingBase target;
    public static EntityLivingBase targetForTargetHud;
    public final BooleanSetting onlyWeapon = new BooleanSetting("Только с оружием", false);;
    public final BooleanSetting targetHud = new BooleanSetting("Таргет худ", true);;
    public final ModeSetting THMode = new ModeSetting("Выбор таргет худа", "Тип 1", "Тип 1", "Тип 2").visibleIf(targetHud::getValue);;
    public final BooleanSetting targetEsp = new BooleanSetting("Таргет есп", false);;
    public final NumberSetting attackDst = new NumberSetting("Дистанция удара", 3.0f, 2.5f, 6.0f, 0.1f);;
    private final NumberSetting rotateDst = new NumberSetting("Дистанция ротации", 1.0f, 0.0f, 2.0f, 0.1f);;
    private final BooleanSetting onlyCrits = new BooleanSetting("Только криты", true);;
    private final BooleanSetting noHitIfEat  = new BooleanSetting("Не бить если ешь", false);;
    private final BooleanSetting onlySpace = new BooleanSetting("Только с пробелом", false).visibleIf(onlyCrits::getValue);;
    private final BooleanSetting waterCrits = new BooleanSetting("Криты в воде", true).visibleIf(onlyCrits::getValue);;
    private final BooleanSetting obsidianCheck = new BooleanSetting("Проверка на обсу", false).visibleIf(onlyCrits::getValue);;
    public final ModeSetting rotationMode = new ModeSetting("Режим ротации", "RW | Vulcan", "RW | Vulcan", "Sunrise | Matrix");;
    private final ModeSetting targetSort = new ModeSetting("Сортировать по", "Всему сразу", "Всему сразу", "Вашему FOV", "Дистанции", "Здоровью");;
    private final BooleanSetting shielddesync = new BooleanSetting("Спам щитом", false);;
    private final BooleanSetting hitToBacktrack = new BooleanSetting("Бить в бектрек", false);;
    private final BooleanSetting wallsBypass = new BooleanSetting("Обход через стену", true);;
    private final BooleanSetting Players = new BooleanSetting("Игроки", true);;
    private final BooleanSetting Naked = new BooleanSetting("Игроки без брони", true);;
    private final BooleanSetting BotsAndNpc = new BooleanSetting("НПС / Боты", false);;
    private final BooleanSetting Mobs = new BooleanSetting("Мобы", false);;
    private final MultiSelectSetting targets = new MultiSelectSetting("Выбор целей", Players, Naked, BotsAndNpc, Mobs);

    private final Vector2f rotationVec = new Vector2f();
    private Entity selectedEntity;
    public static boolean hitTick;
    public static boolean throwPearl;
    public static int cpsLimit;
    public static float clientYaw;
    public static float clientPitch;
    private int throwDelay;
    private float prevYaw;
    private boolean thisContextRotatedBefore;
    private boolean targetVisible;

    public AttackAura() {
        register(targetSort, rotationMode, targets, THMode, attackDst, rotateDst, onlyCrits, waterCrits, onlySpace, wallsBypass, noHitIfEat, obsidianCheck, shielddesync, onlyWeapon, hitToBacktrack, targetHud, targetEsp);
    }

    private Vector2f getRotationToVec(Vec3d vec) {
        double d2 = vec.xCoord - AttackAura.mc.player.posX;
        double d3 = vec.yCoord - AttackAura.mc.player.getPositionEyes((float)1.0f).yCoord;
        double d4 = vec.zCoord - AttackAura.mc.player.posZ;
        double d5 = Math.sqrt(d2 * d2 + d4 * d4);
        float x = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(d4, d2)) - 90.0);
        float y = (float)(-Math.toDegrees(Math.atan2(d3, d5)));
        return new Vector2f(x, y);
    }

    private Vec3d getDeltaForCord(HitBox a2, EntityLivingBase trgt) {
        double d2 = switch (a2) {
            case Head -> trgt.getEyeHeight();
            case Chest -> trgt.getEyeHeight() / 2.0f;
            case Leggs -> 0.05;
        };
        return trgt.getPositionVector().addVector(0.0, d2, 0.0);
    }

    private boolean checkEntity(EntityLivingBase ent) {
        if (ent instanceof EntityPlayer) {
            if (!Players.getValue()) {
                return false;
            }
            if (!Naked.getValue() && ent.getTotalArmorValue() < 1) {
                return false;
            }
            if (!BotsAndNpc.getValue() && ent.isBot) {
                return false;
            }
            if (Celestial.INSTANCE.friendManager.isFriend(ent.getName())) {
                return false;
            }
        }
        if (ent instanceof MobEntity && !Mobs.getValue()) {
            return false;
        }
        if (ent instanceof EntityArmorStand || ent.getName().equals(AttackAura.mc.player.getName())) {
            return false;
        }
        return AttackAura.mc.player != null && AttackAura.mc.world != null;
    }

    private void resolvePlayers() {
        for (EntityPlayer ent : AttackAura.mc.world.playerEntities) {
            if (!(ent instanceof EntityOtherPlayerMP)) continue;
            EntityOtherPlayerMP entityOtherPlayerMP2 = (EntityOtherPlayerMP) ent;
            entityOtherPlayerMP2.resolvePlayer();
           
        }
    }

    private void releaseResolver() {
        for (EntityPlayer ent : AttackAura.mc.world.playerEntities) {
            if (!(ent instanceof EntityOtherPlayerMP)) continue;
            EntityOtherPlayerMP entityOtherPlayerMP2 = (EntityOtherPlayerMP) ent;
            entityOtherPlayerMP2.releaseResolver();
        }
    }

    public static double getEntityArmor(EntityPlayer entityPlayer2) {
        double d2 = 0.0;
        for (int i2 = 0; i2 < 4; ++i2) {
            ItemStack is = entityPlayer2.inventory.armorInventory.get(i2);
            if (is == null || !(is.getItem() instanceof ItemArmor)) continue;
            d2 += AttackAura.getProtectionLvl(is);
        }
        return d2;
    }

    private static double getProtectionLvl(ItemStack stack) {
        ItemArmor itemArmor2 = (ItemArmor)stack.getItem();
        double d2 = itemArmor2.damageReduceAmount;
        if (stack.isItemEnchanted()) {
            d2 += (double) EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
        }
        return d2;
    }

    private double getEntityHealth(EntityLivingBase ent) {
        if (ent instanceof EntityPlayer) {
            EntityPlayer pent = (EntityPlayer) ent;
            return (double)(pent.getHealth() + pent.getAbsorptionAmount()) * (AttackAura.getEntityArmor(pent) / 20.0);
        }
        return ent.getHealth() + ent.getAbsorptionAmount();
    }

    private EntityLivingBase getTarget() {
        ArrayList<Object> arrayList = new ArrayList<>();
        for (Entity entity2 : AttackAura.mc.world.getLoadedEntityList()) {
            boolean bl2;
            if (entity2 == null || entity2 == AttackAura.mc.player || !(entity2 instanceof EntityLivingBase)) continue;
            EntityLivingBase entityLivingBase4 = (EntityLivingBase) entity2;
            boolean bl3 = bl2 = AttackAura.mc.player.getDistance(entityLivingBase4) <= (double)(attackDst.getNumberValue() + getRotateDst());
            if (!checkEntity(entityLivingBase4) || !bl2) continue;
            if (entity2.isEntityAlive()) {
                arrayList.add(entityLivingBase4);
                continue;
            }
            if (target == null || target != entityLivingBase4) continue;
            addEntityToKillFeed(entityLivingBase4);
        }
        if (overrideRotateDst() != -1.0f && arrayList.stream().anyMatch(uf2 -> AttackAura.mc.player.getDistance((EntityLivingBase)uf2) < (double)attackDst.getNumberValue())) {
            arrayList.removeIf(uf2 -> AttackAura.mc.player.getDistance((EntityLivingBase)uf2) > (double)attackDst.getNumberValue());
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        if (target != null && arrayList.contains(target) && !targetSort.selectedMode.equals("Вашему FOV")) {
            return target;
        }
        if (arrayList.size() > 1) {
            switch (targetSort.selectedMode) {
                case "Всему сразу": {
                    arrayList.sort(Comparator.comparingDouble(object -> {
                        if (object instanceof EntityPlayer) {
                            EntityPlayer entityPlayer2 = (EntityPlayer)object;
                            return -AttackAura.getEntityArmor(entityPlayer2);
                        }
                        if (object instanceof EntityLivingBase) {
                            EntityLivingBase entityLivingBase2 = (EntityLivingBase)object;
                            return -entityLivingBase2.getTotalArmorValue();
                        }
                        return 0.0;
                    }).thenComparing((object, object2) -> {
                        double d2 = getEntityHealth((EntityLivingBase)object);
                        double d3 = getEntityHealth((EntityLivingBase)object2);
                        return Double.compare(d2, d3);
                    }).thenComparing((object, object2) -> {
                        double d2 = AttackAura.mc.player.getDistance((EntityLivingBase)object);
                        double d3 = AttackAura.mc.player.getDistance((EntityLivingBase)object2);
                        return Double.compare(d2, d3);
                    }));
                    break;
                }
                case "Дистанции": {
                    arrayList.sort(Comparator.comparingDouble(AttackAura.mc.player::getDistance).thenComparingDouble(this::getEntityHealth));
                    break;
                }
                case "Здоровью": {
                    arrayList.sort(Comparator.comparingDouble(this::getEntityHealth).thenComparingDouble(AttackAura.mc.player::getDistance));
                    break;
                }
                case "Вашему FOV": {
                    arrayList.sort((uf2, uf3) -> (int)(Math.abs(RotationUtil.calcAngle((Entity)uf2) - AttackAura.mc.player.rotationYaw) - Math.abs(RotationUtil.calcAngle((Entity)uf3) - AttackAura.mc.player.rotationYaw)));
                }
            }
        } else {
            cpsLimit = 0;
        }
        return (EntityLivingBase)arrayList.get(0);
    }

    private boolean getBacktrackTarget() {
        return hitToBacktrack.getValue() && Celestial.INSTANCE.moduleManager.backTrack.isEnabled();
    }

    private boolean getPointedEntity(EntityLivingBase entityLivingBase2, Vec3d rotate, double distance) {
        return RayCastUtility.getRotatedEntity(getRotationToVec(rotate), distance, true, entityLivingBase2, getBacktrackTarget()) == entityLivingBase2; // это можешь спиздить из вексайда, мне лень ремапить
    }

    private Vector2f getDeltaForCoord(Vector2f vector2f2, Vec3d jE2) {
        double d2 = jE2.xCoord - AttackAura.mc.player.posX;
        double d3 = jE2.yCoord - AttackAura.mc.player.getPositionEyes((float)1.0f).yCoord;
        double d4 = jE2.zCoord - AttackAura.mc.player.posZ;
        double d5 = Math.sqrt(d2 * d2 + d4 * d4);
        float f2 = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(d4, d2)) - 90.0);
        float f3 = (float)(-Math.toDegrees(Math.atan2(d3, d5)));
        float f4 = MathHelper.wrapDegrees(f2 - vector2f2.x);
        float f5 = f3 - vector2f2.y;
        return new Vector2f(f4, f5);
    }

    private Vec3d getBestVec(EntityLivingBase entityLivingBase2, double dst) {
        if (entityLivingBase2.squareDistanceTo(entityLivingBase2) > 36.0) {
            return null;
        }
        Vec3d headPoint = getDeltaForCord(Head, entityLivingBase2);
        Vec3d chestPoint = getDeltaForCord(Chest, entityLivingBase2);
        Vec3d leggsPoint = getDeltaForCord(Leggs, entityLivingBase2);
        if (overidePoint() != -1.0f) {
            return headPoint;
        }
        ArrayList<Vec3d> arrayList = new ArrayList<>(Arrays.asList(headPoint, chestPoint, leggsPoint));
        arrayList.removeIf(point -> {
            targetVisible = !getPointedEntity(entityLivingBase2, point, dst);
            return targetVisible;
        });
        if (arrayList.isEmpty()) {
            return null;
        }
        arrayList.sort((point1, point2) -> (int)((Math.abs(getDeltaForCoord(rotationVec,point1).y) - Math.abs(getDeltaForCoord(rotationVec,point2).y)) * 1000.0f));
        return arrayList.get(0);
    }

    private void rotationMethod(EntityLivingBase entityLivingBase2, boolean attackContext) {
        thisContextRotatedBefore = true;
        Vec3d ent = getBestVec(entityLivingBase2, attackDst.getNumberValue() + getRotateDst());
        if (ent == null) {
            ent = entityLivingBase2.getPositionEyes(1.0f);
        }
        double deltaX = ent.xCoord - AttackAura.mc.player.posX;
        double deltaY = ent.yCoord - AttackAura.mc.player.getPositionEyes((float)1.0f).yCoord;
        double deltaZ = ent.zCoord - AttackAura.mc.player.posZ;
        double dst = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double yawToTarget  = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
        double pitchToTarget = -Math.toDegrees(Math.atan2(deltaY, dst));
        if (wallsBypass.getValue() && targetVisible && !attackContext) {
            yawToTarget  += 20.0;
        }
        float yawToTarget2 = (float) MathHelper.wrapDegrees(yawToTarget);
        float pitchToTarget2 = (float) pitchToTarget;
        switch (rotationMode.selectedMode) {
            case "Sunrise | Matrix": {
                float yawDelta  = MathHelper.wrapDegrees(yawToTarget2 - rotationVec.x) / 1.0001f;
                int yawDeltaAbs  = (int)Math.abs(yawDelta );
                float pitchDelta  = (pitchToTarget2 - rotationVec.y) / 1.0001f;
                float pitchDeltaAbs  = Math.abs(pitchDelta );
                float additionYaw = Math.min(Math.max(yawDeltaAbs, 1), 80);
                float additionPitch  = Math.max(attackContext && selectedEntity != entityLivingBase2 ? pitchDeltaAbs : 1.0f, 2.0f);
                if (Math.abs(additionYaw - prevYaw) <= 3.0f) {
                    additionYaw = prevYaw + 3.1f;
                }
                float newYaw  = rotationVec.x + (yawDelta  > 0.0f ? additionYaw : -additionYaw) * 1.0001f;
                float newPitch  = MathHelper.clamp(rotationVec.y + (pitchDelta  > 0.0f ? additionPitch  : -additionPitch ) * 1.0001f, -90.0f, 90.0f);
                rotationVec.x = newYaw;
                rotationVec.y = newPitch;
                prevYaw = additionYaw;
                break;
            }
            case "RW | Vulcan": {
                float yawDelta = MathHelper.wrapDegrees(yawToTarget2 - rotationVec.x);
                float pitchDelta = pitchToTarget2 - rotationVec.y;
                if (yawDelta > 180.0f) {
                    yawDelta -= 180.0f;
                }
                float yawDeltaAbs = Math.abs(yawDelta);
                float pitchDelta2 = Math.max(attackContext && selectedEntity != entityLivingBase2 ? Math.abs(pitchDelta) : 1.0f, (float)(2.0 + Math.random() * 2.0));
                float yawDelta2 = Math.min(Math.max(yawDeltaAbs, 1.0f), (float)(60.0 - Math.random() * 2.0));
                if (Math.abs(yawDelta2 - prevYaw) <= 3.0f) {
                    yawDelta2 = prevYaw + 3.1f;
                }
                float newYaw = rotationVec.x + (yawDelta > 0.0f ? yawDelta2 : -yawDelta2);
                float newPitch = rotationVec.y + (pitchDelta > 0.0f ? pitchDelta2 : -pitchDelta2);
                newYaw = rotationVec.x + getFixedRotation(MathHelper.wrapDegrees(newYaw - rotationVec.x));
                rotationVec.y = newPitch = rotationVec.y + getFixedRotation(newPitch - rotationVec.y);
                rotationVec.y = MathHelper.clamp(rotationVec.y, -90.0f, 90.0f);
                rotationVec.x = newYaw;
                prevYaw = yawDelta2;
            }
        }
    }

    public static float getFixedRotation(float rot) {
        return getDeltaMouse(rot) * getGCDValue();
    }

    public static float getGCDValue() {
        return (float) (getGCD() * 0.15);
    }

    public static float getGCD() {
        float f1;
        return (f1 = (float) (mc.gameSettings.mouseSensitivity * 0.6 + 0.2)) * f1 * f1 * 8;
    }

    public static float getDeltaMouse(float delta) {
        return Math.round(delta / getGCDValue());
    }

    private boolean canCrit() {
        boolean bl2;
        boolean bl3 = !onlyCrits.getValue() || AttackAura.mc.player.capabilities.isFlying || AttackAura.mc.player.isElytraFlying() || AttackAura.mc.player.isPotionActive(MobEffects.SLOWNESS) || AttackAura.mc.player.isOnLadder() || AttackAura.mc.player.isInWeb() || NoClip.isClipping && AttackAura.mc.player.motionY < 0.0;
        float f2 = AttackAura.mc.player.getCooledAttackStrength(1.5f);
        if ((double)f2 < 0.93) {
            return false;
        }
        if (onlySpace.getValue() && !AttackAura.mc.gameSettings.keyBindJump.isPressed()) {
            return true;
        }
        if (obsidianCheck.getValue() && !(AttackAura.mc.player.getHeldItemOffhand().getItem() instanceof ItemSkull) && ExplosionBuilder.isSafe(5.0f)) {
            return true;
        }
        if (Jesus.skipAttack) {
            Jesus.skipAttack = false;
            return true;
        }
        if (AttackAura.mc.player.isInsideOfMaterial(Material.LAVA)) {
            return true;
        }
        if (!waterCrits.getValue() && isAboveWater() && AttackAura.mc.gameSettings.keyBindJump.isPressed()) {
            return true;
        }
        if (!AttackAura.mc.gameSettings.keyBindJump.isPressed() && isAboveWater()) {
            return true;
        }
        List<Block> list = ExplosionBuilder.calcExplosion(AttackAura.mc.player.getBoundingBox().K(0.0, AttackAura.mc.player.getEyeHeight(), 0.0));
        double d2 = (double)((int) AttackAura.mc.player.posY) - AttackAura.mc.player.posY;
        boolean bl4 = d2 == -0.01250004768371582;
        boolean bl5 = bl2 = d2 == -0.1875;
        if ((bl2 || bl4) && !list.isEmpty() && !AttackAura.mc.player.isSneaking()) {
            return true;
        }
        if (!bl3) {
            return !AttackAura.mc.player.onGround && AttackAura.mc.player.fallDistance > 0.0f;
        }
        return true;
    }

    public float getRotateDst() {
        float f2 = overrideRotateDst();
        if (f2 != -1.0f) {
            return f2;
        }
        return rotateDst.getNumberValue();
    }


    public boolean isAboveWater() {
        return mc.player.isInWater() || mc.world.isMaterialInBB(mc.player.getBoundingBox().expand(-0.1f, -0.4f, -0.1f), Material.WATER);
    }

    private float overrideRotateDst() {
        ModuleManager manager = Celestial.INSTANCE.moduleManager;
        if (AttackAura.mc.player.isElytraFlying()) {
            return 6.0f;
        }
        if (manager.flight.isEnabled()) {
            return 6.0f;
        }
        BlockPos blockPos2 = new BlockPos(AttackAura.mc.player.posX, AttackAura.mc.player.posY - 0.1, AttackAura.mc.player.posZ);
        if (manager.jesus.isEnabled() && AttackAura.mc.world.getBlockState(blockPos2).getBlock() instanceof BlockLiquid) {
            return 6.0f;
        }
        if (AttackAura.mc.player.fallDistance > 2.5f) {
            return 5.0f;
        }
        if (Strafe.inWater) {
            return 1.0f;
        }
        if (manager.waterSpeed.isActive()) {
            return 3.0f;
        }
        return -1.0f;
    }

    private void shieldBreaker() {
        EntityLivingBase entityLivingBase2 = target;
        if (entityLivingBase2 instanceof EntityPlayer) {
            int n2;
            EntityPlayer pl = (EntityPlayer) entityLivingBase2;
            if (!(!target.isActiveItemStackBlocking(2) || pl.isSpectator() && pl.isCreative() || target.getHeldItemOffhand().getItem() != Items.SHIELD && target.getHeldItemMainhand().getItem() != Items.SHIELD || (n2 = AttackAura.breakShield(pl, false)) <= 8)) {
                AttackAura.mc.playerController.pickItem(n2);
            }
        }
    }

    public static int breakShield(EntityLivingBase target, boolean packet) {
        int n2 = InventoryUtil.getAxe(true); // хотбарчик
        if (n2 != -1) {
            AttackAura.mc.player.connection.sendPacket(new CPacketHeldItemChange(n2));
            if (packet) {
                AttackAura.mc.player.connection.sendPacket(new CPacketUseEntity(target));
                AttackAura.mc.player.connection.sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
            } else {
                AttackAura.mc.playerController.attackEntity(AttackAura.mc.player, target);
                AttackAura.mc.player.swingArm(EnumHand.MAIN_HAND);
            }
            AttackAura.mc.player.connection.sendPacket(new CPacketHeldItemChange(AttackAura.mc.player.inventory.currentItem));
            return n2;
        }
        int n3 = InventoryUtil.getAxe(false); // инвентарь
        if (n3 != -1) {
            AttackAura.mc.playerController.pickItem(n3);
            if (packet) {
                AttackAura.mc.player.connection.sendPacket(new CPacketUseEntity(target));
                AttackAura.mc.player.connection.sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
            } else {
                AttackAura.mc.playerController.attackEntity(AttackAura.mc.player, target);
                AttackAura.mc.player.swingArm(EnumHand.MAIN_HAND);
            }
            return n3;
        }
        return -1;
    }

    private float overidePoint() {
        if (AttackAura.mc.player.isElytraFlying()) {
            return 1.0f;
        }
        if (Celestial.INSTANCE.moduleManager.waterSpeed.isActive()) {
            return 0.0f;
        }
        return -1.0f;
    }

    private void attack(EntityLivingBase entityLivingBase2) {
        boolean bl2 = AttackAura.mc.player.isHandActive();
        if (noHitIfEat.getValue() && bl2) {
            return;
        }
        if (!canCrit() || cpsLimit != 0) {
            return;
        }
        rotationMethod(entityLivingBase2, true);
        selectedEntity = RotationUtil.getPointedEntity(entityLivingBase2, rotationVec.x, rotationVec.y, attackDst.getNumberValue(), true, overidePoint(), getBacktrackTarget());
        if (selectedEntity == null) {
            return;
        }
        cpsLimit = 10;
        if (onlyCrits.getValue()) {
            AttackAura.mc.player.g(entityLivingBase2);
        }
        if (AttackAura.mc.player.isHandActive()) {
            AttackAura.mc.playerController.onStoppedUsingItem(AttackAura.mc.player);
        }
        hitTick = true;
        AttackAura.mc.playerController.interactWithEntity(AttackAura.mc.player, entityLivingBase2);
        AttackAura.mc.player.swingArm(EnumHand.MAIN_HAND);
        if (Celestial.INSTANCE.moduleManager.shieldBreaker.isEnabled()) {
            shieldBreaker();
        }
        hitTick = false;
    }

    private void aura() {
        if (target == null) {
            rotationVec.x = AttackAura.mc.player.rotationYaw;
            rotationVec.y = AttackAura.mc.player.rotationPitch;
            if (AttackAura.mc.gameSettings.keyBindUseItem.isPressed()) {
                AttackAura.mc.gameSettings.keyBindUseItem.setPressed(false);
            }
            return;
        }
        if (Celestial.INSTANCE.moduleManager.elytraFlight.isActive()) {
            return;
        }
        if (shielddesync.getValue() && AttackAura.mc.player.isHandActive() && AttackAura.mc.player.isActiveItemStackBlocking(4 + new Random().nextInt(4))) {
            AttackAura.mc.playerController.onStoppedUsingItem(AttackAura.mc.player);
        }
        if (cpsLimit > 0) {
            --cpsLimit;
        }
        thisContextRotatedBefore = false;
        attack(target);
        if (!thisContextRotatedBefore) {
            rotationMethod(target, false);
        }
    }

    @Override
    public void onEvent(Event event) {
        boolean allowRotate;
        Object object;
        if (event instanceof ToggleEvent && ((ToggleEvent)(object = (ToggleEvent)event)).getModule() == this && AttackAura.mc.player != null) {
            target = null;
            rotationVec.x = AttackAura.mc.player.rotationYaw;
            rotationVec.y = AttackAura.mc.player.rotationPitch;
            cpsLimit = 0;
        }
        if (AttackAura.mc.player != null) {
            object = AttackAura.mc.player.getHeldItemMainhand();
            if (!(!onlyWeapon.getValue() || ((ItemStack)object).getItem() instanceof ItemSword || ((ItemStack)object).getItem() instanceof ItemAxe || ((ItemStack)object).getItem() instanceof ItemSpade || ((ItemStack)object).getItem() instanceof ItemPickaxe)) {
                target = null;
                targetForTargetHud = null;
                return;
            }
        }
        if (event instanceof EntitySyncEvent) {
            if (((EntitySyncEvent)event).getType() != EntitySyncEvent.Types.Pre) {
                return;
            }
            allowRotate = true;
            if (BlockPlacingListener.blockPlaced) {
                allowRotate = false;
                if (AttackAura.mc.player.ticksExisted % 10 == 0) {
                    BlockPlacingListener.blockPlaced = false;
                }
            }
            if (target == null) {
                return;
            }
            if (Celestial.INSTANCE.moduleManager.freeCamera.isEnabled()) {
                allowRotate = false;
            }
            if (throwPearl) {
                allowRotate = false;
                ++throwDelay;
                ((EntitySyncEvent)event).setYaw(clientYaw);
                ((EntitySyncEvent)event).setPitch(clientPitch);
                if (CelestialManager.canUsePearl(throwDelay + 1)) {
                    AttackAura.mc.player.connection.sendPacket(new CPacketPlayerTryUseItem((AttackAura.mc.player.getHeldItemMainhand().getItem() == Items.ENDER_PEARL ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND)));
                    throwPearl = false;
                    throwDelay = 0;
                }
            }
            setAngle((EntitySyncEvent)event, allowRotate, RotationUtil.calculateRotation(target));
        }
        if (event instanceof EventUpdate) {
            resolvePlayers();
            target = getTarget();
            if (target != null) {
                targetForTargetHud = target;
                aura();
                if (!target.isEntityAlive()) {
                    target = null;
                }
            }
            releaseResolver();
        }
        if (event instanceof EventRender3D && ((EventRender3D)event).getType() == EventRender3D.Type.Hand) {
            if (!targetEsp.getValue() || !(target instanceof EntityPlayer)) {
                return;
            }
            boolean bl2 = target.isChild();
            float f2 = ((EventRender3D)event).getPartialTicks();
            RenderManager renderManager2 = mc.getRenderManager();
            double d2 = ShaderUtil.a(AttackAura.target.posX, AttackAura.target.lastTickPosX, f2) - renderManager2.getRenderPosX();
            double d3 = ShaderUtil.a(AttackAura.target.posY, AttackAura.target.lastTickPosY, f2) - renderManager2.getRenderPosY() + Math.sin((double)System.currentTimeMillis() / 200.0) + (double)(AttackAura.target.eyeHeight / 2.0f);
            double d4 = ShaderUtil.a(AttackAura.target.posZ, AttackAura.target.lastTickPosZ, f2) - renderManager2.getRenderPosZ();
            if (bl2) {
                d3 *= 0.5;
            }
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            RenderUtil.buffer.begin(5, RealmsDefaultVertexFormat.POSITION_COLOR);
            for (int i2 = 0; i2 <= 45; ++i2) {
                int n2 = CelestialManager.getColor(i2 * 48);
                float f3 = MathHelper.cN((float)Math.toRadians(i2 * 16));
                float f4 = MathHelper.cO((float)Math.toRadians(i2 * 16));
                float f5 = 0.6f;
                double d5 = d2 + (double)(0.6f * f4);
                double d6 = d4 + (double)(0.6f * f3);
                RenderUtil.buffer.pos(d5, d3 - Math.cos((double)System.currentTimeMillis() / 200.0) / 2.0, d6).color(ShaderUtil.injectAlpha(n2, 0)).next();
                RenderUtil.buffer.pos(d5, d3, d6).color(ShaderUtil.injectAlpha(n2, 180)).next();
            }
            RenderUtil.tessellator.draw();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glPopMatrix();
            GlStateManager.resetColor();
        }
    }

    private void setAngle(EntitySyncEvent event, boolean silent, float[] rot) {
        if (silent) {
            event.setYaw(rotationVec.x);
            event.setPitch(rotationVec.y);
        }
        setFakeAngle(event, rot);
    }

    private void setFakeAngle(EntitySyncEvent event, float[] rot) {
        if (Celestial.INSTANCE.moduleManager.rotationView.isEnabled()) {
            event.setVisualYaw(rot[0]);
            event.setVisualPitch(rot[1]);
            return;
        }
        AttackAura.mc.player.rotationYawHead  = rot[0];
        AttackAura.mc.player.renderYawOffset  = rot[0];
        AttackAura.mc.player.rotationPitchHead  = rot[1];
    }

    private void addEntityToKillFeed(EntityLivingBase entityLivingBase2) {
        if (!AttackAura.mc.player.isEntityAlive()) {
            return;
        }
        KillFeed.addKill(entityLivingBase2, AttackAura.mc.player.getHeldItemMainhand(), new Vector3d((long) entityLivingBase2.posX, (long) entityLivingBase2.posY, (long) entityLivingBase2.posZ));
    }


    @Override
    public void save(JsonObject jsonObject) {
        super.save(jsonObject);
        TargetHud.reset();
        targetForTargetHud = null;
    }


    private enum HitBox {
        Head, Chest, Leggs;
    }
}

