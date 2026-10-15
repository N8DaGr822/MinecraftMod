package darkspawn.black.pirate;

import net.minecraft.server.level.ServerPlayer;

public final class SeaLegends {
    private SeaLegends() { }
    public static void renown(ServerPlayer p,int amount){var s=Pirate.state(p);s.renown=Math.clamp(s.renown+amount,0,1000);}
    public static void infamy(ServerPlayer p,int amount){var s=Pirate.state(p);s.infamy=Math.clamp(s.infamy+amount,0,100);}
    public static void victory(ServerPlayer p,PirateShip defeated){
        if(!p.isAlive()||p.isSpectator()||p.level()!=defeated.level())return;
        renown(p,defeated.encounterKind()==1?8:3);
        if(defeated.encounterKind()==1){Pirate.state(p).ghostVoyage=false;SevenSeals.discover(p,5);p.sendSystemMessage(net.minecraft.network.chat.Component.literal(Pirate.state(p).sealContract==3?"The spectral captain yields the Sixth Seal: Courage.":"The spectral sails dissolve. Record the First Binding at harbor to understand their captain's testimony."));}
        if(defeated.encounterKind()==2)infamy(p,3);
    }
    public static String journal(PiratePlayerState s){return "Renown: "+s.renown+" | Infamy: "+s.infamy+"\nRenown grows through discoveries, completed work, naval victories and first fort captures. At 50 renown harbor timber and ammunition cost one emerald instead of two.\n\nAttacking faction guards/patrols, sinking player ships and taking player forts increases infamy. At 30 infamy bounty hunters may appear. Harbor amends can reduce infamy separately from faction standing.\n\nSunk named officers survive as rescue claims: pay eight emeralds each through harbor services, then recruit them again with their commissions intact. Ordinary crew must be rehired. Ship blocks and dropped cargo are not restored.\n\nSaltwatch Fort: sneak-use your Logbound Compass inside to open shore services. Defeat its garrison, then hold for sixty uncontested seconds. Offline owners can lose forts. Reinforce twice, repair your docked ships and collect four emeralds every twenty minutes. Captures change control and services, never replace builds, refill caches or transfer ship/cargo ownership. Vanilla block editing remains enabled.\n\nGhost voyage: accept through Briggs or the board's services, then sail your own ship into clear loaded ocean. Pale spectral sails and soul particles identify the encounter. Existing patrol caps and harbor protection apply.";}
}
