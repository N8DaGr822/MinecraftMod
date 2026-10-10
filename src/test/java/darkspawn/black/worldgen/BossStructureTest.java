package darkspawn.black.worldgen;

import darkspawn.black.TestBootstrap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossStructureTest {
	@BeforeAll static void init() { TestBootstrap.initialize(); }
	@Test void nativePalettesHaveNoUnknownBlocksPropertiesEntitiesOrDuplicateCoordinates() throws Exception {
		for(String site:List.of("druid_shrine","hunter_camp")) for(int variant=0;variant<3;variant++) {
			String path="/data/darkspawn/structure/landmarks/"+site+"_"+variant+".nbt";
			try(var stream=getClass().getResourceAsStream(path)) {
				assertNotNull(stream,path);
				var nbt=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());
				assertTrue(nbt.getListOrEmpty("entities").isEmpty(),"Landmarks must not contain mobs");
				var palette=nbt.getListOrEmpty("palette").compoundStream().toList();
				for(var entry:palette) {
					var id=Identifier.parse(entry.getStringOr("id","minecraft:missing"));
					assertTrue(BuiltInRegistries.BLOCK.containsKey(id),id.toString());
					var block=BuiltInRegistries.BLOCK.getValue(id);
					var properties=entry.getCompoundOrEmpty("properties");
					for(String key:properties.keySet()) {
						var property=block.getStateDefinition().getProperty(key);
						assertNotNull(property,id+" "+key);
						assertTrue(property.getValue(properties.getStringOr(key,"")).isPresent(),id+" invalid "+key);
					}
				}
				var coordinates=new HashSet<List<Integer>>(); int chests=0;
				int width=site.equals("druid_shrine")?31:25;
				for(var block:nbt.getListOrEmpty("blocks").compoundStream().toList()) {
					var pos=block.getListOrEmpty("pos");
					int x=pos.getIntOr(0,-1),y=pos.getIntOr(1,-1),z=pos.getIntOr(2,-1);
					assertTrue(x>=0 && x<width && z>=0 && z<width && y>=0 && y<48,path);
					assertTrue(coordinates.add(List.of(x,y,z)),"Duplicate template block");
					int state=block.getIntOr("state",-1); assertTrue(state>=0 && state<palette.size());
					if(palette.get(state).getStringOr("id","").equals("minecraft:chest")) {
						chests++;
						assertEquals("darkspawn:chests/"+site,block.getCompoundOrEmpty("nbt").getStringOr("LootTable",""));
					}
				}
				assertEquals(1,chests);
			}
		}
	}
}
