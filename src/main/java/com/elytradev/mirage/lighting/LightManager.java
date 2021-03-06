/*
 * The MIT License
 *
 * Copyright (c) 2017 Elucent, William Thompson (unascribed), and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.elytradev.mirage.lighting;

import java.util.ArrayList;
import java.util.Comparator;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.elytradev.mirage.ConfigManager;
import com.elytradev.mirage.event.GatherLightsEvent;
import com.elytradev.mirage.lighting.Light;
import com.elytradev.mirage.shader.Shaders;

public class LightManager {
	public static final ArrayList<Light> lights = Lists.newArrayList();
	private static Vec3d cameraPos;
	private static ICamera camera;
	private static long frameId = 0;

	public static void addLight(Light l) {
		if (cameraPos.squareDistanceTo(l.x, l.y, l.z) > l.mag + ConfigManager.maxDist) {
			return;
		}

		if (camera != null && !camera.isBoundingBoxInFrustum(new AxisAlignedBB(
				l.x - l.mag,
				l.y - l.mag,
				l.z - l.mag,
				l.x + l.mag,
				l.y + l.mag,
				l.z + l.mag
		))) {
			return;
		}

		if (l != null) {
			lights.add(l);
		}
	}
	
	public static void uploadLights() {
		Shaders.currentProgram.getUniform("lightCount").setInt(lights.size());
		
		frameId++;
		if (frameId<ConfigManager.frameSkip+1) return;
		frameId = 0;

		for (int i = 0; i < Math.min(ConfigManager.maxLights, lights.size()); i++) {
				Light l = lights.get(i);
				Shaders.currentProgram.getUniform("lights["+i+"].position").setFloat(l.x, l.y, l.z);
				Shaders.currentProgram.getUniform("lights["+i+"].color").setFloat(l.r, l.g, l.b, l.a);
				Shaders.currentProgram.getUniform("lights["+i+"].coneDirection").setFloat(l.sx, l.sy, l.sz);
				Shaders.currentProgram.getUniform("lights["+i+"].coneFalloff").setFloat(l.sf);
				Shaders.currentProgram.getUniform("lights["+i+"].intensity").setFloat(l.l);
		}
	}

	private static Vec3d interpolate(Entity entity, float partialTicks) {
		return new Vec3d(
				entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
				entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks,
				entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks
		);
	}

	@SuppressWarnings("deprecation")
	public static void update(World world) {
		Minecraft mc = Minecraft.getMinecraft();
		Entity cameraEntity = mc.getRenderViewEntity();
		if (cameraEntity != null) {
			cameraPos = interpolate(cameraEntity, mc.getRenderPartialTicks());
			camera = new Frustum();
			camera.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
		} else {
			camera = null;
			return;
		}

		GatherLightsEvent event = new GatherLightsEvent(lights, cameraPos, camera, ConfigManager.maxDist);
		MinecraftForge.EVENT_BUS.post(event);

		for (Entity e : world.getLoadedEntityList()) {
			if (e instanceof EntityItem) {
				Item item = ((EntityItem) e).getItem().getItem();
				if (item instanceof IEntityLightEventConsumer) {
					((IEntityLightEventConsumer)item).gatherLights(event, e);
				} else if (item instanceof IColoredLight) {
					Light l = ((IColoredLight)item).getColoredLight();
					if (l!=null) {
						l.x = (float) e.posX;
						l.y = (float) e.posY;
						l.z = (float) e.posZ;
						addLight(l);
					}
				}
			} else {
				if (e instanceof IEntityLightEventConsumer) {
					((IEntityLightEventConsumer)e).gatherLights(event, e);
				} else if (e instanceof IColoredLight){
					Light l = ((IColoredLight)e).getColoredLight();
					if (l!=null) addLight(l);
				}
				
				for(ItemStack itemStack : e.getHeldEquipment()) {
					Item item = itemStack.getItem();
					if (item instanceof IEntityLightEventConsumer) {
						((IEntityLightEventConsumer)item).gatherLights(event, e);
					} else if (item instanceof IColoredLight) {
						Light l = ((IColoredLight)item).getColoredLight();
						if (l!=null) {
							l.x = (float) e.posX;
							l.y = (float) e.posY;
							l.z = (float) e.posZ;
							addLight(l);
						}
					}
				}
				for(ItemStack itemStack : e.getArmorInventoryList()) {
					Item item = itemStack.getItem();
					if (item instanceof IEntityLightEventConsumer) {
						((IEntityLightEventConsumer)item).gatherLights(event, e);
					} else if (item instanceof IColoredLight) {
						Light l = ((IColoredLight)item).getColoredLight();
						if (l!=null) {
							l.x = (float) e.posX;
							l.y = (float) e.posY;
							l.z = (float) e.posZ;
							addLight(l);
						}
					}
				}
			}
		}

		for (TileEntity t : world.loadedTileEntityList) {
			if (t instanceof ILightEventConsumer) {
				((ILightEventConsumer)t).gatherLights(event);
			} else if (t instanceof IColoredLight) {
				addLight(((IColoredLight)t).getColoredLight());
			}
		}
		
		lights.sort(distComparator);
		camera = null;
	}
	
	public static void clear() {
		lights.clear();
	}
	
	public static DistComparator distComparator = new DistComparator();
	
	public static class DistComparator implements Comparator<Light> {
		@Override
		public int compare(Light a, Light b) {
			double dist1 = cameraPos.squareDistanceTo(a.x, a.y, a.z);
			double dist2 = cameraPos.squareDistanceTo(b.x, b.y, b.z);
			return Double.compare(dist1, dist2);
		}
	}
}
