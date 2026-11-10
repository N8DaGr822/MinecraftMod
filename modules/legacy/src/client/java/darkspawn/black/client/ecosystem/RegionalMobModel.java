package darkspawn.black.client.ecosystem;

import darkspawn.black.ecosystem.RegionalKind;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public final class RegionalMobModel extends EntityModel<RegionalMobRenderState> {
	private final RegionalKind kind;
	private final ModelPart body, head, left, right, tail, crest;

	public RegionalMobModel(ModelPart root, RegionalKind kind) {
		super(root); this.kind = kind;
		body = root.getChild("body"); head = root.getChild("head"); left = root.getChild("left");
		right = root.getChild("right"); tail = root.getChild("tail"); crest = root.getChild("crest");
	}

	public static LayerDefinition createBodyLayer(RegionalKind kind) {
		var mesh = new MeshDefinition(); var root = mesh.getRoot();
		var body = CubeListBuilder.create().texOffs(0, 0);
		var head = CubeListBuilder.create().texOffs(0, 0);
		var left = CubeListBuilder.create().texOffs(0, 0);
		var right = CubeListBuilder.create().texOffs(0, 0);
		var tail = CubeListBuilder.create().texOffs(0, 0);
		var crest = CubeListBuilder.create().texOffs(0, 0);
		float w = kind.width * 16, h = kind.height * 16;
		// Regional Silhouettes: Geometry follows the registered dimensions; all variants have named animated parts.
		switch (kind.form) {
			case HUMANOID -> {
				body.addBox(-w * .3F, -h * .78F, -w * .2F, w * .6F, h * .45F, w * .4F);
				head.addBox(-w * .25F, -h, -w * .25F, w * .5F, h * .23F, w * .5F);
				left.addBox(w * .3F, -h * .75F, -w * .15F, w * .2F, h * .43F, w * .3F)
						.addBox(w * .06F, -h * .33F, -w * .17F, w * .2F, h * .33F, w * .34F);
				right.addBox(-w * .5F, -h * .75F, -w * .15F, w * .2F, h * .43F, w * .3F)
						.addBox(-w * .26F, -h * .33F, -w * .17F, w * .2F, h * .33F, w * .34F);
			}
			case QUADRUPED, BIPED -> {
				body.addBox(-w * .28F, -h * .8F, -w * .35F, w * .56F, h * .42F, w * .7F);
				head.addBox(-w * .2F, -h, -w * .55F, w * .4F, h * .3F, w * .35F);
				left.addBox(w * .18F, -h * .4F, w * .15F, w * .16F, h * .4F, w * .2F);
				right.addBox(-w * .34F, -h * .4F, w * .15F, w * .16F, h * .4F, w * .2F);
				if (kind.form == RegionalKind.Form.QUADRUPED) {
					left.addBox(w * .18F, -h * .4F, -w * .35F, w * .16F, h * .4F, w * .2F);
					right.addBox(-w * .34F, -h * .4F, -w * .35F, w * .16F, h * .4F, w * .2F);
				}
				tail.addBox(-w * .08F, -h * .65F, w * .3F, w * .16F, h * .12F, w * .45F);
			}
			case ARTHROPOD -> {
				body.addBox(-w * .3F, -h, -w * .1F, w * .6F, h * .8F, w * .5F);
				head.addBox(-w * .2F, -h * .7F, -w * .4F, w * .4F, h * .5F, w * .3F);
				for (int i = 0; i < 4; i++) {
					left.addBox(w * .2F, -h * .4F, -w * .35F + i * w * .22F, w * .3F, 2, 2);
					right.addBox(-w * .5F, -h * .4F, -w * .35F + i * w * .22F, w * .3F, 2, 2);
				}
			}
			case SERPENT -> {
				body.addBox(-w * .17F, -h * .75F, -w * .25F, w * .34F, h * .65F, w * .65F);
				head.addBox(-w * .22F, -h, -w * .5F, w * .44F, h * .6F, w * .3F);
				tail.addBox(-w * .1F, -h * .55F, w * .35F, w * .2F, h * .4F, w * .45F);
			}
			case BIRD, RAY -> {
				body.addBox(-w * .16F, -h * .8F, -w * .22F, w * .32F, h * .5F, w * .5F);
				head.addBox(-w * .1F, -h, -w * .35F, w * .2F, h * .5F, w * .2F);
				left.addBox(w * .12F, -h * .6F, -w * .2F, w * .38F, h * .15F, w * .4F);
				right.addBox(-w * .5F, -h * .6F, -w * .2F, w * .38F, h * .15F, w * .4F);
				tail.addBox(-w * .06F, -h * .5F, w * .22F, w * .12F, h * .1F, w * .4F);
			}
			case FISH -> {
				body.addBox(-w * .15F, -h * .9F, -w * .4F, w * .3F, h * .8F, w * .6F);
				head.addBox(-w * .18F, -h * .9F, -w * .5F, w * .36F, h * .6F, w * .2F);
				tail.addBox(-w * .02F, -h, w * .15F, w * .04F, h, w * .3F);
			}
			case CREEPER -> {
				body.addBox(-w * .28F, -h * .75F, -w * .22F, w * .56F, h * .55F, w * .44F);
				head.addBox(-w * .35F, -h, -w * .35F, w * .7F, h * .28F, w * .65F);
				left.addBox(w * .07F, -h * .22F, -w * .4F, w * .25F, h * .22F, w * .8F);
				right.addBox(-w * .32F, -h * .22F, -w * .4F, w * .25F, h * .22F, w * .8F);
			}
			case MUSHROOM, PLANT -> {
				body.addBox(-w * .15F, -h * .7F, -w * .15F, w * .3F, h * .7F, w * .3F);
				head.addBox(-w * .5F, -h, -w * .5F, w, h * .35F, w);
				left.addBox(w * .12F, -h * .45F, -w * .07F, w * .35F, h * .1F, w * .14F);
				right.addBox(-w * .47F, -h * .45F, -w * .07F, w * .35F, h * .1F, w * .14F);
			}
			case ORB, SKULL, EYE -> {
				body.addBox(-w * .4F, -h * .9F, -w * .35F, w * .8F, h * .8F, w * .7F);
				head.addBox(-w * .18F, -h * .7F, -w * .45F, w * .36F, h * .35F, w * .12F);
				if (kind.form == RegionalKind.Form.SKULL) { tail.addBox(-w * .4F, -h * .22F, -w * .4F, w * .8F, h * .15F, w * .7F); }
			}
		}
		if (kind.herald() && kind != RegionalKind.VOID_SENTINEL) { crest.addBox(-w * .4F, -h, -w * .2F, w * .15F, h * .25F, w * .4F).addBox(w * .25F, -h, -w * .2F, w * .15F, h * .25F, w * .4F); }
		if (kind == RegionalKind.MUTANT_HUSKLING || kind == RegionalKind.FAILED_MUTANT) { left.addBox(w * .2F, -h * .7F, -w * .25F, w * .3F, h * .65F, w * .5F); }
		if (kind == RegionalKind.FOSSIL_SCORPION) { tail.addBox(-2, -h * 1.4F, w * .3F, 4, h, 4).addBox(-2, -h * 1.4F, w * .1F, 4, 3, w * .3F); }
		if (kind == RegionalKind.STONEBACK_GOAT || kind == RegionalKind.STORMSTRIDER) { crest.addBox(-w * .2F, -h, -w * .4F, 2, h * .3F, 2).addBox(w * .2F - 2, -h, -w * .4F, 2, h * .3F, 2); }
		if (kind == RegionalKind.BROOD_CARRIER || kind == RegionalKind.INFECTED_MOOSHROOM) {
			for (int i = 0; i < 3; i++) { crest.addBox(-w * .2F + i * w * .18F, -h, 0, 3, 4, 4); }
		}
		if (kind == RegionalKind.SHADOW_PRAETORIAN) { tail.addBox(-2, -h * .3F, w * .2F, 4, 4, w); }
		if (kind == RegionalKind.SOUL_KEEPER) { crest.addBox(w * .3F, -h * .5F, -w * .3F, w * .2F, h * .3F, w * .4F); }
		if (kind == RegionalKind.VOID_SENTINEL || kind == RegionalKind.SHARDLING) {
			for (int i = 0; i < 3; i++) {
				double angle = i * Math.PI * 2 / 3;
				crest.addBox((float)-Math.sin(angle) * w * .45F - 2, -h * .8F, (float)-Math.cos(angle) * w * .45F - 2, 4, h * .5F, 4);
			}
		}
		root.addOrReplaceChild("body", body, PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("head", head, PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("left", left, PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("right", right, PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("tail", tail, PartPose.offset(0, 24, 0));
		root.addOrReplaceChild("crest", crest, PartPose.offset(0, 24, 0));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override public void setupAnim(RegionalMobRenderState state) {
		super.setupAnim(state);
		float walk = (float)Math.sin(state.walkAnimationPos * .8F) * state.walkAnimationSpeed * .12F;
		left.xRot = walk; right.xRot = -walk; head.yRot = state.yRot * .005F;
		tail.yRot = (float)Math.sin(state.ageInTicks * .12F) * .13F;
		if (kind.form == RegionalKind.Form.BIRD || kind.form == RegionalKind.Form.RAY) {
			left.zRot = (float)Math.sin(state.ageInTicks * .35F) * .35F; right.zRot = -left.zRot;
		}
		if (kind.form == RegionalKind.Form.SERPENT) { body.yRot = (float)Math.sin(state.walkAnimationPos) * .15F; }
		if (state.hiding) { body.y += kind.height * 5; head.y += kind.height * 5; }
		if (state.warning > 0) { left.xRot = -.2F; right.xRot = -.2F; body.xRot = (float)Math.sin(state.ageInTicks * .8F) * .04F; }
		if (kind == RegionalKind.VOID_SENTINEL) { crest.yRot = (float)Math.toRadians(state.shieldAngle); }
		if (kind == RegionalKind.SHARDLING) { crest.yRot = state.ageInTicks * .04F; }
	}
}
