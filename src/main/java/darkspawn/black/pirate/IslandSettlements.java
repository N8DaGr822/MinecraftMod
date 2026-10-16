package darkspawn.black.pirate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public final class IslandSettlements {
	public static final String WARDEN_TAG="darkspawn_island_warden";
	private IslandSettlements() { }
	public static boolean safe(int kind) {return kind==9||kind==10||kind==12||kind==14;}
	public static boolean sheltered(LivingEntity target) {
		return target.level().dimension().equals(Pirate.LEVEL)&&(target.entityTags().contains(WARDEN_TAG)
			||target instanceof ServerPlayer&&!target.level().getEntitiesOfClass(Mob.class,target.getBoundingBox().inflate(28),mob->mob.isAlive()&&mob.entityTags().contains(WARDEN_TAG)&&mob.distanceToSqr(target)<=28*28).isEmpty());
	}
	public static int spawn(ServerPlayer player,IslandDiscoveries.Island island) {
		var level=player.level();boolean safe=safe(island.kind());
		if(!player.isAlive()||player.isSpectator()||!level.dimension().equals(Pirate.LEVEL))return 0;
		if(!safe&&(player.isCreative()||level.getDifficulty()==Difficulty.PEACEFUL||!level.getGameRules().get(GameRules.SPAWN_MOBS)))return 0;
		var saved=level.getDataStorage().computeIfAbsent(IslandDiscoveries.Encounters.TYPE);
		int mask=saved.mask(island.center()),total=safe?1:4,spawned=0;
		if((mask&0xF0)==(safe?0x10:0xF0))return 0;
		int count=0;for(var entity:level.getAllEntities())if(entity.isAlive()&&entity.entityTags().contains(IslandDiscoveries.ENCOUNTER_TAG))count++;
		for(int slot=0;slot<total&&(safe||count<48);slot++) {
			int bit=1<<(slot+4);if((mask&bit)!=0)continue;
			var pos=island.center().offset(safe?0:(slot%2==0?-7:7),0,safe?10:(slot<2?10:-10));
			if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null||!level.getBlockState(pos.below()).isSolidRender()||!level.isEmptyBlock(pos)||!level.isEmptyBlock(pos.above()))continue;
			Mob mob=safe?EntityTypes.VILLAGER.create(level,EntitySpawnReason.EVENT):island.kind()==11?EntityTypes.PIGLIN.create(level,EntitySpawnReason.EVENT):EntityTypes.PILLAGER.create(level,EntitySpawnReason.EVENT);
			if(mob==null)continue;mob.setPos(Vec3.atBottomCenterOf(pos));
			if(!level.noCollision(mob)||!level.getEntities(mob,mob.getBoundingBox()).isEmpty()){mob.discard();continue;}
			mob.setPersistenceRequired();mob.addTag(IslandDiscoveries.ENCOUNTER_TAG);
			if(safe) {
				// Settlement Services: Reuse the existing protected faction contact and parley menus.
				var faction=island.kind()==10||island.kind()==12?PirateProgression.Faction.DROWNED:PirateProgression.Faction.FLEET;
				mob.addTag(PirateDiplomacy.CONTACT_TAG);mob.addTag(PirateDiplomacy.FACTION_TAG+faction.id);mob.addTag(WARDEN_TAG);
				mob.setNoAi(true);mob.setPermanentlyInvulnerable(true);
				mob.setCustomName(Component.literal(IslandDiscoveries.NAMES[island.kind()]+" Warden"));mob.setCustomNameVisible(true);
			} else {
				mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(island.kind()==11&&slot%2==0?Items.GOLDEN_SWORD:Items.CROSSBOW));
				mob.setCustomName(Component.literal(IslandDiscoveries.NAMES[island.kind()]+" Occupier"));
			}
			if(mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin)piglin.setImmuneToZombification(true);
			if(mob instanceof net.minecraft.world.entity.raid.Raider raider)raider.setCanJoinRaid(false);
			// Finite Occupation: Record only successful spawns; defeats and reloads cannot refill the garrison.
			if(level.addFreshEntity(mob)){mask|=bit;saved.record(island.center(),mask);count++;spawned++;}else mob.discard();
		}
		return spawned;
	}
}
