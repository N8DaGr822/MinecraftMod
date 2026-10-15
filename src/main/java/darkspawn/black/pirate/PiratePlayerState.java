package darkspawn.black.pirate;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PiratePlayerState {
	public GlobalPos entrance;
	public int navalVictories;
    public int renown,infamy,seals,ending; public boolean ghostVoyage;
    public java.util.List<net.minecraft.core.BlockPos> conqueredForts=new java.util.ArrayList<>();
	public String logboundDestination = "";
	public boolean logboundArrived;
	public boolean logboundCompassIssued;
	public int harborContract;
	public boolean harborSurveyed;
	public int[] islandContracts = new int[3];
	public int islandObjectives;
	public int pirateClues;
	public int[] factionStanding = {0,0,-25};
	public int[] factionContracts = new int[3];
	public int[] factionRanks = new int[3];
	public int factionObjectives;
	public String factionMembership="";
    public int[] repeatContracts=new int[3];
    public long[] repeatAvailableAt=new long[3];
    // Personal Rescue: 0 unaccepted, 1 searching, 2 release papers, 3 rescued, 4 freed, 5 hired.
    public int garrickRescue;
    public boolean firstSealClue;
    public int sealContract;
    // Officer commissions: 0 unaccepted, 1 active, 2 objective ready, 3 hire unlocked, 4 hired.
    public int[] officerContracts=new int[3];
    public java.util.UUID garrickCaptive;
    public net.minecraft.core.BlockPos garrickPrison;
	public int logboundKind = -1;
	public int logboundSecondsRemaining = LogboundCompass.RECALIBRATION_SECONDS;
	public net.minecraft.core.BlockPos logboundNextIsland;
	public boolean logboundCoveDiscovered;
	public java.util.List<net.minecraft.core.BlockPos> logboundVisited = new java.util.ArrayList<>();
	public long logboundSearchAfter = Long.MIN_VALUE;
	public long lastTravel = Long.MIN_VALUE;
	public void read(ValueInput input) {
        renown=Math.clamp(input.getIntOr("darkspawn_renown",0),0,1000);infamy=Math.clamp(input.getIntOr("darkspawn_infamy",0),0,100);seals=input.getIntOr("darkspawn_seals",0)&127;ending=Math.clamp(input.getIntOr("darkspawn_ending",0),0,3);ghostVoyage=input.getBooleanOr("darkspawn_ghost_voyage",false);
        conqueredForts=new java.util.ArrayList<>(input.read("darkspawn_conquered_forts",net.minecraft.core.BlockPos.CODEC.listOf()).orElse(java.util.List.of()));
		for(var faction:PirateProgression.Faction.values()) {
            int index=faction.ordinal();
            factionStanding[index]=Math.clamp(input.getIntOr("darkspawn_standing_"+faction.id,faction.initial),-100,100);
            factionContracts[index]=Math.clamp(input.getIntOr("darkspawn_contract_"+faction.id,0),0,2);
            factionRanks[index]=Math.clamp(input.getIntOr("darkspawn_rank_"+faction.id,0),0,5);
            repeatContracts[index]=Math.clamp(input.getIntOr("darkspawn_repeat_"+faction.id,0),0,2);
            repeatAvailableAt[index]=Math.max(0,input.getLongOr("darkspawn_repeat_after_"+faction.id,0));
        }
        firstSealClue=input.getBooleanOr("darkspawn_first_seal",false);
        sealContract=Math.clamp(input.getIntOr("darkspawn_seal_contract",0),0,3);if(sealContract==3)seals|=1;
        for(int i=0;i<3;i++)officerContracts[i]=Math.clamp(input.getIntOr("darkspawn_officer_"+i,0),0,4);
        garrickRescue=Math.clamp(input.getIntOr("darkspawn_garrick_rescue",0),0,5);
        garrickCaptive=input.read("darkspawn_garrick_captive",net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        garrickPrison=input.read("darkspawn_garrick_prison",net.minecraft.core.BlockPos.CODEC).orElse(null);
        factionMembership=input.getStringOr("darkspawn_faction_membership","");
        if(PirateDiplomacy.faction(factionMembership)==null)factionMembership="";
        factionObjectives=input.getIntOr("darkspawn_faction_objectives",0)&7;
		entrance = input.read("darkspawn_pirate_entrance", GlobalPos.CODEC).orElse(null);
		lastTravel = Long.MIN_VALUE;
		for(int i=0;i<3;i++)islandContracts[i]=Math.clamp(input.getIntOr("darkspawn_island_contract_"+i,0),0,2);
		islandObjectives=input.getIntOr("darkspawn_island_objectives",0)&7;
		pirateClues=input.getIntOr("darkspawn_pirate_clues",0)&((1<<IslandDiscoveries.NAMES.length)-1);
		logboundKind=Math.clamp(input.getIntOr("darkspawn_logbound_kind",-1),-1,IslandDiscoveries.NAMES.length-1);
		logboundCompassIssued = input.getBooleanOr("darkspawn_logbound_issued", false);
		harborContract = Math.clamp(input.getIntOr("darkspawn_harbor_contract", 0), 0, 2);
		harborSurveyed = input.getBooleanOr("darkspawn_harbor_surveyed", false);
		navalVictories = Math.max(0,input.getIntOr("darkspawn_naval_victories",0));
		logboundDestination = input.getStringOr("darkspawn_logbound_destination", "");
		logboundArrived = !logboundDestination.isEmpty() && input.getBooleanOr("darkspawn_logbound_arrived", false);
		logboundSecondsRemaining = Math.clamp(input.getIntOr("darkspawn_logbound_seconds", LogboundCompass.RECALIBRATION_SECONDS), 0, LogboundCompass.RECALIBRATION_SECONDS);
		logboundNextIsland = input.read("darkspawn_logbound_next_island", net.minecraft.core.BlockPos.CODEC).orElse(null);
		logboundCoveDiscovered = input.getBooleanOr("darkspawn_logbound_cove_discovered", false)
			|| (LogboundCompass.FIRST_COVE.equals(logboundDestination) && logboundArrived);
		logboundVisited = new java.util.ArrayList<>(input.read("darkspawn_logbound_visited", net.minecraft.core.BlockPos.CODEC.listOf()).orElse(java.util.List.of()));
		// Route Migration: Preserve discoveries from the former two-island prototype.
		if(logboundCoveDiscovered && !logboundVisited.contains(LogboundCompass.FIRST_COVE_CENTER)) logboundVisited.add(LogboundCompass.FIRST_COVE_CENTER);
		if(logboundArrived && LogboundCompass.NEXT_ISLAND.equals(logboundDestination) && logboundNextIsland!=null) {
			if(!logboundVisited.contains(logboundNextIsland))logboundVisited.add(logboundNextIsland);
			if(input.getIntOr("darkspawn_logbound_route_version",0)==0)logboundSecondsRemaining=LogboundCompass.RECALIBRATION_SECONDS;
		}
		logboundSearchAfter = Long.MIN_VALUE;
	}
	public void write(ValueOutput output) {
        output.putInt("darkspawn_renown",renown);output.putInt("darkspawn_infamy",infamy);output.putInt("darkspawn_seals",seals);output.putInt("darkspawn_ending",ending);output.putBoolean("darkspawn_ghost_voyage",ghostVoyage);output.store("darkspawn_conquered_forts",net.minecraft.core.BlockPos.CODEC.listOf(),conqueredForts);
		output.putInt("darkspawn_pirate_version", 1);
        for(var faction:PirateProgression.Faction.values()) {
            output.putInt("darkspawn_standing_"+faction.id,factionStanding[faction.ordinal()]);
            output.putInt("darkspawn_contract_"+faction.id,factionContracts[faction.ordinal()]);
            output.putInt("darkspawn_rank_"+faction.id,factionRanks[faction.ordinal()]);
            output.putInt("darkspawn_repeat_"+faction.id,repeatContracts[faction.ordinal()]);
            output.putLong("darkspawn_repeat_after_"+faction.id,repeatAvailableAt[faction.ordinal()]);
        }
        output.putBoolean("darkspawn_first_seal",firstSealClue);output.putInt("darkspawn_seal_contract",sealContract);
        for(int i=0;i<3;i++)output.putInt("darkspawn_officer_"+i,officerContracts[i]);
        output.putInt("darkspawn_garrick_rescue",garrickRescue);
        if(garrickCaptive!=null)output.store("darkspawn_garrick_captive",net.minecraft.core.UUIDUtil.CODEC,garrickCaptive);
        if(garrickPrison!=null)output.store("darkspawn_garrick_prison",net.minecraft.core.BlockPos.CODEC,garrickPrison);
        output.putInt("darkspawn_faction_objectives",factionObjectives);
        output.putString("darkspawn_faction_membership",factionMembership);
		for(int i=0;i<3;i++)output.putInt("darkspawn_island_contract_"+i,islandContracts[i]);
		output.putInt("darkspawn_island_objectives",islandObjectives);
		output.putInt("darkspawn_pirate_clues",pirateClues);
		output.putInt("darkspawn_logbound_kind",logboundKind);
		output.putBoolean("darkspawn_logbound_issued", logboundCompassIssued);
		output.putInt("darkspawn_harbor_contract", harborContract);
		output.putBoolean("darkspawn_harbor_surveyed", harborSurveyed);
		output.putInt("darkspawn_naval_victories", navalVictories);
		output.putString("darkspawn_logbound_destination", logboundDestination);
		output.putInt("darkspawn_logbound_route_version", 1);
		output.store("darkspawn_logbound_visited", net.minecraft.core.BlockPos.CODEC.listOf(), logboundVisited);
		output.putBoolean("darkspawn_logbound_arrived", logboundArrived);
		output.putInt("darkspawn_logbound_seconds", logboundSecondsRemaining);
		output.putBoolean("darkspawn_logbound_cove_discovered", logboundCoveDiscovered);
		if (logboundNextIsland != null) output.store("darkspawn_logbound_next_island", net.minecraft.core.BlockPos.CODEC, logboundNextIsland);
		if (entrance != null) output.store("darkspawn_pirate_entrance", GlobalPos.CODEC, entrance);
	}
	public void copyFrom(PiratePlayerState old) {
        factionRanks=old.factionRanks.clone();
        renown=old.renown;infamy=old.infamy;seals=old.seals;ending=old.ending;ghostVoyage=old.ghostVoyage;conqueredForts=new java.util.ArrayList<>(old.conqueredForts);
		// Expedition Death: Return links survive; vanilla owns active inventory and respawn.
		firstSealClue=old.firstSealClue;sealContract=old.sealContract;officerContracts=old.officerContracts.clone();
        repeatContracts=old.repeatContracts.clone();repeatAvailableAt=old.repeatAvailableAt.clone();garrickRescue=old.garrickRescue;garrickCaptive=old.garrickCaptive;garrickPrison=old.garrickPrison;
        factionMembership=old.factionMembership;factionStanding=old.factionStanding.clone();factionContracts=old.factionContracts.clone();factionObjectives=old.factionObjectives;
		entrance = old.entrance; navalVictories = old.navalVictories;
		islandContracts=old.islandContracts.clone();islandObjectives=old.islandObjectives;pirateClues=old.pirateClues;logboundKind=old.logboundKind;
		logboundCompassIssued = old.logboundCompassIssued; harborContract = old.harborContract; harborSurveyed = old.harborSurveyed;
		logboundDestination = old.logboundDestination; logboundArrived = old.logboundArrived;
		logboundSecondsRemaining = old.logboundSecondsRemaining; logboundNextIsland = old.logboundNextIsland;
		logboundVisited = new java.util.ArrayList<>(old.logboundVisited);
		logboundCoveDiscovered = old.logboundCoveDiscovered; logboundSearchAfter = old.logboundSearchAfter;
	}
}
