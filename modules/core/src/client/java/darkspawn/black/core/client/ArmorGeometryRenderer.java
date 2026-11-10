package darkspawn.black.core.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import darkspawn.black.core.DarkspawnCore;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.ItemLike;

/**
 * Renders armor from a JSON box geometry ({@code assets/darkspawn/geometry/armor/<family>.json}) and a 256x128 material
 * texture ({@code textures/equipment/<family>.png}), with native trims drawn through a {@code *_style_overlay} equipment asset.
 * Each armor mod registers its own families.
 */
public final class ArmorGeometryRenderer implements ArmorRenderer {
	public static final String IRON_TRIM = "iron_style_overlay", DIAMOND_TRIM = "diamond_style_overlay", NETHERITE_TRIM = "netherite_style_overlay";
	private final Map<EquipmentSlot, HumanoidModel<HumanoidRenderState>> models = new EnumMap<>(EquipmentSlot.class);
	private final Map<EquipmentSlot, HumanoidModel<HumanoidRenderState>> trimModels = new EnumMap<>(EquipmentSlot.class);
	private final net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer equipmentRenderer;
	private final Identifier texture;
	private final Identifier pulseTexture;
	private final String trimAsset;

	public ArmorGeometryRenderer(EntityRendererProvider.Context context, String family, String trimAsset) {
		this(context, family, trimAsset, null);
	}

	/** @param pulseFamily optional second texture shown on alternate 12-tick beats */
	public ArmorGeometryRenderer(EntityRendererProvider.Context context, String family, String trimAsset, String pulseFamily) {
		equipmentRenderer = context.getEquipmentRenderer();
		this.trimAsset = trimAsset;
		texture = DarkspawnCore.id("textures/equipment/" + family + ".png");
		pulseTexture = pulseFamily == null ? null : DarkspawnCore.id("textures/equipment/" + pulseFamily + ".png");
		try (var reader = context.getResourceManager().getResourceOrThrow(DarkspawnCore.id("geometry/armor/" + family + ".json")).openAsReader()) {
			var geometry = JsonParser.parseReader(reader).getAsJsonObject();
			for (ArmorType type : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) {
				MeshDefinition mesh = new MeshDefinition();
				MeshDefinition trimMesh = new MeshDefinition();
				boolean hasTrim = false;
				for (var part : geometry.getAsJsonArray("parts")) {
					var definition = part.getAsJsonObject();
					var origin = definition.getAsJsonArray("origin");
					var cubes = CubeListBuilder.create();
					var trimCubes = CubeListBuilder.create();
					for (var cube : definition.getAsJsonArray("cubes")) {
						var box = cube.getAsJsonObject();
						if (!box.get("slot").getAsString().equals(type.getName())) continue;
						var from = box.getAsJsonArray("from");
						var size = box.getAsJsonArray("size");
						int material = box.get("material").getAsInt();
						cubes.texOffs(material % 4 * 64, material / 4 * 64).addBox(from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
							size.get(0).getAsFloat(), size.get(1).getAsFloat(), size.get(2).getAsFloat());
						if (box.has("trim_uv")) {
							var uv = box.getAsJsonArray("trim_uv");
							trimCubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt()).addBox(from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
								size.get(0).getAsFloat(), size.get(1).getAsFloat(), size.get(2).getAsFloat(), new net.minecraft.client.model.geom.builders.CubeDeformation(.015F));
							hasTrim = true;
						}
					}
					var child = mesh.getRoot().addOrReplaceChild(definition.get("name").getAsString(), cubes,
						PartPose.offset(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat()));
					if (definition.get("name").getAsString().equals("head")) child.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
					var trimChild = trimMesh.getRoot().addOrReplaceChild(definition.get("name").getAsString(), trimCubes,
						PartPose.offset(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat()));
					if (definition.get("name").getAsString().equals("head")) trimChild.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
				}
				models.put(type.getSlot(), new HumanoidModel<>(LayerDefinition.create(mesh, 256, 128).bakeRoot()));
				if (hasTrim) trimModels.put(type.getSlot(), new HumanoidModel<>(LayerDefinition.create(trimMesh, 64, 32).bakeRoot()));
			}
		} catch (IOException error) {
			throw new IllegalStateException("Cannot load armor geometry: " + family, error);
		}
	}

	public static void register(String family, String trimAsset, ItemLike... items) {
		ArmorRenderer.register(context -> new ArmorGeometryRenderer(context, family, trimAsset), items);
	}

	@Override public void render(PoseStack pose, SubmitNodeCollector collector, ItemStack stack, HumanoidRenderState state,
			EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> vanillaModel) {
		// Pose Ownership: Copy the wearer's native armor transforms at submission time, including crouching and riding.
		var renderedTexture = pulseTexture != null && ((int)state.ageInTicks / 12) % 2 == 1 ? pulseTexture : texture;
		ArmorRenderer.submitTransformCopyingModel(vanillaModel, state, models.get(slot), state, false, collector, pose,
			stack.hasFoil() ? RenderTypes.armorCutoutNoCullGlint(renderedTexture) : RenderTypes.armorCutoutNoCull(renderedTexture),
			light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		if (stack.has(net.minecraft.core.component.DataComponents.TRIM) && trimModels.containsKey(slot)) {
			// Native Trims: Render vanilla pattern/material palettes over raised plates with native UV coordinates.
			var trimStack = stack.copy();
			trimStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
			var layer = slot == EquipmentSlot.LEGS ? net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
				: net.minecraft.client.resources.model.EquipmentClientInfo.LayerType.HUMANOID;
			equipmentRenderer.renderLayers(layer,
				net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, DarkspawnCore.id(trimAsset)),
				net.fabricmc.fabric.api.client.rendering.v1.TransformCopyingModel.create(vanillaModel, trimModels.get(slot), false),
				com.mojang.datafixers.util.Pair.of(state, state), trimStack, pose, collector, light, state.outlineColor);
		}
	}
}
